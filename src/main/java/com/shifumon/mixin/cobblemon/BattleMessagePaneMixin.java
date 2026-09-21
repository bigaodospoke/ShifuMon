package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane;
import com.shifumon.battle.log.BattleLogRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redesenha o histórico de batalha. Rolagem, barra de rolagem e o botão de expandir continuam
 * sendo tratados pelo Cobblemon; o ShifuMon desenha nas mesmas posições.
 */
@Mixin(BattleMessagePane.class)
public abstract class BattleMessagePaneMixin {
    @Inject(method = "renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    private void shifumon$renderBattleLog(GuiGraphics context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (BattleLogRenderer.render((BattleMessagePane) (Object) this, context)) {
            ci.cancel();
        }
    }
}
