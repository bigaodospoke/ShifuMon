package com.shifumon.battle

import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.pokemon.Gender
import net.minecraft.network.chat.Component

// Modelos de exibição: desacoplam os painéis das classes de batalha do Cobblemon e permitem
// que o editor de HUD use dados de exemplo.

data class CompetitiveView(
    val baseStats: List<Pair<Stat, Int>>,
    /** Habilidades possíveis da forma; `true` = habilidade oculta. */
    val abilities: List<Pair<Component, Boolean>>,
)

data class BattleInfoView(
    val turn: Int,
    val weather: String?,
    val terrain: String?,
    val fieldEffects: List<String>,
)

data class BattlePokemonView(
    val name: Component,
    val level: Int,
    val gender: Gender,
    val shiny: Boolean,
    val types: List<ElementalType>,
    val hpRatio: Float,
    val hpText: String,
    /** Id do Showdown do status persistente (psn, par, slp...). */
    val status: String?,
    val boosts: List<Pair<Stat, Int>>,
    /** Stats base e habilidades possíveis (só no painel do oponente). */
    val competitive: CompetitiveView?,
)
