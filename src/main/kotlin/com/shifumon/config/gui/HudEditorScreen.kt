package com.shifumon.config.gui

import com.shifumon.config.ConfigManager
import com.shifumon.config.HudAnchor
import com.shifumon.config.HudPosition
import com.shifumon.hud.HudElement
import com.shifumon.hud.HudManager
import com.shifumon.hud.panel.Panel
import com.shifumon.hud.render.PixelUi
import com.shifumon.hud.render.RetroPalette
import com.shifumon.util.FeatureGuard
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Editor visual da HUD: arraste os painéis (com dados de exemplo), setas movem 1px (Shift: 10px)
 * e botão direito restaura a posição padrão. A âncora é escolhida pelo terço da tela.
 */
class HudEditorScreen(private val parent: Screen?) : Screen(Component.translatable("shifumon.hud_editor.title")) {

    private class Placed(val element: HudElement, val panel: Panel, val x: Int, val y: Int) {
        fun contains(mouseX: Double, mouseY: Double) =
            mouseX >= x && mouseX < x + panel.width && mouseY >= y && mouseY < y + panel.height
    }

    private var placed: List<Placed> = emptyList()
    private var dragging: HudElement? = null
    private var selected: HudElement? = null
    private var grabOffsetX = 0
    private var grabOffsetY = 0

    override fun init() {
        val y = height - 26
        addRenderableWidget(
            Button.builder(Component.translatable("shifumon.hud_editor.reset_all")) { HudManager.resetAllPositions() }
                .bounds(width / 2 - 104, y, 100, 20).build(),
        )
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(width / 2 + 4, y, 100, 20).build())
    }

    /** Fundo sem blur (para ver o mundo), grade de alinhamento e os painéis abaixo dos botões. */
    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, 0x50000000)
        var gridX = 0
        while (gridX < width) {
            graphics.fill(gridX, 0, gridX + 1, height, GRID_COLOR)
            gridX += GRID_STEP
        }
        var gridY = 0
        while (gridY < height) {
            graphics.fill(0, gridY, width, gridY + 1, GRID_COLOR)
            gridY += GRID_STEP
        }
        graphics.fill(width / 2, 0, width / 2 + 1, height, CENTER_LINE_COLOR)
        graphics.fill(0, height / 2, width, height / 2 + 1, CENTER_LINE_COLOR)

        placed = layoutElements()
        for (item in placed) {
            item.panel.render(graphics, item.x, item.y)
            if (!item.element.isEnabled()) {
                graphics.fill(item.x, item.y, item.x + item.panel.width, item.y + item.panel.height, DISABLED_OVERLAY)
            }
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)

        for (item in placed) {
            val active = item.element == dragging || item.element == selected
            val hovered = dragging == null && item.contains(mouseX.toDouble(), mouseY.toDouble())
            val color = when {
                active -> RetroPalette.SHINY
                hovered -> RetroPalette.TEXT
                else -> IDLE_OUTLINE
            }
            PixelUi.dashedRect(graphics, item.x - 2, item.y - 2, item.panel.width + 4, item.panel.height + 4, color)
            if (active || hovered) {
                val label = item.element.displayName.copy()
                if (!item.element.isEnabled()) label.append(" (").append(Component.translatable("shifumon.hud_editor.disabled")).append(")")
                val labelY = if (item.y >= 14) item.y - 12 else item.y + item.panel.height + 4
                graphics.drawString(font, label, item.x, labelY, color, true)
            }
        }

        graphics.drawCenteredString(font, title, width / 2, 6, RetroPalette.SHINY)
        graphics.drawCenteredString(font, Component.translatable("shifumon.hud_editor.hint"), width / 2, 17, RetroPalette.TEXT_DIM)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (super.mouseClicked(mouseX, mouseY, button)) return true
        val hit = placed.lastOrNull { it.contains(mouseX, mouseY) }
        if (hit == null) {
            selected = null
            return false
        }
        when (button) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                dragging = hit.element
                selected = hit.element
                grabOffsetX = mouseX.toInt() - hit.x
                grabOffsetY = mouseY.toInt() - hit.y
            }
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> HudManager.resetPosition(hit.element)
        }
        return true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        val element = dragging ?: return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        val item = placed.firstOrNull { it.element == element } ?: return true
        moveTo(item, mouseX.toInt() - grabOffsetX, mouseY.toInt() - grabOffsetY)
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        dragging = null
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val item = placed.firstOrNull { it.element == selected }
        if (item != null) {
            val step = if (hasShiftDown()) 10 else 1
            val delta = when (keyCode) {
                GLFW.GLFW_KEY_LEFT -> -step to 0
                GLFW.GLFW_KEY_RIGHT -> step to 0
                GLFW.GLFW_KEY_UP -> 0 to -step
                GLFW.GLFW_KEY_DOWN -> 0 to step
                else -> null
            }
            if (delta != null) {
                moveTo(item, item.x + delta.first, item.y + delta.second)
                return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun onClose() {
        ConfigManager.save()
        minecraft?.setScreen(parent)
    }

    private fun layoutElements(): List<Placed> = HudManager.elements.mapNotNull { element ->
        val panel = FeatureGuard.guard("hud.${element.id}", null) { element.build(preview = true) } ?: return@mapNotNull null
        val (x, y) = HudManager.screenPosition(element, panel.width, panel.height, width, height)
        Placed(element, panel, x, y)
    }

    private fun moveTo(item: Placed, rawX: Int, rawY: Int) {
        val panel = item.panel
        val x = rawX.coerceIn(0, (width - panel.width).coerceAtLeast(0))
        val y = rawY.coerceIn(0, (height - panel.height).coerceAtLeast(0))
        val anchor = HudAnchor.nearest(x + panel.width / 2f, y + panel.height / 2f, width, height)
        ConfigManager.config.hud.positions[item.element.id] =
            HudPosition(anchor, x - anchor.originX(width, panel.width), y - anchor.originY(height, panel.height))
        placed = placed.map { if (it === item) Placed(it.element, panel, x, y) else it }
    }

    private companion object {
        const val GRID_STEP = 20
        const val GRID_COLOR = 0x12FFFFFF
        const val CENTER_LINE_COLOR = 0x30FFFFFF
        const val IDLE_OUTLINE = 0x80FFFFFF.toInt()
        const val DISABLED_OVERLAY = 0xA0101018.toInt()
    }
}
