package com.shifumon.hud.panel

import com.shifumon.hud.render.HudIcons
import com.shifumon.hud.render.RetroPalette
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

@DslMarker
annotation class PanelDsl

/**
 * DSL para descrever painéis:
 * ```
 * panel(accent = RetroPalette.ACCENT_INFO) {
 *     row { shiny(); text(nome); right { text(nivel) } }
 *     separator()
 *     labeled(rotulo, valor)
 * }
 * ```
 * Retorna `null` quando nenhuma linha foi adicionada.
 */
fun panel(accent: Int?, minWidth: Int = 0, block: PanelBuilder.() -> Unit): Panel? =
    PanelBuilder().apply(block).build(accent, minWidth)

@PanelDsl
class PanelBuilder internal constructor() {
    private val rows = mutableListOf<PanelRow>()

    fun row(gap: Int = 3, block: RowBuilder.() -> Unit) {
        val builder = RowBuilder().apply(block)
        if (!builder.isEmpty()) rows.add(builder.build(gap))
    }

    fun separator(color: Int = RetroPalette.SEPARATOR) {
        if (rows.isNotEmpty() && rows.last() !is SeparatorRow) rows.add(SeparatorRow(color))
    }

    fun labeled(label: Component, value: Component, valueColor: Int = RetroPalette.TEXT, extra: RowBuilder.() -> Unit = {}) =
        row {
            text(label, RetroPalette.LABEL)
            text(value, valueColor)
            extra()
        }

    internal fun build(accent: Int?, minWidth: Int): Panel? {
        while (rows.lastOrNull() is SeparatorRow) rows.removeAt(rows.lastIndex)
        return if (rows.isEmpty()) null else Panel(rows.toList(), accent, minWidth)
    }
}

@PanelDsl
class RowBuilder internal constructor() {
    private val left = mutableListOf<Inline>()
    private val right = mutableListOf<Inline>()
    private var target = left

    fun text(text: Component, color: Int = RetroPalette.TEXT) {
        target.add(TextInline(text, color))
    }

    fun text(text: String, color: Int = RetroPalette.TEXT) = text(Component.literal(text), color)

    /** Texto na fonte pixel 3x5 (só ASCII maiúsculo, números e + - / % . :). */
    fun tiny(text: String, color: Int = RetroPalette.TEXT) {
        target.add(TinyTextInline(text, color))
    }

    fun icon(texture: ResourceLocation, size: Int = HudIcons.SIZE, textureSize: Int = size) {
        target.add(IconInline(texture, size, textureSize))
    }

    fun shiny(size: Int = 8) {
        target.add(ShinyInline(size))
    }

    fun badge(text: Component, color: Int) {
        target.add(BadgeInline(text, color))
    }

    fun chip(text: String, background: Int, foreground: Int = RetroPalette.TEXT) {
        target.add(ChipInline(text, background, foreground))
    }

    fun swatch(color: Int, width: Int = 5, height: Int = 7) {
        target.add(SwatchInline(color, width, height))
    }

    fun hpBar(width: Int, ratio: Float) {
        target.add(HpBarInline(width, ratio))
    }

    fun space(width: Int) {
        target.add(SpacerInline(width))
    }

    /** Itens adicionados dentro do bloco ficam alinhados à direita da linha. */
    fun right(block: RowBuilder.() -> Unit) {
        val previous = target
        target = right
        block()
        target = previous
    }

    internal fun isEmpty() = left.isEmpty() && right.isEmpty()

    internal fun build(gap: Int): PanelRow = InlineRow(left.toList(), right.toList(), gap)
}
