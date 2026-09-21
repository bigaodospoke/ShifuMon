package com.shifumon.battle

import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.battle.ClientBattle
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon
import com.cobblemon.mod.common.client.battle.ClientBattleSide
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbilityType
import com.shifumon.util.StatNames
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

    fun infoView(battle: ClientBattle): BattleInfoView {
        val field = BattleTracker.stateFor(battle.battleId)
        return BattleInfoView(field.turn, field.weather, field.terrain, field.fieldEffects.toList())
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
        return BattlePokemonView(
            name = pokemon.displayName,
            level = pokemon.level,
            gender = pokemon.gender,
            shiny = properties.shiny == true || "shiny" in properties.aspects,
            types = types(pokemon),
            hpRatio = ratio.coerceIn(0f, 1f),
            hpText = hpText,
            status = pokemon.status?.showdownName,
            // O servidor só manda os estágios no início da batalha; o resto vem das mensagens
            boosts = BattleBoostTracker.stagesFor(pokemon),
            competitive = if (withCompetitive) competitiveView(pokemon) else null,
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
