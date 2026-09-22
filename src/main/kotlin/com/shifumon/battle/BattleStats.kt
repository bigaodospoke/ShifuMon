package com.shifumon.battle

import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon
import kotlin.math.floor
import kotlin.math.sign

/**
 * Atributos atuais em batalha. Dos Pokémon do jogador vêm os valores exatos; dos outros, a faixa
 * possível pela fórmula dos jogos (IV de 0 a 31, EV de 0 a 252, natureza que reduz ou aumenta),
 * porque o servidor não envia IVs, EVs nem natureza de quem não é seu.
 */
internal object BattleStats {
    private val stats = listOf(Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED)
    private const val MAX_IV = 31
    private const val MAX_EV = 252

    fun current(pokemon: ClientBattlePokemon, boosts: Map<Stat, Int>): StatsView {
        val owned = BattleReader.ownedPokemon(pokemon)
        val ally = BattleReader.isAlly(pokemon)
        // Selvagens não treinam EVs
        val maxEv = if (!ally && BattleReader.isWildBattle()) 0 else MAX_EV
        val paralyzed = pokemon.status?.showdownName == "par"
        val tailwind = hasTailwind(ally)
        val form = pokemon.species.getForm(pokemon.properties.aspects)

        val values = stats.map { stat ->
            val (min, max) = if (owned != null) {
                owned.getStat(stat).let { it to it }
            } else {
                estimate(form.baseStats[stat] ?: 0, pokemon.level, maxEv)
            }
            var modifier = stageMultiplier(boosts[stat] ?: 0)
            if (stat == Stats.SPEED) {
                if (paralyzed) modifier *= 0.5
                if (tailwind) modifier *= 2.0
            }
            StatValueView(stat, floor(min * modifier).toInt(), floor(max * modifier).toInt(), (modifier - 1.0).sign.toInt())
        }
        return StatsView(values, estimated = owned == null)
    }

    /** +1 = x1,5, +2 = x2 ... -1 = x2/3, -2 = x1/2 ... */
    private fun stageMultiplier(stage: Int): Double = if (stage >= 0) (2 + stage) / 2.0 else 2.0 / (2 - stage)

    /** Menor e maior valor possíveis no nível atual. */
    private fun estimate(base: Int, level: Int, maxEv: Int): Pair<Int, Int> =
        formula(base, 0, 0, level) * 90 / 100 to formula(base, MAX_IV, maxEv, level) * 110 / 100

    private fun formula(base: Int, iv: Int, ev: Int, level: Int): Int = (2 * base + iv + ev / 4) * level / 100 + 5

    private fun hasTailwind(ally: Boolean): Boolean {
        val battle = BattleReader.battle() ?: return false
        val field = BattleTracker.stateFor(battle.battleId)
        return "tailwind" in (if (ally) field.allySide else field.opponentSide)
    }
}
