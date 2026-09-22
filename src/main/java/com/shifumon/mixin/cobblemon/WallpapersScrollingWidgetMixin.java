package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.client.gui.pc.WallpapersScrollingWidget;
import com.shifumon.pc.PcWallpaper;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Acrescenta o papel de parede do ShifuMon ao fim da lista do PC.
 *
 * O Cobblemon monta a lista a partir dos papéis que o servidor liberou para o jogador; o item do
 * ShifuMon entra depois disso, então nenhum dos outros é perdido ou trocado. O método é chamado de
 * novo a cada atualização da lista (ele começa limpando tudo), e por isso o item é acrescentado
 * aqui e não uma vez só — caso contrário sumiria na primeira atualização.
 *
 * Quem trata o clique nesse item é o WallpaperEntryMixin.
 */
@Mixin(WallpapersScrollingWidget.class)
public abstract class WallpapersScrollingWidgetMixin {
    /**
     * Tipo cru de propósito: o método vem de AbstractSelectionList e, depois do apagamento de
     * genéricos, é {@code addEntry(AbstractSelectionList$Entry)}. Declarar com o tipo do item não
     * casaria com a assinatura que existe no bytecode.
     */
    @SuppressWarnings("rawtypes")
    @Shadow
    protected abstract int addEntry(AbstractSelectionList.Entry entry);

    @Inject(method = "createEntries()V", at = @At("RETURN"))
    private void shifumon$addOption(CallbackInfo ci) {
        if (!PcWallpaper.inList()) return;
        WallpapersScrollingWidget list = (WallpapersScrollingWidget) (Object) this;
        // Mesma textura nos dois lugares: o Cobblemon usa a segunda como variante do shift-clique,
        // e aqui não há variante. O "false" é o selo de novidade, que não faz sentido para este.
        addEntry(list.new WallpaperEntry(PcWallpaper.getWallpaper(), PcWallpaper.getWallpaper(), false));
    }
}
