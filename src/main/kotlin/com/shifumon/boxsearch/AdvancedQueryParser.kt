package com.shifumon.boxsearch

import com.cobblemon.mod.common.api.storage.pc.search.PokemonFilter
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Pokemon
import com.shifumon.pokemoninfo.PokemonInfoResolver
import com.shifumon.util.TextUtil

/**
 * Interpreta os termos avançados da busca do PC. Termos não reconhecidos voltam em
 * [ParsedQuery.remainder] para o parser nativo do Cobblemon.
 *
 * Sintaxe (sem acento e sem diferença de maiúsculas; `!` na frente nega o termo):
 * - `nature:adamant` / `natureza:firme`
 * - `ability:static` / `hab:estatica`
 * - `type:fire` / `tipo:fogo`
 * - `name:pika` / `nome:pika`
 * - `gender:f` / `sexo:m`
 * - `lv:50`, `lv:>30`, `lv:<=10`, `lv:10-20`
 * - `shiny`, `ha` (habilidade oculta)
 */
object AdvancedQueryParser {
    data class ParsedQuery(val filters: List<PokemonFilter>, val remainder: String)

    private val whitespace = Regex("\\s+")
    private val comparison = Regex("^(>=|<=|>|<)?(\\d+)$")
    private val range = Regex("^(\\d+)-(\\d+)$")

    private val natureKeys = setOf("nature", "natureza", "nat")
    private val abilityKeys = setOf("ability", "habilidade", "hab")
    private val typeKeys = setOf("type", "tipo")
    private val nameKeys = setOf("name", "nome", "species", "especie")
    private val genderKeys = setOf("gender", "sexo", "genero")
    private val levelKeys = setOf("lv", "lvl", "level", "nivel", "nv")
    private val shinyWords = setOf("shiny", "brilhante")
    private val hiddenAbilityWords = setOf("ha", "hidden", "oculta")

    fun parse(query: String): ParsedQuery {
        val filters = mutableListOf<PokemonFilter>()
        val remainder = mutableListOf<String>()
        for (token in query.trim().split(whitespace)) {
            if (token.isEmpty()) continue
            val negated = token.startsWith("!")
            val filter = parseToken(if (negated) token.substring(1) else token)
            when {
                filter == null -> remainder += token
                negated -> filters += filter.inverted()
                else -> filters += filter
            }
        }
        return ParsedQuery(filters, remainder.joinToString(" "))
    }

    private fun parseToken(token: String): PokemonFilter? {
        val word = TextUtil.normalize(token)
        if (word in shinyWords) return filter { it.shiny }
        if (word in hiddenAbilityWords) return filter(PokemonInfoResolver::hasHiddenAbility)

        val separator = token.indexOf(':')
        if (separator <= 0 || separator == token.lastIndex) return null
        val key = TextUtil.normalize(token.substring(0, separator))
        val rawValue = token.substring(separator + 1).trim()
        val value = TextUtil.normalize(rawValue)

        return when (key) {
            in natureKeys -> filter { matches(value, it.effectiveNature.name.path, it.effectiveNature.displayName) }
            in abilityKeys -> filter { matches(value, it.ability.name, it.ability.displayName) }
            in typeKeys -> filter { pokemon ->
                pokemon.types.any { type ->
                    TextUtil.normalize(type.name).contains(value) || TextUtil.normalize(type.displayName.string).contains(value)
                }
            }
            in nameKeys -> filter { pokemon ->
                TextUtil.normalize(pokemon.species.name).contains(value) ||
                    TextUtil.normalize(pokemon.species.translatedName.string).contains(value) ||
                    pokemon.nickname?.let { TextUtil.normalize(it.string).contains(value) } == true
            }
            in genderKeys -> genderFilter(value)
            in levelKeys -> levelFilter(rawValue)
            else -> null
        }
    }

    /** Compara com o id interno e com o nome traduzido no idioma atual. */
    private fun matches(value: String, id: String, translationKey: String): Boolean =
        TextUtil.normalize(id).contains(value) || TextUtil.normalizedTranslation(translationKey).contains(value)

    private fun genderFilter(value: String): PokemonFilter? {
        val gender = when (value) {
            "m", "male", "macho", "masculino", "♂" -> Gender.MALE
            "f", "female", "femea", "feminino", "♀" -> Gender.FEMALE
            "none", "genderless", "sem", "nenhum", "neutro" -> Gender.GENDERLESS
            else -> return null
        }
        return filter { it.gender == gender }
    }

    private fun levelFilter(value: String): PokemonFilter? {
        range.matchEntire(value)?.let { match ->
            val (first, second) = match.destructured
            val bounds = minOf(first.toInt(), second.toInt())..maxOf(first.toInt(), second.toInt())
            return filter { it.level in bounds }
        }
        val match = comparison.matchEntire(value) ?: return null
        val (operator, number) = match.destructured
        val level = number.toInt()
        return when (operator) {
            ">" -> filter { it.level > level }
            ">=" -> filter { it.level >= level }
            "<" -> filter { it.level < level }
            "<=" -> filter { it.level <= level }
            else -> filter { it.level == level }
        }
    }

    private inline fun filter(crossinline predicate: (Pokemon) -> Boolean): PokemonFilter =
        object : PokemonFilter {
            override fun test(pokemon: Pokemon): Boolean = predicate(pokemon)
        }
}
