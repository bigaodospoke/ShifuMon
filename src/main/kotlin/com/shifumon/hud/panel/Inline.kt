package com.shifumon.hud.panel

import com.shifumon.hud.render.PixelUi
import com.shifumon.hud.render.RetroPalette
import com.shifumon.hud.render.TinyFont
import com.shifumon.hud.render.TypeIcons
import com.shifumon.shiny.ShinyIcons
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/** Um item dentro de uma linha do painel. Medidas em pixels da interface. */
interface Inline {
    val width: Int
    val height: Int
    fun render(graphics: GuiGraphics, x: Int, y: Int)
}

private val font: Font get() = Minecraft.getInstance().font

class TextInline(private val text: Component, private val color: Int) : Inline {
    override val width = font.width(text)
    override val height = 8

    override fun render(graphics: GuiGraphics, x: Int, y: Int) {
        graphics.drawString(font, text, x, y, color, true)
    }
}

class TinyTextInline(text: String, private val color: Int) : Inline {
    private val text = TinyFont.sanitize(text)
    override val width = TinyFont.width(this.text) + 1
    override val height = TinyFont.HEIGHT + 1

    override fun render(graphics: GuiGraphics, x: Int, y: Int) {
        TinyFont.draw(graphics, text, x, y, color, shadow = true)
    }
}

class IconInline(private val texture: ResourceLocation, private val size: Int, private val textureSize: Int) : Inline {
    override val width = size
    override val height = size

    override fun render(graphics: GuiGraphics, x: Int, y: Int) {
        graphics.blit(texture, x, y, size, size, 0f, 0f, textureSize, textureSize, textureSize, textureSize)
    }
}

class ShinyInline(private val size: Int) : Inline {
    override val width = size
    override val height = size

    override fun render(graphics: GuiGraphics, x: Int, y: Int) {
        ShinyIcons.draw(graphics, null, x, y, size)
    }
}

class BadgeInline(private val text: Component, private val color: Int) : Inline {
    override val width = font.width(text) + 6
    override val height = PixelUi.BADGE_HEIGHT

    override fun render(graphics: GuiGraphics, x: Int, y: Int) {
        PixelUi.badge(graphics, font, text, x, y, color)
    }
}

class ChipInline(text: String, private val background: Int, private val foreground: Int) : Inline {
    private val text = TinyFont.sanitize(text)
    override val width = TinyFont.width(this.text) + 4
    override val height = PixelUi.CHIP_HEIGHT

    override fun render(graphics: GuiGraphics, x: Int, y: Int) {
        PixelUi.chip(graphics, text, x, y, background, foreground)
    }
}

/** Etiqueta de tipo: símbolo quando existe desenho, três letras quando não. */
class TypeChipInline(private val texture: ResourceLocation?, private val text: String, private val background: Int) : Inline {
    override val width = if (texture != null) TypeIcons.SIZE + 4 else TinyFont.width(text) + 4
    override val height = PixelUi.CHIP_HEIGHT

    override fun render(graphics: GuiGraphics, x: Int, y: Int) {
        if (texture != null) PixelUi.typeChip(graphics, texture, x, y, background)
        else PixelUi.chip(graphics, text, x, y, background, RetroPalette.TEXT)
    }
}

class SwatchInline(private val color: Int, override val width: Int, override val height: Int) : Inline {
    override fun render(graphics: GuiGraphics, x: Int, y: Int) {
        PixelUi.box(graphics, x, y, width, height, color)
    }
}

class HpBarInline(override val width: Int, private val ratio: Float) : Inline {
    override val height = PixelUi.HP_BAR_HEIGHT

    override fun render(graphics: GuiGraphics, x: Int, y: Int) {
        PixelUi.hpBar(graphics, x, y, width, ratio)
    }
}

class SpacerInline(override val width: Int) : Inline {
    override val height = 0

    override fun render(graphics: GuiGraphics, x: Int, y: Int) = Unit
}
