package com.shifumon.battle

import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.battle.ClientBattle
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon
import com.cobblemon.mod.common.client.battle.ClientBattleSide
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbilityType
import com.shifumon.util.StatNames
import com.shifumon.util.TextUtil
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

/** Ponto único de leitura de `CobblemonClient.battle`; converte tudo para os modelos de [BattleViews]. */
object BattleReader {
    fun battle(): ClientBattle? = CobblemonClient.battle

    fun playerSide(battle: ClientBattle): ClientBattleSide {
        val playerId = Minecraft.getInstance().player?.uuid
        return battle.sides.firstOrNull { side -> side.actors.any { it.uuid == playerId } } ?: battle.side1
    }

    fun opponentSide(battle: ClientBattle): ClientBattleSide =
        if (playerSide(battle) === battle.side1) battle.side2 else battle.side1

    fun active(side: ClientBattleSide): List<ClientBattlePokemon> =
        side.activeClientBattlePokemon.mapNotNull { it.battlePokemon }

    fun types(pokemon: ClientBattlePokemon): List<ElementalType> =
        pokemon.species.getForm(pokemon.properties.aspects).types.toList()

    fun isAlly(pokemon: ClientBattlePokemon): Boolean {
        val battle = battle() ?: return false
        return active(playerSide(battle)).any { it.uuid == pokemon.uuid }
    }

    /** Batalha contra Pokémon selvagem: eles não treinam EVs e raramente seguram itens. */
    fun isWildBattle(): Boolean {
        val battle = battle() ?: return false
        val opponents = opponentSide(battle).actors
        return opponents.isNotEmpty() && opponents.all { it.type == ActorType.WILD }
    }

    /** O Pokémon completo (atributos, item) quando é do jogador; `null` para os dos outros. */
    fun ownedPokemon(pokemon: ClientBattlePokemon): Pokemon? {
        val battle = battle() ?: return null
        val playerId = Minecraft.getInstance().player?.uuid
        battle.sides.flatMap { it.actors }.firstOrNull { it.uuid == playerId }
            ?.pokemon?.firstOrNull { it.uuid == pokemon.uuid }
            ?.let { return it }
        return CobblemonClient.storage.party.findByUUID(pokemon.uuid)
    }

    /**
     * Pokémon em campo citado numa mensagem. O nome pode vir com o dono junto ("Rowlet de fulano"),
     * então vale o nome exato ou o único Pokémon ativo cujo nome está contido no texto.
     */
    fun activeFromNames(names: List<String>): ClientBattlePokemon? {
        val battle = battle() ?: return null
        val active = battle.sides.flatMap(::active)
        if (active.isEmpty()) return null
        for (name in names) {
            val normalized = TextUtil.normalize(name)
            active.firstOrNull { TextUtil.normalize(it.displayName.string) == normalized }?.let { return it }
            val contained = active.filter { normalized.contains(TextUtil.normalize(it.displayName.string)) }
            if (contained.size == 1) return contained.first()
        }
        return null
    }

    fun infoView(battle: ClientBattle): BattleInfoView {
        val field = BattleTracker.stateFor(battle.battleId)
        fun view(effect: BattleTracker.TimedEffect) = FieldEffectView(effect.id, effect.remaining(field.turn), effect.layers)
        return BattleInfoView(
            turn = field.turn,
            weather = field.weather?.let(::view),
            terrain = field.terrain?.let(::view),
            fieldEffects = field.fieldEffects.values.map(::view),
            allySide = field.allySide.values.map(::view),
            opponentSide = field.opponentSide.values.map(::view),
        )
    }

    fun pokemonView(pokemon: ClientBattlePokemon, withCompetitive: Boolean): BattlePokemonView {
        // Aliados chegam com HP absoluto; oponentes, com fração de 0 a 1
        val ratio = if (pokemon.isHpFlat) {
            if (pokemon.maxHp > 0f) pokemon.hpValue / pokemon.maxHp else 0f
        } else {
            pokemon.hpValue
        }
        val hpText = if (pokemon.isHpFlat) {
            "${pokemon.hpValue.roundToInt()}/${pokemon.maxHp.roundToInt()}"
        } else {
            "${(ratio * 100).roundToInt()}%"
        }
        val properties = pokemon.properties
        // O servidor só manda os estágios no início da batalha; o resto vem das mensagens
        val boosts = BattleBoostTracker.stagesFor(pokemon)
        return BattlePokemonView(
            name = pokemon.displayName,
            level = pokemon.level,
            gender = pokemon.gender,
            shiny = properties.shiny == true || "shiny" in properties.aspects,
            types = types(pokemon),
            hpRatio = ratio.coerceIn(0f, 1f),
            hpText = hpText,
            status = pokemon.status?.showdownName,
            boosts = boosts,
            competitive = if (withCompetitive) competitiveView(pokemon) else null,
            stats = BattleStats.current(pokemon, boosts.toMap()),
        )
    }

    fun competitiveView(pokemon: ClientBattlePokemon): CompetitiveView {
        val form = pokemon.species.getForm(pokemon.properties.aspects)
        val baseStats = StatNames.BATTLE_ORDER.take(6).map { it to (form.baseStats[it] ?: 0) }
        val abilities = form.abilities
            .map { Component.translatable(it.template.displayName) to (it.type === HiddenAbilityType) }
            .distinctBy { it.first.string }
        return CompetitiveView(baseStats, abilities)
    }
}
