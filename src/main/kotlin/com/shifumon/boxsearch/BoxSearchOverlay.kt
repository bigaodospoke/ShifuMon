package com.shifumon.boxsearch

import com.cobblemon.mod.common.api.storage.pc.search.Search
import com.cobblemon.mod.common.client.gui.pc.FilterWidget
import com.cobblemon.mod.common.client.gui.pc.PCGUI
import com.cobblemon.mod.common.client.storage.ClientBox
import com.shifumon.config.ConfigManager
import com.shifumon.hud.render.PixelUi
import com.shifumon.hud.render.RetroPalette
import com.shifumon.util.FeatureGuard
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

/**
 * Painel ao lado do PC: resultados da busca em todas as caixas (clique para ir até a caixa)
 * e, com o campo de filtro focado, um resumo da sintaxe avançada.
 */
object BoxSearchOverlay {
    private const val FEATURE = "box_search.overlay"
    private const val ROW_HEIGHT = 11
    private const val REFRESH_MS = 750L

    private data class BoxResult(val index: Int, val name: Component, val count: Int)

    private data class Hitbox(val boxIndex: Int, val x: Int, val y: Int, val width: Int, val height: Int) {
        fun contains(mouseX: Double, mouseY: Double) = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    }

    private val helpKeys = listOf("shifumon.search.help.1", "shifumon.search.help.2", "shifumon.search.help.3")

    private var cachedSearch: Search? = null
    private var cachedAt = 0L
    private var cachedResults: List<BoxResult> = emptyList()
    private var hitboxes: List<Hitbox> = emptyList()

    fun register() {
        // A Fabric recria os eventos da tela a cada init (inclusive ao redimensionar)
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (!FeatureGuard.guard(FEATURE, false) { screen is PCGUI }) return@register
            cachedSearch = null
            ScreenEvents.afterRender(screen).register { current, graphics, mouseX, mouseY, _ ->
                FeatureGuard.guard(FEATURE, Unit) { render(current as PCGUI, graphics, mouseX, mouseY) }
            }
            ScreenMouseEvents.allowMouseClick(screen).register { current, mouseX, mouseY, button ->
                FeatureGuard.guard(FEATURE, true) { !handleClick(current as PCGUI, mouseX, mouseY, button) }
            }
        }
    }

    private fun render(gui: PCGUI, graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        hitboxes = emptyList()
        val config = ConfigManager.config.boxSearch
        if (!config.enabled || !config.showResultsPanel) return

        val search = gui.search
        if (search === Search.DEFAULT) {
            if (gui.focused is FilterWidget) renderHelp(gui, graphics)
            return
        }
        renderResults(gui, graphics, results(gui, search), mouseX, mouseY)
    }

    private fun results(gui: PCGUI, search: Search): List<BoxResult> {
        val now = Util.getMillis()
        if (search !== cachedSearch || now - cachedAt > REFRESH_MS) {
            cachedSearch = search
            cachedAt = now
            cachedResults = gui.pc.boxes.mapIndexedNotNull { index, box ->
                val count = box.slots.count { it != null && search.passes(it) }
                if (count > 0) BoxResult(index, boxName(box, index), count) else null
            }
        }
        return cachedResults
    }

    private fun renderResults(gui: PCGUI, graphics: GuiGraphics, results: List<BoxResult>, mouseX: Int, mouseY: Int) {
        val font = Minecraft.getInstance().font
        val title = Component.translatable("shifumon.search.results", results.sumOf { it.count })
        val hint = Component.translatable("shifumon.search.click_hint")
        val maxRows = ((PCGUI.BASE_HEIGHT - 48) / ROW_HEIGHT).coerceAtLeast(1)
        val shown = results.take(maxRows)
        val hiddenBoxes = results.size - shown.size
        val more = Component.translatable("shifumon.search.more", hiddenBoxes)

        val rowsWidth = shown.maxOfOrNull { font.width(it.name) + 10 + font.width(it.count.toString()) } ?: 0
        val width = maxOf(font.width(title), font.width(hint), if (hiddenBoxes > 0) font.width(more) else 0, rowsWidth) + 12
        val rowCount = shown.size + if (hiddenBoxes > 0) 1 else 0
        val height = 19 + rowCount * ROW_HEIGHT + 15
        val (x, y) = panelOrigin(gui, width)

        PixelUi.panel(graphics, x, y, width, height, RetroPalette.ACCENT_SEARCH)
        graphics.drawString(font, title, x + 6, y + 7, RetroPalette.LABEL, true)

        val currentBox = gui.storage.box
        val boxes = ArrayList<Hitbox>(shown.size)
        var rowY = y + 19
        for (result in shown) {
            val hitbox = Hitbox(result.index, x + 3, rowY - 1, width - 6, ROW_HEIGHT)
            val isCurrent = result.index == currentBox
            if (hitbox.contains(mouseX.toDouble(), mouseY.toDouble())) {
                graphics.fill(hitbox.x, hitbox.y, hitbox.x + hitbox.width, hitbox.y + hitbox.height, 0x30FFFFFF)
            }
            if (isCurrent) graphics.fill(x + 3, rowY - 1, x + 4, rowY + ROW_HEIGHT - 1, RetroPalette.ACCENT_SEARCH)
            graphics.drawString(font, result.name, x + 6, rowY + 1, if (isCurrent) RetroPalette.TEXT else RetroPalette.TEXT_DIM, true)
            val count = result.count.toString()
            graphics.drawString(font, count, x + width - 6 - font.width(count), rowY + 1, RetroPalette.SHINY, true)
            boxes += hitbox
            rowY += ROW_HEIGHT
        }
        if (hiddenBoxes > 0) {
            graphics.drawString(font, more, x + 6, rowY + 1, RetroPalette.UNKNOWN, true)
            rowY += ROW_HEIGHT
        }
        graphics.drawString(font, hint, x + 6, rowY + 3, RetroPalette.UNKNOWN, true)
        hitboxes = boxes
    }

    private fun renderHelp(gui: PCGUI, graphics: GuiGraphics) {
        val font = Minecraft.getInstance().font
        val title = Component.translatable("shifumon.search.help.title")
        val lines = helpKeys.map { Component.translatable(it) }
        val width = maxOf(font.width(title), lines.maxOf { font.width(it) }) + 12
        val height = 19 + lines.size * ROW_HEIGHT + 4
        val (x, y) = panelOrigin(gui, width)

        PixelUi.panel(graphics, x, y, width, height, RetroPalette.ACCENT_SEARCH)
        graphics.drawString(font, title, x + 6, y + 7, RetroPalette.LABEL, true)
        lines.forEachIndexed { index, line ->
            graphics.drawString(font, line, x + 6, y + 20 + index * ROW_HEIGHT, RetroPalette.TEXT_DIM, true)
        }
    }

    private fun handleClick(gui: PCGUI, mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false
        val hit = hitboxes.firstOrNull { it.contains(mouseX, mouseY) } ?: return false
        if (gui.storage.box != hit.boxIndex) {
            gui.storage.box = hit.boxIndex
            Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f))
        }
        return true
    }

    /** À esquerda da janela do PC; encosta na borda da tela se não houver espaço. */
    private fun panelOrigin(gui: PCGUI, width: Int): Pair<Int, Int> {
        val pcLeft = (gui.width - PCGUI.BASE_WIDTH) / 2
        val x = (pcLeft - width - 4).coerceAtLeast(2)
        val y = ((gui.height - PCGUI.BASE_HEIGHT) / 2 + 8).coerceAtLeast(2)
        return x to y
    }

    private fun boxName(box: ClientBox, index: Int): Component {
        val name = box.name?.string
        return if (name.isNullOrBlank()) Component.translatable("shifumon.search.box", index + 1) else Component.literal(name)
    }
}
