package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.client.net.battle.BattleMessageHandler;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMessagePacket;
import com.shifumon.battle.BattleTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lê as mensagens de batalha antes do Cobblemon convertê-las em texto formatado, enquanto ainda
 * carregam as chaves de tradução (turno, clima, terreno). Assim não dependemos do idioma do jogo.
 */
@Mixin(BattleMessageHandler.class)
public abstract class BattleMessageHandlerMixin {
    @Inject(
            method = "handle(Lcom/cobblemon/mod/common/net/messages/client/battle/BattleMessagePacket;Lnet/minecraft/client/Minecraft;)V",
            at = @At("HEAD")
    )
    private void shifumon$onBattleMessages(BattleMessagePacket packet, Minecraft client, CallbackInfo ci) {
        BattleTracker.onBattleMessages(packet.getMessages());
    }
}
