package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.client.gui.pc.WallpapersScrollingWidget;
import com.shifumon.pc.PcWallpaper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * O clique nos itens da lista de papéis de parede.
 *
 * No item do ShifuMon o clique é interrompido: o original mandaria ao servidor um pedido com um
 * papel de parede que ele não conhece. Em vez disso, a caixa atual é marcada aqui no cliente.
 *
 * Nos itens de verdade o clique segue normal, e quando ele é aceito (o método devolve true, ou seja,
 * o Cobblemon realmente aplicou aquele papel) a marca do ShifuMon sai daquela caixa. É assim que se
 * troca de volta: basta escolher outro papel de parede.
 */
@Mixin(WallpapersScrollingWidget.WallpaperEntry.class)
public abstract class WallpaperEntryMixin {
    /** Referência à lista, gerada pelo Kotlin por ser uma classe interna. */
    @Shadow(aliases = "this$0")
    @Final
    WallpapersScrollingWidget this$0;

    @Shadow
    public abstract ResourceLocation getWallpaper();

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void shifumon$choose(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!PcWallpaper.isShifuMon(getWallpaper())) return;
        // Mesmas condições do original: lista aberta e o ponteiro em cima deste item. Sem isso, um
        // clique na tela com a lista fechada trocaria o papel de parede sem querer.
        if (!this$0.visible || !((AbstractSelectionList.Entry<?>) (Object) this).isMouseOver(mouseX, mouseY)) return;

        PcWallpaper.choose(this$0.getStorageWidget().getBox());
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(CobblemonSounds.PC_CLICK, 1.0F));
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("RETURN"))
    private void shifumon$clear(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Só quando o clique foi aceito: o papel escolhido é um de verdade e a caixa deixa de ser
        // do ShifuMon. No item do ShifuMon este ponto não é alcançado, porque o de cima cancela.
        if (cir.getReturnValueZ()) PcWallpaper.clear(this$0.getStorageWidget().getBox());
    }
}
