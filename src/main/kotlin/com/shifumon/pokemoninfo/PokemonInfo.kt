package com.shifumon.pokemoninfo

import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.pokemon.Gender
import net.minecraft.network.chat.Component

/**
 * Snapshot do que o cliente sabe sobre um Pokémon. Campos `null` significam "o servidor não
 * enviou" (ex.: nature de Pokémon selvagem), e não "vazio".
 */
data class PokemonInfo(
    val displayName: Component,
    /** Nome da espécie quando o Pokémon tem apelido. */
    val speciesName: Component?,
    val level: Int?,
    val shiny: Boolean,
    val gender: Gender,
    val types: List<ElementalType>,
    val form: String,
    val nature: NatureInfo?,
    val ability: Component?,
    val abilityIsHidden: Boolean,
    /** Habilidade oculta possível da espécie/forma (dado público, disponível para todos). */
    val hiddenAbility: Component?,
    val scale: Float,
    val heightMeters: Float,
    val weightKg: Float,
    /** true = Pokémon do próprio jogador, com dados completos. */
    val owned: Boolean,
)

data class NatureInfo(val name: Component, val raised: Stat?, val lowered: Stat?)
