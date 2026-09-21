package com.shifumon.pokemoninfo

import com.cobblemon.mod.common.api.abilities.AbilityTemplate
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbilityType
import net.minecraft.network.chat.Component
import java.util.UUID

/**
 * Monta um [PokemonInfo] a partir de uma entidade.
 *
 * O pacote de spawn do Cobblemon só envia espécie, forma, aspectos, sexo, shiny, escala e nível.
 * Nature e habilidade reais existem no cliente apenas para Pokémon do jogador (party/PC), então
 * procuramos a entidade no armazenamento local pelo UUID antes de cair nos dados parciais.
 */
object PokemonInfoResolver {
    fun resolve(entity: PokemonEntity): PokemonInfo {
        val synced = entity.pokemon
        val owned = findOwnedPokemon(synced.uuid)
        val pokemon = owned ?: synced
        val form = pokemon.form
        val scale = pokemon.scaleModifier
        val nickname = pokemon.nickname

        return PokemonInfo(
            displayName = nickname ?: pokemon.species.translatedName,
            speciesName = if (nickname != null) pokemon.species.translatedName else null,
            level = owned?.level ?: entity.entityData.get(PokemonEntity.LABEL_LEVEL).takeIf { it > 0 },
            shiny = pokemon.shiny,
            gender = pokemon.gender,
            types = form.types.toList(),
            form = form.name,
            nature = owned?.effectiveNature?.let { NatureInfo(Component.translatable(it.displayName), it.increasedStat, it.decreasedStat) },
            ability = owned?.let { Component.translatable(it.ability.displayName) },
            abilityIsHidden = owned?.let(::hasHiddenAbility) ?: false,
            hiddenAbility = hiddenAbility(form)?.let { Component.translatable(it.displayName) },
            scale = scale,
            heightMeters = form.height / 10f * scale,
            weightKg = form.weight / 10f,
            owned = owned != null,
        )
    }

    fun findOwnedPokemon(uuid: UUID): Pokemon? {
        val storage = CobblemonClient.storage
        storage.party.findByUUID(uuid)?.let { return it }
        for (pc in storage.pcStores.values) {
            pc.findByUUID(uuid)?.let { return it }
        }
        return null
    }

    fun hiddenAbility(form: FormData): AbilityTemplate? =
        form.abilities.firstOrNull { it.type === HiddenAbilityType }?.template

    fun hasHiddenAbility(pokemon: Pokemon): Boolean {
        val template = pokemon.ability.template
        return pokemon.form.abilities.any { it.type === HiddenAbilityType && it.template == template }
    }
}
