package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.api.gui.GuiUtilsKt;
import com.shifumon.pc.PcWallpaper;
import com.shifumon.shiny.ShinyIcons;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Todas as telas do Cobblemon desenham texturas por {@code GuiUtilsKt.blitk}. Trocar o
 * ResourceLocation aqui cobre Summary, PC e Trade de uma vez, sem depender da estrutura interna
 * de cada tela: serve para o ícone de shiny e para o papel de parede das caixas do PC.
 */
@Mixin(GuiUtilsKt.class)
public abstract class GuiUtilsMixin {
    @ModifyVariable(
            method = "blitk(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;ZF)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static ResourceLocation shifumon$replaceTexture(ResourceLocation texture) {
        return PcWallpaper.resolveTexture(ShinyIcons.resolveTexture(texture));
    }
}
