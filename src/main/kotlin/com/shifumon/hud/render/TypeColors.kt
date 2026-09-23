package com.shifumon.hud.render

import com.cobblemon.mod.common.api.types.ElementalType
import com.shifumon.util.Colors

/**
 * Cores clássicas dos tipos, levemente escurecidas para o texto branco ficar legível.
 *
 * Elétrico, Terra e Pedra saem do amarelo-parecido dos jogos: Elétrico fica amarelo vivo, Terra
 * marrom-alaranjado e Pedra cinza-pedra, porque lado a lado numa etiqueta pequena os três se
 * confundiam. Sombrio também escureceu, para não parecer Terra.
 */
object TypeColors {
    private const val UNKNOWN = 0xFF686878.toInt()

    private val classic = mapOf(
        "normal" to 0x98986A, "fire" to 0xF08030, "water" to 0x6890F0, "electric" to 0xF0C81C,
        "grass" to 0x68B840, "ice" to 0x58B8C8, "fighting" to 0xC03028, "poison" to 0xA040A0,
        "ground" to 0xC07830, "flying" to 0x9880E0, "psychic" to 0xF85888, "bug" to 0x98A818,
        "rock" to 0x8C8474, "ghost" to 0x705898, "dragon" to 0x7038F8, "dark" to 0x4C4450,
        "steel" to 0x9898B8, "fairy" to 0xE088A8,
    )

    /** Tipos de datapacks sem cor conhecida usam o `hue` definido pelo próprio Cobblemon. */
    fun of(type: ElementalType?): Int {
        if (type == null) return UNKNOWN
        return Colors.opaque(classic[type.name.lowercase()] ?: type.hue)
    }
}

/** Etiquetas de status no estilo Gen 3 (PSN, PAR, SLP...). */
object StatusStyle {
    fun label(showdownName: String): String = showdownName.uppercase().take(3)

    fun color(showdownName: String): Int = when (showdownName) {
        "psn" -> 0xFFA040A0
        "tox" -> 0xFF702878
        "par" -> 0xFFB89818
        "slp" -> 0xFF7C7C8C
        "frz" -> 0xFF4AA8C8
        "brn" -> 0xFFD85830
        else -> 0xFF606070
    }.toInt()
}
