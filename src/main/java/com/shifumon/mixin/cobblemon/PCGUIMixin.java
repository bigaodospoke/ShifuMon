package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.shifumon.pc.PcFavoriteBoxes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cliques na abinha das caixas salvas.
 *
 * O evento de clique do Fabric não chega aqui, porque a tela do PC trata o clique por conta própria,
 * então o mod entra antes dela: quando o clique é da abinha, ele para aqui e a tela não o recebe.
 */
@Mixin(PCGUI.class)
public abstract class PCGUIMixin {
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void shifumon$favoriteBoxes(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        if (PcFavoriteBoxes.click((PCGUI) (Object) this, (int) mouseX, (int) mouseY)) {
            cir.setReturnValue(true);
        }
    }
}
