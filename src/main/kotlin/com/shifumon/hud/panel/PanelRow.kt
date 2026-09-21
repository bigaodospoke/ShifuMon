package com.shifumon.hud.panel

import net.minecraft.client.gui.GuiGraphics

abstract class PanelRow {
    abstract val width: Int
    abstract val height: Int
    abstract fun render(graphics: GuiGraphics, x: Int, y: Int, innerWidth: Int)
}

/** Linha com itens alinhados à esquerda e, opcionalmente, itens colados à direita. */
class InlineRow(
    private val left: List<Inline>,
    private val right: List<Inline>,
    private val gap: Int,
) : PanelRow() {
    private val leftWidth = span(left)
    private val rightWidth = span(right)

    override val width: Int = leftWidth + if (right.isEmpty()) 0 else RIGHT_MARGIN + rightWidth
    override val height: Int = maxOf(left.maxOfOrNull { it.height } ?: 0, right.maxOfOrNull { it.height } ?: 0)

    override fun render(graphics: GuiGraphics, x: Int, y: Int, innerWidth: Int) {
        draw(graphics, left, x, y)
        if (right.isNotEmpty()) draw(graphics, right, x + innerWidth - rightWidth, y)
    }

    private fun draw(graphics: GuiGraphics, items: List<Inline>, startX: Int, y: Int) {
        var cursorX = startX
        for (item in items) {
            item.render(graphics, cursorX, y + (height - item.height) / 2)
            cursorX += item.width + gap
        }
    }

    private fun span(items: List<Inline>): Int = if (items.isEmpty()) 0 else items.sumOf { it.width } + gap * (items.size - 1)

    private companion object {
        const val RIGHT_MARGIN = 8
    }
}

class SeparatorRow(private val color: Int) : PanelRow() {
    override val width = 0
    override val height = 3

    override fun render(graphics: GuiGraphics, x: Int, y: Int, innerWidth: Int) {
        graphics.fill(x, y + 1, x + innerWidth, y + 2, color)
    }
}
