package com.shifumon.mixin.minecraft;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Acesso ao {@code addEntry}, que é protegido e fica nesta classe do Minecraft.
 *
 * A lista de papéis de parede do PC só herda o método, e o Mixin não procura um {@code @Shadow} nas
 * classes-pai: por isso o acesso vem daqui, de onde o método realmente é declarado.
 */
@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListInvoker {
    /** Tipo cru: depois do apagamento de genéricos a assinatura é {@code addEntry(Entry)}. */
    @SuppressWarnings("rawtypes")
    @Invoker("addEntry")
    int shifumon$addEntry(AbstractSelectionList.Entry entry);
}
