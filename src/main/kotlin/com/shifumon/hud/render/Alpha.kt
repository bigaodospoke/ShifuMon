package com.shifumon.hud.render

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics

/** Transparência global para retângulos, texto e texturas (a batalha minimizada fica semitransparente). */
object Alpha {
    inline fun draw(graphics: GuiGraphics, alpha: Float, block: () -> Unit) {
        if (alpha >= 0.999f) {
            block()
            return
        }
        graphics.flush()
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha.coerceIn(0f, 1f))
        try {
            block()
            graphics.flush()
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }
    }
}
