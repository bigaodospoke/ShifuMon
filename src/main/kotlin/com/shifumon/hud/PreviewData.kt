package com.shifumon.hud

import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.pokemon.Gender
import com.shifumon.battle.BattleInfoView
import com.shifumon.battle.BattlePokemonView
import com.shifumon.battle.CompetitiveView
import com.shifumon.capture.CaptureEstimate
import com.shifumon.pokemoninfo.NatureInfo
import com.shifumon.pokemoninfo.PokemonInfo
import net.minecraft.network.chat.Component

/** Dados fictícios para o editor de HUD mostrar cada painel sem precisar de batalha ou Pokémon por perto. */
object PreviewData {
    fun pokemonInfo() = PokemonInfo(
        displayName = Component.literal("Pikachu"),
        speciesName = null,
        level = 32,
        shiny = true,
        gender = Gender.FEMALE,
        types = listOf(ElementalTypes.ELECTRIC),
        form = "Normal",
        nature = NatureInfo(Component.translatable("cobblemon.nature.timid"), Stats.SPEED, Stats.ATTACK),
        ability = Component.translatable("cobblemon.ability.lightningrod"),
        abilityIsHidden = true,
        hiddenAbility = Component.translatable("cobblemon.ability.lightningrod"),
        scale = 1.1f,
        heightMeters = 0.44f,
        weightKg = 6.0f,
        owned = true,
    )

    fun captureEstimate() = CaptureEstimate(Component.translatable("item.cobblemon.poke_ball"), 0.11, 0.62)

    fun battleInfo() = BattleInfoView(
        turn = 3,
        weather = "raindance",
        terrain = "electricterrain",
        fieldEffects = listOf("trickroom"),
    )

    fun battlePokemon(ally: Boolean): List<BattlePokemonView> = if (ally) {
        listOf(
            BattlePokemonView(
                name = Component.literal("Pikachu"), level = 32, gender = Gender.FEMALE, shiny = true,
                types = listOf(ElementalTypes.ELECTRIC), hpRatio = 0.72f, hpText = "58/80", status = null,
                boosts = listOf(Stats.SPEED to 2), competitive = null,
            ),
        )
    } else {
        listOf(
            BattlePokemonView(
                name = Component.literal("Garchomp"), level = 48, gender = Gender.MALE, shiny = false,
                types = listOf(ElementalTypes.DRAGON, ElementalTypes.GROUND), hpRatio = 0.18f, hpText = "18%",
                status = "brn", boosts = listOf(Stats.ATTACK to 1, Stats.DEFENCE to -1),
                competitive = CompetitiveView(
                    baseStats = listOf(
                        Stats.HP to 108, Stats.ATTACK to 130, Stats.DEFENCE to 95,
                        Stats.SPECIAL_ATTACK to 80, Stats.SPECIAL_DEFENCE to 85, Stats.SPEED to 102,
                    ),
                    abilities = listOf(
                        Component.translatable("cobblemon.ability.sandveil") to false,
                        Component.translatable("cobblemon.ability.roughskin") to true,
                    ),
                ),
            ),
        )
    }
}
