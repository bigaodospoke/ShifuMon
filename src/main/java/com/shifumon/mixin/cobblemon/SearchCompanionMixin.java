package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.api.storage.pc.search.Search;
import com.shifumon.boxsearch.BoxSearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O filtro nativo do PC do Cobblemon chama {@code Search.of(texto)}. Aqui adicionamos os filtros
 * avançados (nature:, tipo:, hab:, shiny...) e deixamos o resto do texto para o parser original,
 * então a busca padrão do Cobblemon continua funcionando igual.
 */
@Mixin(value = Search.Companion.class, remap = false)
public abstract class SearchCompanionMixin {
    @Inject(method = "of", at = @At("HEAD"), cancellable = true)
    private void shifumon$advancedSearch(String query, CallbackInfoReturnable<Search> cir) {
        Search advanced = BoxSearch.buildSearch(query);
        if (advanced != null) {
            cir.setReturnValue(advanced);
        }
    }
}
