package com.shifumon.keybind

import com.cobblemon.mod.common.client.gui.battle.BattleGUI
import com.mojang.blaze3d.platform.InputConstants
import com.shifumon.battle.BattleDetails
import com.shifumon.config.gui.HudEditorScreen
import com.shifumon.config.gui.ShifuMonConfigScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object ShifuMonKeys {
    private const val CATEGORY = "key.categories.shifumon"

    private lateinit var openConfig: KeyMapping
    private lateinit var openHudEditor: KeyMapping
    private lateinit var toggleInfo: KeyMapping
    private lateinit var toggleDetails: KeyMapping

    /** Atalho para esconder o painel de info sem mexer na config. */
    var infoHudVisible = true
        private set

    fun register() {
        openConfig = register("open_config", GLFW.GLFW_KEY_O)
        openHudEditor = register("open_hud_editor", InputConstants.UNKNOWN.value)
        toggleInfo = register("toggle_info", InputConstants.UNKNOWN.value)
        toggleDetails = register("toggle_battle_details", GLFW.GLFW_KEY_V)
        ClientTickEvents.END_CLIENT_TICK.register(::onEndTick)
        // A tela de batalha do Cobblemon engole as teclas, então o atalho é ouvido nela também.
        // Só nela: em telas com campo de texto (chat, busca do PC) a letra faz parte do que se digita.
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen !is BattleGUI) return@register
            ScreenKeyboardEvents.afterKeyPress(screen).register { _, key, scancode, _ ->
                if (toggleDetails.matches(key, scancode)) BattleDetails.toggle()
            }
        }
    }

    /** Nome da tecla que recolhe os detalhes, para a dica no canto do painel. */
    fun toggleDetailsKeyName(): String? {
        if (!::toggleDetails.isInitialized || toggleDetails.isUnbound) return null
        return toggleDetails.translatedKeyMessage.string.uppercase()
    }

    private fun register(name: String, defaultKey: Int): KeyMapping =
        KeyBindingHelper.registerKeyBinding(KeyMapping("key.shifumon.$name", InputConstants.Type.KEYSYM, defaultKey, CATEGORY))

    private fun onEndTick(client: Minecraft) {
        while (openConfig.consumeClick()) client.setScreen(ShifuMonConfigScreen(client.screen))
        while (openHudEditor.consumeClick()) client.setScreen(HudEditorScreen(client.screen))
        while (toggleDetails.consumeClick()) BattleDetails.toggle()
        while (toggleInfo.consumeClick()) {
            infoHudVisible = !infoHudVisible
            val key = if (infoHudVisible) "shifumon.message.info_on" else "shifumon.message.info_off"
            client.player?.displayClientMessage(Component.translatable(key), true)
        }
    }
}
