package com.shifumon.battle

/**
 * Tabela de tipos (Gen 6+). Não considera habilidades (Levitate, Flash Fire...), itens ou
 * Terastal: é uma referência rápida, não um cálculo de dano.
 */
object TypeChart {
    /** Os 18 tipos oficiais, na ordem clássica. */
    val TYPES: List<String> = listOf(
        "normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison", "ground",
        "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy",
    )

    private val chart: Map<String, Map<String, Double>> = mapOf(
        "normal" to mapOf("rock" to 0.5, "ghost" to 0.0, "steel" to 0.5),
        "fire" to mapOf("fire" to 0.5, "water" to 0.5, "grass" to 2.0, "ice" to 2.0, "bug" to 2.0, "rock" to 0.5, "dragon" to 0.5, "steel" to 2.0),
        "water" to mapOf("fire" to 2.0, "water" to 0.5, "grass" to 0.5, "ground" to 2.0, "rock" to 2.0, "dragon" to 0.5),
        "electric" to mapOf("water" to 2.0, "electric" to 0.5, "grass" to 0.5, "ground" to 0.0, "flying" to 2.0, "dragon" to 0.5),
        "grass" to mapOf(
            "fire" to 0.5, "water" to 2.0, "grass" to 0.5, "poison" to 0.5, "ground" to 2.0, "flying" to 0.5,
            "bug" to 0.5, "rock" to 2.0, "dragon" to 0.5, "steel" to 0.5,
        ),
        "ice" to mapOf("fire" to 0.5, "water" to 0.5, "grass" to 2.0, "ice" to 0.5, "ground" to 2.0, "flying" to 2.0, "dragon" to 2.0, "steel" to 0.5),
        "fighting" to mapOf(
            "normal" to 2.0, "ice" to 2.0, "poison" to 0.5, "flying" to 0.5, "psychic" to 0.5, "bug" to 0.5,
            "rock" to 2.0, "ghost" to 0.0, "dark" to 2.0, "steel" to 2.0, "fairy" to 0.5,
        ),
        "poison" to mapOf("grass" to 2.0, "poison" to 0.5, "ground" to 0.5, "rock" to 0.5, "ghost" to 0.5, "steel" to 0.0, "fairy" to 2.0),
        "ground" to mapOf("fire" to 2.0, "electric" to 2.0, "grass" to 0.5, "poison" to 2.0, "flying" to 0.0, "bug" to 0.5, "rock" to 2.0, "steel" to 2.0),
        "flying" to mapOf("electric" to 0.5, "grass" to 2.0, "fighting" to 2.0, "bug" to 2.0, "rock" to 0.5, "steel" to 0.5),
        "psychic" to mapOf("fighting" to 2.0, "poison" to 2.0, "psychic" to 0.5, "dark" to 0.0, "steel" to 0.5),
        "bug" to mapOf(
            "fire" to 0.5, "grass" to 2.0, "fighting" to 0.5, "poison" to 0.5, "flying" to 0.5, "psychic" to 2.0,
            "ghost" to 0.5, "dark" to 2.0, "steel" to 0.5, "fairy" to 0.5,
        ),
        "rock" to mapOf("fire" to 2.0, "ice" to 2.0, "fighting" to 0.5, "ground" to 0.5, "flying" to 2.0, "bug" to 2.0, "steel" to 0.5),
        "ghost" to mapOf("normal" to 0.0, "psychic" to 2.0, "ghost" to 2.0, "dark" to 0.5),
        "dragon" to mapOf("dragon" to 2.0, "steel" to 0.5, "fairy" to 0.0),
        "dark" to mapOf("fighting" to 0.5, "psychic" to 2.0, "ghost" to 2.0, "dark" to 0.5, "fairy" to 0.5),
        "steel" to mapOf("fire" to 0.5, "water" to 0.5, "electric" to 0.5, "ice" to 2.0, "rock" to 2.0, "steel" to 0.5, "fairy" to 2.0),
        "fairy" to mapOf("fire" to 0.5, "fighting" to 2.0, "poison" to 0.5, "dragon" to 2.0, "dark" to 2.0, "steel" to 0.5),
    )

    fun multiplier(attackingType: String, defendingTypes: Iterable<String>): Double {
        val row = chart[attackingType.lowercase()] ?: return 1.0
        return defendingTypes.fold(1.0) { total, defending -> total * (row[defending.lowercase()] ?: 1.0) }
    }
}

/** Rótulo e cor do chip de eficácia. */
object Effectiveness {
    fun label(multiplier: Double): String = when {
        multiplier == 0.0 -> "X0"
        multiplier >= 4.0 -> "X4"
        multiplier >= 2.0 -> "X2"
        multiplier >= 1.0 -> "X1"
        multiplier >= 0.5 -> "X1/2"
        else -> "X1/4"
    }

    fun color(multiplier: Double): Int = when {
        multiplier == 0.0 -> 0xFF40404C
        multiplier >= 4.0 -> 0xFF1E9E48
        multiplier >= 2.0 -> 0xFF36B85A
        multiplier >= 1.0 -> 0xFF5A5A70
        multiplier >= 0.5 -> 0xFFC07820
        else -> 0xFFB04028
    }.toInt()

    /** Versão clara da cor, para texto sobre o fundo escuro do botão. */
    fun textColor(multiplier: Double): Int = when {
        multiplier == 0.0 -> 0xFFB8B8C8
        multiplier >= 2.0 -> 0xFF7CE89C
        multiplier >= 1.0 -> 0xFFC8C8D8
        else -> 0xFFFFC06C
    }.toInt()
}
