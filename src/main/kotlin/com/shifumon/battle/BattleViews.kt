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

/** Efeito em campo; [remaining] = turnos restantes contando o atual (faixa quando um item pode estender). */
data class FieldEffectView(val id: String, val remaining: IntRange?, val layers: Int = 1)

data class BattleInfoView(
    val turn: Int,
    val weather: FieldEffectView?,
    val terrain: FieldEffectView?,
    val fieldEffects: List<FieldEffectView>,
    val allySide: List<FieldEffectView>,
    val opponentSide: List<FieldEffectView>,
)

/** Valor atual de um atributo, com estágios, paralisia e Tailwind; [trend] > 0 = aumentado, < 0 = reduzido. */
data class StatValueView(val stat: Stat, val min: Int, val max: Int, val trend: Int)

/** Atributos exatos (Pokémon do jogador) ou uma faixa estimada pelos stats base e nível (oponente). */
data class StatsView(val values: List<StatValueView>, val estimated: Boolean)

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
    val stats: StatsView? = null,
)
