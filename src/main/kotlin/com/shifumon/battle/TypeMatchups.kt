package com.shifumon.battle

import com.cobblemon.mod.common.api.types.ElementalType

/**
 * Relações de tipo de um Pokémon, a partir da tabela de tipos.
 * Defensivo: quais tipos o acertam com mais/menos força. Ofensivo: contra quais tipos os golpes
 * do próprio tipo (STAB) são super efetivos.
 */
data class TypeMatchups(
    val weak4: List<String>,
    val weak2: List<String>,
    val resist: List<String>,
    val immune: List<String>,
    val strongAgainst: List<String>,
) {
    companion object {
        fun of(types: List<ElementalType>): TypeMatchups {
            val ownTypes = types.map { it.name.lowercase() }
            val defensive = TypeChart.TYPES.associateWith { attacking -> TypeChart.multiplier(attacking, ownTypes) }
            return TypeMatchups(
                weak4 = defensive.filterValues { it >= 4.0 }.keys.toList(),
                weak2 = defensive.filterValues { it >= 2.0 && it < 4.0 }.keys.toList(),
                resist = defensive.filterValues { it > 0.0 && it < 1.0 }.keys.toList(),
                immune = defensive.filterValues { it == 0.0 }.keys.toList(),
                strongAgainst = TypeChart.TYPES.filter { defending ->
                    ownTypes.any { attacking -> TypeChart.multiplier(attacking, listOf(defending)) >= 2.0 }
                },
            )
        }
    }
}
