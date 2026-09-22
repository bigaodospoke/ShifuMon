package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.client.gui.pc.StorageWidget;
import com.shifumon.pc.PcWallpaper;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Diz qual caixa está sendo desenhada, e quando o desenho do quadro acaba.
 *
 * Serve a duas coisas: o papel de parede do ShifuMon só entra dentro do quadro (a lista de escolha
 * continua mostrando as texturas de verdade), e só na caixa que foi marcada — por isso o número da
 * caixa, e não um simples liga/desliga. O número também é o que o item da lista usa para saber em
 * qual caixa aplicar a escolha.
 */
@Mixin(StorageWidget.class)
public abstract class StorageWidgetMixin {
    @Shadow
    public abstract int getBox();

    @Inject(method = "renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
    private void shifumon$beginBoxes(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        PcWallpaper.renderingBox(getBox());
    }

    @Inject(method = "renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"))
    private void shifumon$endBoxes(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        PcWallpaper.renderingBox(-1);
    }
}
