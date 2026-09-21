package com.shifumon.hud.render

import com.shifumon.util.Colors
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

/**
 * Primitivas de desenho em pixel art. Tudo é feito com retângulos em coordenadas inteiras da
 * interface, então nada fica borrado em nenhuma escala de GUI.
 */
object PixelUi {
    const val BADGE_HEIGHT = 11
    const val CHIP_HEIGHT = 9
    const val HP_BAR_HEIGHT = 7

    /** Caixa com cantos cortados, chanfro de 1px e faixa de destaque no topo. */
    fun panel(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, accent: Int?) {
        outline(graphics, x, y, width, height, RetroPalette.OUTLINE)
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, RetroPalette.BODY)
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, RetroPalette.BEVEL_LIGHT)
        graphics.fill(x + 1, y + 2, x + 2, y + height - 1, RetroPalette.BEVEL_LIGHT)
        graphics.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, RetroPalette.BEVEL_DARK)
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 2, RetroPalette.BEVEL_DARK)
        if (accent != null) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, accent)
            graphics.fill(x + 2, y + 3, x + width - 2, y + 4, Colors.darken(accent, 0.55f))
        }
    }

    /** Contorno de 1px sem os pixels dos cantos (visual arredondado de sprite). */
    fun outline(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, color: Int) {
        graphics.fill(x + 1, y, x + width - 1, y + 1, color)
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, color)
        graphics.fill(x, y + 1, x + 1, y + height - 1, color)
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color)
    }

    /** Bloco colorido com contorno escuro e brilho de 1px no topo. */
    fun box(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, fill: Int) {
        outline(graphics, x, y, width, height, Colors.darken(fill, 0.4f))
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill)
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, Colors.lighten(fill, 0.3f))
    }

    /** Etiqueta com a fonte do Minecraft (tipos). */
    fun badge(graphics: GuiGraphics, font: Font, text: Component, x: Int, y: Int, color: Int) {
        box(graphics, x, y, font.width(text) + 6, BADGE_HEIGHT, color)
        graphics.drawString(font, text, x + 3, y + 2, RetroPalette.TEXT, true)
    }

    /** Etiqueta pequena com a fonte 3x5 (status, boosts, eficácia). */
    fun chip(graphics: GuiGraphics, text: String, x: Int, y: Int, background: Int, foreground: Int) {
        box(graphics, x, y, TinyFont.width(text) + 4, CHIP_HEIGHT, background)
        TinyFont.draw(graphics, text, x + 2, y + 2, foreground)
    }

    /** Barra de HP no estilo Gen 3: rótulo "HP", trilho escuro e cor por faixa de vida. */
    fun hpBar(graphics: GuiGraphics, x: Int, y: Int, width: Int, ratio: Float) {
        outline(graphics, x, y, width, HP_BAR_HEIGHT, RetroPalette.HP_FRAME)
        graphics.fill(x + 1, y + 1, x + width - 1, y + HP_BAR_HEIGHT - 1, RetroPalette.HP_BACKGROUND)
        TinyFont.draw(graphics, "HP", x + 2, y + 1, RetroPalette.LABEL)

        val barX = x + 10
        val barWidth = width - 11
        if (barWidth <= 0) return
        graphics.fill(barX, y + 1, barX + barWidth, y + 6, RetroPalette.HP_TRACK)

        val clamped = ratio.coerceIn(0f, 1f)
        var filled = (barWidth * clamped).roundToInt()
        if (clamped > 0f && filled == 0) filled = 1
        if (filled == 0) return

        val (base, light) = when {
            clamped > 0.5f -> RetroPalette.HP_GREEN to RetroPalette.HP_GREEN_LIGHT
            clamped > 0.2f -> RetroPalette.HP_YELLOW to RetroPalette.HP_YELLOW_LIGHT
            else -> RetroPalette.HP_RED to RetroPalette.HP_RED_LIGHT
        }
        graphics.fill(barX, y + 1, barX + filled, y + 6, base)
        graphics.fill(barX, y + 1, barX + filled, y + 2, light)
        graphics.fill(barX, y + 5, barX + filled, y + 6, Colors.darken(base, 0.7f))
    }

    /** Retângulo tracejado (seleção no editor de HUD). */
    fun dashedRect(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, color: Int) {
        var offset = 0
        while (offset < width) {
            val end = minOf(offset + 2, width)
            graphics.fill(x + offset, y, x + end, y + 1, color)
            graphics.fill(x + offset, y + height - 1, x + end, y + height, color)
            offset += 4
        }
        offset = 0
        while (offset < height) {
            val end = minOf(offset + 2, height)
            graphics.fill(x, y + offset, x + 1, y + end, color)
            graphics.fill(x + width - 1, y + offset, x + width, y + end, color)
            offset += 4
        }
    }
}
