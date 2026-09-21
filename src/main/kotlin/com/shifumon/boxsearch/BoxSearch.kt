package com.shifumon.boxsearch

import com.cobblemon.mod.common.api.storage.pc.search.PokemonFilter
import com.cobblemon.mod.common.api.storage.pc.search.Search
import com.cobblemon.mod.common.pokemon.Pokemon
import com.shifumon.config.ConfigManager
import com.shifumon.util.FeatureGuard

/** Integra a busca avançada ao filtro nativo do PC (via mixin em `Search.Companion.of`). */
object BoxSearch {
    /** Retorna `null` quando não há termos avançados, deixando o Cobblemon agir sozinho. */
    @JvmStatic
    fun buildSearch(query: String): Search? {
        if (!ConfigManager.config.boxSearch.enabled) return null
        return FeatureGuard.guard("box_search.query", null) {
            val parsed = AdvancedQueryParser.parse(query)
            if (parsed.filters.isEmpty()) return@guard null
            val base = parsed.remainder.takeIf { it.isNotBlank() }?.let { Search.of(it) }
            Search(setOf(CombinedFilter(base, parsed.filters)), HashSet(), HashSet())
        }
    }

    /** Termos avançados E a busca nativa do Cobblemon para o restante do texto. */
    private class CombinedFilter(private val base: Search?, private val filters: List<PokemonFilter>) : PokemonFilter {
        override fun test(pokemon: Pokemon): Boolean = FeatureGuard.guard("box_search.filter", true) {
            (base?.passes(pokemon) ?: true) && filters.all { it.test(pokemon) }
        }
    }
}
