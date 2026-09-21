package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection;
import com.shifumon.battle.move.MoveButtonRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redesenha os botões de golpe do Cobblemon. Só o visual muda: o clique continua sendo detectado
 * pelo próprio Cobblemon, pelos mesmos limites de 92x24.
 */
@Mixin(BattleMoveSelection.MoveTile.class)
public abstract class MoveTileMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    private void shifumon$renderMoveButton(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MoveButtonRenderer.render((BattleMoveSelection.MoveTile) (Object) this, context, mouseX, mouseY)) {
            ci.cancel();
        }
    }
}
