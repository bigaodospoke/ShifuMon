package com.shifumon.hud

import com.shifumon.battle.BattleInfoHud
import com.shifumon.battle.BattlePokemonHud
import com.shifumon.config.ConfigManager
import com.shifumon.config.HudPosition
import com.shifumon.config.gui.HudEditorScreen
import com.shifumon.pokemoninfo.PokemonInfoHud
import com.shifumon.util.FeatureGuard
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

/** Registro e renderização de todos os elementos da HUD. */
object HudManager {
    val elements: List<HudElement> by lazy {
        listOf(PokemonInfoHud, BattleInfoHud, BattlePokemonHud.OPPONENT, BattlePokemonHud.ALLY)
    }

    fun register() {
        HudRenderCallback.EVENT.register { graphics, _ -> render(graphics) }
    }

    private fun render(graphics: GuiGraphics) {
        val client = Minecraft.getInstance()
        if (client.options.hideGui || client.player == null || client.debugOverlay.showDebugScreen()) return
        if (client.screen is HudEditorScreen) return

        for (element in elements) {
            if (!element.isEnabled()) continue
            val feature = "hud.${element.id}"
            val panel = FeatureGuard.guard(feature, null) { element.build(preview = false) } ?: continue
            val (x, y) = screenPosition(element, panel.width, panel.height, graphics.guiWidth(), graphics.guiHeight())
            FeatureGuard.guard(feature, Unit) { panel.render(graphics, x, y) }
        }
    }

    fun positionOf(element: HudElement): HudPosition =
        ConfigManager.config.hud.positions.getOrPut(element.id) { element.defaultPosition.copy() }

    fun screenPosition(element: HudElement, width: Int, height: Int, screenWidth: Int, screenHeight: Int): Pair<Int, Int> {
        val position = positionOf(element)
        val x = (position.anchor.originX(screenWidth, width) + position.x).coerceIn(0, (screenWidth - width).coerceAtLeast(0))
        val y = (position.anchor.originY(screenHeight, height) + position.y).coerceIn(0, (screenHeight - height).coerceAtLeast(0))
        return x to y
    }

    fun resetPosition(element: HudElement) {
        ConfigManager.config.hud.positions[element.id] = element.defaultPosition.copy()
    }

    fun resetAllPositions() {
        ConfigManager.config.hud.positions.clear()
    }
}
