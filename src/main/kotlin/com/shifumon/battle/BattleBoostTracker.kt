package com.shifumon.battle

import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon
import com.shifumon.util.StatNames
import com.shifumon.util.TextUtil
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import java.util.UUID

/**
 * Estágios de atributo em batalha (+1 de Ataque, -2 de Defesa...).
 *
 * O Cobblemon só envia os estágios ao entrar na batalha, nunca durante ela, então o mod acompanha
 * pelas mensagens ("o Ataque de X subiu", "caiu bruscamente"...). A conta começa no que veio do
 * servidor e zera quando o Pokémon troca, desmaia ou algo limpa os estágios.
 */
object BattleBoostTracker {
    private const val MAX_STAGE = 6
    private const val BATTLE = "cobblemon.battle."

    private var battleId: UUID? = null
    private val stages = HashMap<UUID, MutableMap<Stat, Int>>()

    private var cachedLanguage: String? = null
    private var statsByName: Map<String, Stats> = emptyMap()

    /** Chamado junto com as outras leituras das mensagens de batalha. */
    fun record(battleId: UUID?, messages: List<Component>) {
        sync(battleId)
        messages.forEach { visit(it) }
    }

    /** Estágios atuais do Pokémon, já ordenados; semeados com o que veio no início da batalha. */
    fun stagesFor(pokemon: ClientBattlePokemon): List<Pair<Stat, Int>> {
        sync(CobblemonClient.battle?.battleId)
        val current = stages.getOrPut(pokemon.uuid) { pokemon.statChanges.toMutableMap() }
        return current.filterValues { it != 0 }.toList().sortedBy { StatNames.order(it.first) }
    }

    private fun sync(battleId: UUID?) {
        if (battleId == this.battleId) return
        this.battleId = battleId
        stages.clear()
    }

    private fun visit(component: Component) {
        val contents = findTranslatable(component) ?: return
        val key = contents.key
        val args = contents.args
        when {
            key.startsWith("${BATTLE}boost.") -> applyDelta(args, severity(key))
            key.startsWith("${BATTLE}unboost.") -> applyDelta(args, -severity(key))
            // Belly Drum e Anger Point levam o Ataque ao máximo
            key.startsWith("${BATTLE}setboost.") -> pokemonFrom(args)?.let { setStage(it, Stats.ATTACK, MAX_STAGE) }
            key == "${BATTLE}clearboost" -> pokemonFrom(args)?.let { stages[it]?.clear() }
            key == "${BATTLE}clearallboost" -> stages.values.forEach { it.clear() }
            key == "${BATTLE}clearallnegativeboost" -> pokemonFrom(args)?.let { uuid ->
                stages[uuid]?.let { map -> map.keys.filter { (map[it] ?: 0) < 0 }.forEach(map::remove) }
            }
            // Entrou ou saiu de campo: os estágios voltam a zero
            key.startsWith("${BATTLE}switch.") || key.startsWith("${BATTLE}withdraw.") ||
                key == "${BATTLE}dragged_out" || key == "${BATTLE}fainted" -> pokemonFrom(args)?.let { stages.remove(it) }
        }
    }

    /** "subiu" = 1, "bruscamente" = 2, "drasticamente" = 3; mensagens de limite não mudam nada. */
    private fun severity(key: String): Int = when {
        key.contains(".slight") -> 1
        key.contains(".sharp") -> 2
        key.contains(".severe") -> 3
        else -> 0
    }

    private fun applyDelta(args: Array<Any>, delta: Int) {
        if (delta == 0) return
        val pokemon = pokemonFrom(args) ?: return
        val stat = statFrom(args) ?: return
        val map = stages.getOrPut(pokemon) { HashMap() }
        map[stat] = ((map[stat] ?: 0) + delta).coerceIn(-MAX_STAGE, MAX_STAGE)
    }

    private fun setStage(pokemon: UUID, stat: Stat, value: Int) {
        stages.getOrPut(pokemon) { HashMap() }[stat] = value
    }

    /** O nome na mensagem pode vir com o dono junto ("Rowlet de bigaors"), então basta conter o nome. */
    private fun pokemonFrom(args: Array<Any>): UUID? {
        val battle = CobblemonClient.battle ?: return null
        val active = battle.sides.flatMap { side -> side.activeClientBattlePokemon.mapNotNull { it.battlePokemon } }
        if (active.isEmpty()) return null

        for (arg in args) {
            val text = (arg as? Component)?.string ?: continue
            val normalized = TextUtil.normalize(text)
            active.firstOrNull { TextUtil.normalize(it.displayName.string) == normalized }?.let { return it.uuid }
            val contained = active.filter { normalized.contains(TextUtil.normalize(it.displayName.string)) }
            if (contained.size == 1) return contained.first().uuid
        }
        return null
    }

    /** O argumento do atributo vem traduzido; comparamos com o nome de cada atributo. */
    private fun statFrom(args: Array<Any>): Stats? {
        val language = Minecraft.getInstance().languageManager.selected
        if (language != cachedLanguage) {
            cachedLanguage = language
            statsByName = Stats.entries.associateBy { TextUtil.normalize(it.displayName.string) }
        }
        for (arg in args) {
            val text = (arg as? Component)?.string ?: continue
            statsByName[TextUtil.normalize(text)]?.let { return it }
        }
        return null
    }

    private fun findTranslatable(component: Component): TranslatableContents? {
        val contents = component.contents
        if (contents is TranslatableContents && contents.key.startsWith("cobblemon.")) return contents
        for (sibling in component.siblings) findTranslatable(sibling)?.let { return it }
        return null
    }
}
