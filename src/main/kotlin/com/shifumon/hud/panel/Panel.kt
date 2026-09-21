package com.shifumon.hud.panel

import com.shifumon.hud.render.PixelUi
import net.minecraft.client.gui.GuiGraphics

/**
 * Painel pixel art composto por linhas. Ele se mede sozinho: os elementos da HUD só descrevem
 * o conteúdo (via [panel]) e quem desenha decide a posição.
 */
class Panel internal constructor(
    private val rows: List<PanelRow>,
    private val accent: Int?,
    minContentWidth: Int,
) {
    /** Medidas só das linhas, sem moldura (para compor painéis maiores, como o bloco de batalha). */
    val contentWidth: Int = maxOf(minContentWidth, rows.maxOfOrNull { it.width } ?: 0)
    val contentHeight: Int = rows.sumOf { it.height } + ROW_GAP * (rows.size - 1).coerceAtLeast(0)

    val width: Int = contentWidth + PAD_X * 2
    val height: Int = contentHeight + PAD_TOP + PAD_BOTTOM

    fun render(graphics: GuiGraphics, x: Int, y: Int) {
        PixelUi.panel(graphics, x, y, width, height, accent)
        renderRows(graphics, x + PAD_X, y + PAD_TOP, contentWidth)
    }

    /** Desenha só as linhas, sem moldura, dentro de uma área de largura [innerWidth]. */
    fun renderRows(graphics: GuiGraphics, x: Int, y: Int, innerWidth: Int) {
        var cursorY = y
        for (row in rows) {
            row.render(graphics, x, cursorY, innerWidth)
            cursorY += row.height + ROW_GAP
        }
    }

    companion object {
        const val PAD_X = 5
        const val PAD_TOP = 6
        const val PAD_BOTTOM = 4
        const val ROW_GAP = 2
    }
}
