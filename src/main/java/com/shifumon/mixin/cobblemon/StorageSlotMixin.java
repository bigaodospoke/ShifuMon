package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.client.gui.pc.StorageSlot;
import com.shifumon.pc.PcSlotOverlay;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Desenha o ícone de shiny e o selo de IVs por cima do slot, depois do Cobblemon desenhar o dele. */
@Mixin(StorageSlot.class)
public abstract class StorageSlotMixin {
    @Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void shifumon$renderSlotOverlay(GuiGraphics context, int posX, int posY, float partialTicks, CallbackInfo ci) {
        PcSlotOverlay.render((StorageSlot) (Object) this, context);
    }
}
