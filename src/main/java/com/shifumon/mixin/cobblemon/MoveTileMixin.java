package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection;
import com.shifumon.battle.move.MoveButtonRenderer;
import com.shifumon.battle.move.MoveTooltip;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redesenha os botões de golpe do Cobblemon e mostra a dica do golpe sob o mouse. Só o visual muda:
 * o clique continua sendo detectado pelo próprio Cobblemon, pelos mesmos limites de 92x24.
 */
@Mixin(BattleMoveSelection.MoveTile.class)
public abstract class MoveTileMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    private void shifumon$renderMoveButton(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BattleMoveSelection.MoveTile tile = (BattleMoveSelection.MoveTile) (Object) this;
        MoveTooltip.onRender(tile, mouseX, mouseY);
        if (MoveButtonRenderer.render(tile, context, mouseX, mouseY)) {
            ci.cancel();
        }
    }
}
