package com.shifumon.util

import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats

/** Abreviações de 3 letras no estilo dos jogos, usadas na fonte pixel. */
object StatNames {
    val BATTLE_ORDER: List<Stats> = listOf(
        Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED,
        Stats.ACCURACY, Stats.EVASION,
    )

    fun short(stat: Stat): String = when (stat) {
        Stats.HP -> "HP"
        Stats.ATTACK -> "ATK"
        Stats.DEFENCE -> "DEF"
        Stats.SPECIAL_ATTACK -> "SPA"
        Stats.SPECIAL_DEFENCE -> "SPD"
        Stats.SPEED -> "SPE"
        Stats.ACCURACY -> "ACC"
        Stats.EVASION -> "EVA"
        else -> stat.showdownId.uppercase().take(3)
    }

    fun order(stat: Stat): Int = BATTLE_ORDER.indexOf(stat).let { if (it < 0) Int.MAX_VALUE else it }
}
