package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.client.battle.ClientBallDisplay;
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Acesso à animação da Poké Bola balançando durante a captura (método privado do Cobblemon). */
@Mixin(BattleOverlay.class)
public interface BattleOverlayInvoker {
    @Invoker("drawPokeBall")
    void shifumon$drawPokeBall(ClientBallDisplay state, PoseStack matrixStack, float scale, float partialTicks, boolean reversed);
}
