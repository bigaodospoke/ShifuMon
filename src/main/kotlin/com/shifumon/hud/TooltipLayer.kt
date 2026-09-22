package com.shifumon.hud

import com.shifumon.hud.panel.Panel
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.gui.GuiGraphics

/**
 * Dicas flutuantes por cima de toda a tela. Quem detecta o mouse (um botão de golpe, por exemplo)
 * é desenhado no meio da tela, então a dica só é guardada e sai no fim do quadro, sobre o resto.
 */
object TooltipLayer {
    private const val MOUSE_OFFSET = 12
    private const val SCREEN_MARGIN = 2

    private var pending: Panel? = null
    private var mouseX = 0
    private var mouseY = 0

    fun register() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            ScreenEvents.beforeRender(screen).register { _, _, _, _, _ -> pending = null }
            ScreenEvents.afterRender(screen).register { _, graphics, _, _, _ -> flush(graphics) }
        }
    }

    /** Pede a dica neste quadro, ao lado do mouse. */
    fun show(panel: Panel, mouseX: Int, mouseY: Int) {
        pending = panel
        this.mouseX = mouseX
        this.mouseY = mouseY
    }

    private fun flush(graphics: GuiGraphics) {
        val panel = pending ?: return
        pending = null
        // À direita do mouse; à esquerda se não couber
        var x = mouseX + MOUSE_OFFSET
        if (x + panel.width > graphics.guiWidth() - SCREEN_MARGIN) x = mouseX - MOUSE_OFFSET - panel.width
        val y = (mouseY - MOUSE_OFFSET).coerceAtMost(graphics.guiHeight() - panel.height - SCREEN_MARGIN)

        graphics.pose().pushPose()
        graphics.pose().translate(0f, 0f, 400f)
        panel.render(graphics, x.coerceAtLeast(SCREEN_MARGIN), y.coerceAtLeast(SCREEN_MARGIN))
        graphics.pose().popPose()
    }
}
