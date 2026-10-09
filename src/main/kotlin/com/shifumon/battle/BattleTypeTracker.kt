package com.shifumon.battle

import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon
import com.shifumon.util.TextUtil
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import java.util.UUID

/**
 * Tipos que mudam durante a batalha: Soak, Forest's Curse, Conversion, Reflect Type, Terastal e afins.
 *
 * O Cobblemon não envia a tipagem nova ao cliente — o painel continuaria mostrando a de origem, e as
 * fraquezas junto. As mensagens de batalha avisam a troca ("X se transformou no tipo Água"), então é
 * delas que sai a tipagem nova. O nome do tipo vem traduzido, por isso a comparação é pelo nome
 * exibido de cada tipo, do jeito que já é feito com os atributos em [BattleBoostTracker].
 *
 * A troca vale enquanto o Pokémon estiver em campo: sair, desmaiar ou voltar à forma base limpa tudo.
 */
object BattleTypeTracker {
    private const val BATTLE = "cobblemon.battle."

    private var battleId: UUID? = null
    private val overrides = HashMap<UUID, List<ElementalType>>()
    /** Mega evolução: a forma nova pode não chegar ao cliente, então o mod procura a forma mega. */
    private val megas = HashSet<UUID>()

    private var cachedLanguage: String? = null
    private var typesByName: Map<String, ElementalType> = emptyMap()

    /** Chamado junto com as outras leituras das mensagens de batalha. */
    fun record(battleId: UUID?, messages: List<Component>) {
        sync(battleId)
        messages.forEach { visit(it) }
    }

    /** Tipagem atual, ou `null` quando nada mudou e vale a da espécie. */
    fun typesFor(pokemon: ClientBattlePokemon): List<ElementalType>? {
        sync(CobblemonClient.battle?.battleId)
        overrides[pokemon.uuid]?.let { return it }
        if (pokemon.uuid in megas) return megaTypes(pokemon)
        return null
    }

    private fun sync(battleId: UUID?) {
        if (battleId == this.battleId) return
        this.battleId = battleId
        overrides.clear()
        megas.clear()
    }

    private fun visit(component: Component) {
        val contents = component.contents
        if (contents is TranslatableContents) {
            apply(contents.key, contents.args)
            contents.args.forEach { if (it is Component) visit(it) }
        }
        component.siblings.forEach { visit(it) }
    }

    private fun apply(key: String, args: Array<Any>) {
        when {
            // "%1$s se transformou no tipo %2$s!" e "%1$s Terastalizou no tipo %2$s!"
            key == "${BATTLE}start.typechange" || key == "${BATTLE}terastallize" ->
                pokemonFrom(args)?.let { uuid -> typesFrom(args)?.let { overrides[uuid] = it } }

            // "O tipo %2$s foi adicionado a %1$s!"
            key == "${BATTLE}start.typeadd" -> pokemonFrom(args)?.let { uuid ->
                val added = typesFrom(args) ?: return@let
                val current = overrides[uuid] ?: currentTypes(uuid) ?: return@let
                overrides[uuid] = (current + added).distinct()
            }

            key == "${BATTLE}mega" || key.startsWith("${BATTLE}formechange.mega") ->
                pokemonFrom(args)?.let { megas += it }

            // Voltou à forma base ou saiu de campo: a tipagem volta a ser a da espécie
            key.startsWith("${BATTLE}formechange.default.temporary.ended") ||
                key.startsWith("${BATTLE}switch.") || key.startsWith("${BATTLE}withdraw.") ||
                key == "${BATTLE}dragged_out" || key == "${BATTLE}fainted" -> pokemonFrom(args)?.let {
                overrides.remove(it)
                megas.remove(it)
            }
        }
    }

    /** Tipos citados na mensagem; o Showdown manda dois tipos separados por barra em alguns golpes. */
    private fun typesFrom(args: Array<Any>): List<ElementalType>? {
        syncLanguage()
        for (arg in args) {
            val text = textOf(arg) ?: continue
            val parts = text.split('/', '·').mapNotNull { typesByName[TextUtil.normalize(it)] }
            if (parts.isNotEmpty()) return parts
        }
        return null
    }

    /** Argumento de mensagem: o Cobblemon manda ora um componente traduzido, ora o texto cru. */
    private fun textOf(arg: Any): String? = when (arg) {
        is Component -> arg.string
        is String -> arg
        else -> null
    }

    private fun syncLanguage() {
        val language = Minecraft.getInstance().languageManager.selected
        if (language == cachedLanguage) return
        cachedLanguage = language
        typesByName = buildMap {
            ElementalTypes.all().forEach { type ->
                put(TextUtil.normalize(type.name), type)
                put(TextUtil.normalize(type.displayName.string), type)
            }
        }
    }

    private fun currentTypes(uuid: UUID): List<ElementalType>? {
        val battle = CobblemonClient.battle ?: return null
        val pokemon = battle.sides.flatMap(BattleReader::active).firstOrNull { it.uuid == uuid } ?: return null
        return BattleReader.form(pokemon).types.toList()
    }

    /**
     * Forma mega da espécie. Serve de rede de segurança: quando o cliente já recebeu a forma nova,
     * a tipagem dela é a que está em uso e nada muda aqui.
     */
    private fun megaTypes(pokemon: ClientBattlePokemon): List<ElementalType>? {
        val current = BattleReader.form(pokemon)
        if (current.name.contains("mega", ignoreCase = true)) return null
        val mega = pokemon.species.forms.firstOrNull { it.name.contains("mega", ignoreCase = true) } ?: return null
        return mega.types.toList()
    }

    private fun pokemonFrom(args: Array<Any>): UUID? =
        BattleReader.activeFromNames(args.mapNotNull(::textOf))?.uuid
}
