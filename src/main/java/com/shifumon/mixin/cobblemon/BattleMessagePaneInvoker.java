package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** O Cobblemon recalcula o tamanho do histórico no início do render, que o ShifuMon substitui. */
@Mixin(BattleMessagePane.class)
public interface BattleMessagePaneInvoker {
    @Invoker("correctSize")
    void shifumon$correctSize();
}
