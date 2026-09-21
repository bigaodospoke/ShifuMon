package com.shifumon.battle.log

import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane
import com.shifumon.config.ConfigManager
import com.shifumon.hud.render.Alpha
import com.shifumon.hud.render.HudIcons
import com.shifumon.hud.render.PixelUi
import com.shifumon.hud.render.RetroPalette
import com.shifumon.hud.render.StatusStyle
import com.shifumon.hud.render.TinyFont
import com.shifumon.mixin.cobblemon.BattleMessagePaneInvoker
import com.shifumon.util.Colors
import com.shifumon.util.FeatureGuard
import com.shifumon.util.TexturePackCheck
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import kotlin.math.roundToInt

/**
 * Histórico de batalha pixel art: divisória por turno, barra na cor do tipo do golpe e etiquetas
 * para crítico, eficácia, desmaio, troca, boosts, status, cura, dano e clima.
 *
 * A geometria é a mesma do painel do Cobblemon (169 px, texto em x+5, rolagem em x+154,
 * expandir em x+160), porque os cliques e a rolagem continuam sendo tratados por ele.
 */
object BattleLogRenderer {
    private const val FRAME_WIDTH = BattleMessagePane.FRAME_WIDTH
    private const val TEXT_BOX_HEIGHT = BattleMessagePane.TEXT_BOX_HEIGHT
    private const val TEXT_X = 5
    private const val TEXT_Y = 6
    private const val TEXT_WIDTH = 147
    private const val GUTTER = 18
    private const val LINE_HEIGHT = 10
    private const val SCROLLBAR_X = 154
    private const val TOGGLE_X = 160

    private const val SUPER_COLOR = 0xFF2FA84F.toInt()
    private const val WEAK_COLOR = 0xFFC07820.toInt()
    private const val NEUTRAL_COLOR = 0xFF5A5A6C.toInt()
    private const val CRIT_COLOR = 0xFFD89A10.toInt()
    private const val SWITCH_COLOR = 0xFF3C78D8.toInt()

    private class Row(val height: Int, val draw: (GuiGraphics, Int, Int) -> Unit)

    private var cacheKey: Any? = null
    private var cachedRows: List<Row> = emptyList()

    @JvmStatic
    fun render(pane: BattleMessagePane, graphics: GuiGraphics): Boolean {
        val config = ConfigManager.config.battleHud
        if (!config.restyleBattleLog || TexturePackCheck.shouldYieldTo(TexturePackCheck.BATTLE_LOG)) return false
        return FeatureGuard.guard("battle.log", false) {
            draw(pane, graphics)
            true
        }
    }

    private fun draw(pane: BattleMessagePane, graphics: GuiGraphics) {
        (pane as Any as BattleMessagePaneInvoker).`shifumon$correctSize`()
        val x = pane.x
        val y = pane.appropriateY
        val boxHeight = pane.height
        val expanded = boxHeight > TEXT_BOX_HEIGHT
        val frameHeight = if (expanded) BattleMessagePane.FRAME_EXPANDED_HEIGHT else BattleMessagePane.FRAME_HEIGHT

        val rows = rows(pane)
        val contentHeight = rows.sumOf { it.height }
        // A rolagem do Cobblemon conta as linhas dele; usamos a mesma proporção sobre as nossas
        val ratio = if (pane.maxScroll > 0) (pane.scrollAmount / pane.maxScroll).toFloat().coerceIn(0f, 1f) else 1f
        val offset = ((contentHeight - boxHeight).coerceAtLeast(0) * ratio).roundToInt()

        Alpha.draw(graphics, pane.opacity) {
            PixelUi.panel(graphics, x, y, FRAME_WIDTH, frameHeight, RetroPalette.ACCENT_BATTLE)
            graphics.fill(x + TEXT_X - 1, y + TEXT_Y - 1, x + TEXT_X + TEXT_WIDTH + 1, y + TEXT_Y + boxHeight + 1, 0x50000000)

            val boxTop = y + TEXT_Y
            val boxBottom = boxTop + boxHeight
            graphics.enableScissor(x + TEXT_X, boxTop, x + TEXT_X + TEXT_WIDTH, boxBottom)
            var rowY = boxTop - offset
            for (row in rows) {
                if (rowY + row.height > boxTop && rowY < boxBottom) row.draw(graphics, x + TEXT_X, rowY)
                rowY += row.height
            }
            graphics.disableScissor()

            drawScrollbar(graphics, x + SCROLLBAR_X, boxTop, boxHeight, contentHeight, ratio)
            drawToggle(graphics, x + TOGGLE_X, y + if (expanded) 92 else 46, expanded)
        }
    }

    private fun rows(pane: BattleMessagePane): List<Row> {
        val battleId = CobblemonClient.battle?.battleId
        val entries = BattleLog.entries(battleId)
        if (entries.isEmpty()) return fallbackRows(pane)

        val key = Triple(BattleLog.revision, battleId, Minecraft.getInstance().languageManager.selected)
        if (key != cacheKey) {
            cachedRows = entries.map(::row)
            cacheKey = key
        }
        return cachedRows
    }

    /** Sem histórico próprio (ex.: batalha já em andamento ao carregar), mostra as linhas do Cobblemon. */
    private fun fallbackRows(pane: BattleMessagePane): List<Row> = pane.children().map { line ->
        Row(LINE_HEIGHT) { graphics, x, y -> graphics.drawString(font(), line.line, x + GUTTER, y + 1, RetroPalette.TEXT_DIM, true) }
    }

    private fun row(entry: LogEntry): Row = when (entry.kind) {
        LogKind.TURN -> turnRow(entry.turn)
        LogKind.SUPER_EFFECTIVE -> badgeRow("shifumon.log.super_effective", SUPER_COLOR, entry.target)
        LogKind.NOT_EFFECTIVE -> badgeRow("shifumon.log.not_effective", WEAK_COLOR, entry.target)
        LogKind.IMMUNE -> badgeRow("shifumon.log.immune", NEUTRAL_COLOR, entry.target)
        LogKind.CRITICAL -> badgeRow("shifumon.log.critical", CRIT_COLOR, entry.target)
        LogKind.MISS -> badgeRow("shifumon.log.miss", NEUTRAL_COLOR, null)
        LogKind.MOVE -> textRow(entry.text, RetroPalette.TEXT) { graphics, x, y, height ->
            graphics.fill(x + 2, y, x + 5, y + height - 1, entry.color ?: RetroPalette.TEXT_DIM)
        }
        LogKind.FAINT -> textRow(entry.text, Colors.lighten(RetroPalette.NEGATIVE, 0.3f), gutterChip("KO", RetroPalette.BOOST_DOWN))
        LogKind.SWITCH_IN -> textRow(entry.text, RetroPalette.TEXT, gutterChip(">", SWITCH_COLOR))
        LogKind.SWITCH_OUT -> textRow(entry.text, RetroPalette.TEXT_DIM, gutterChip("<", SWITCH_COLOR))
        LogKind.BOOST -> textRow(entry.text, RetroPalette.TEXT, gutterArrow(up = true, RetroPalette.POSITIVE))
        LogKind.UNBOOST -> textRow(entry.text, RetroPalette.TEXT, gutterArrow(up = false, RetroPalette.NEGATIVE))
        LogKind.STATUS -> {
            val status = entry.detail
            val gutter = if (status != null) gutterChip(StatusStyle.label(status), StatusStyle.color(status)) else gutterDot()
            textRow(entry.text, RetroPalette.TEXT, gutter)
        }
        LogKind.HEAL -> textRow(entry.text, RetroPalette.TEXT, gutterChip("+", RetroPalette.BOOST_UP))
        LogKind.DAMAGE -> textRow(entry.text, RetroPalette.TEXT_DIM, gutterChip("-", RetroPalette.BOOST_DOWN))
        LogKind.WEATHER -> textRow(entry.text, RetroPalette.TEXT, gutterIcon(entry.detail?.let(HudIcons::weather)))
        LogKind.TERRAIN -> textRow(entry.text, RetroPalette.TEXT, gutterIcon(entry.detail?.let(HudIcons::terrain)))
        LogKind.RESULT -> textRow(entry.text, RetroPalette.LABEL, gutterChip("!", CRIT_COLOR))
        LogKind.OTHER -> textRow(entry.text, RetroPalette.TEXT_DIM, gutterDot())
    }

    /** ── [TURNO 4] ── */
    private fun turnRow(turn: Int): Row {
        val label = TinyFont.sanitize(I18n.get("shifumon.log.turn", turn))
        val chipWidth = TinyFont.width(label) + 4
        return Row(13) { graphics, x, y ->
            val chipX = x + (TEXT_WIDTH - chipWidth) / 2
            val lineY = y + 6
            graphics.fill(x + 2, lineY, chipX - 3, lineY + 1, RetroPalette.SEPARATOR)
            graphics.fill(chipX + chipWidth + 3, lineY, x + TEXT_WIDTH - 2, lineY + 1, RetroPalette.SEPARATOR)
            PixelUi.chip(graphics, label, chipX, y + 2, Colors.darken(RetroPalette.ACCENT_BATTLE, 0.45f), RetroPalette.LABEL)
        }
    }

    /** Etiqueta curta ("Super efetivo!") com o alvo ao lado, quando houver. */
    private fun badgeRow(labelKey: String, color: Int, target: Component?): Row {
        val label = Component.translatable(labelKey)
        val labelWidth = font().width(label) + 6
        val targetLine = target?.let { font().split(it, TEXT_WIDTH - GUTTER - labelWidth - 4).firstOrNull() }
        return Row(13) { graphics, x, y ->
            PixelUi.badge(graphics, font(), label, x + GUTTER, y + 1, color)
            targetLine?.let { graphics.drawString(font(), it, x + GUTTER + labelWidth + 4, y + 3, RetroPalette.TEXT_DIM, true) }
        }
    }

    private fun textRow(text: Component, color: Int, gutter: (GuiGraphics, Int, Int, Int) -> Unit): Row {
        val lines = font().split(text, TEXT_WIDTH - GUTTER)
        val height = lines.size.coerceAtLeast(1) * LINE_HEIGHT + 1
        return Row(height) { graphics, x, y ->
            gutter(graphics, x, y, height)
            lines.forEachIndexed { index, line -> graphics.drawString(font(), line, x + GUTTER, y + 1 + index * LINE_HEIGHT, color, true) }
        }
    }

    private fun gutterChip(label: String, color: Int): (GuiGraphics, Int, Int, Int) -> Unit = { graphics, x, y, _ ->
        PixelUi.chip(graphics, label, x + 1, y, color, RetroPalette.TEXT)
    }

    private fun gutterIcon(icon: ResourceLocation?): (GuiGraphics, Int, Int, Int) -> Unit = { graphics, x, y, _ ->
        icon?.let { graphics.blit(it, x + 3, y, HudIcons.SIZE, HudIcons.SIZE, 0f, 0f, HudIcons.SIZE, HudIcons.SIZE, HudIcons.SIZE, HudIcons.SIZE) }
    }

    private fun gutterDot(): (GuiGraphics, Int, Int, Int) -> Unit = { graphics, x, y, _ ->
        graphics.fill(x + 6, y + 4, x + 8, y + 6, RetroPalette.UNKNOWN)
    }

    /** Seta pixel 5x3 (▲ boost, ▼ queda). */
    private fun gutterArrow(up: Boolean, color: Int): (GuiGraphics, Int, Int, Int) -> Unit = { graphics, x, y, _ ->
        val widths = if (up) intArrayOf(1, 3, 5) else intArrayOf(5, 3, 1)
        widths.forEachIndexed { index, rowWidth ->
            val startX = x + 5 + (5 - rowWidth) / 2
            graphics.fill(startX, y + 3 + index, startX + rowWidth, y + 4 + index, color)
        }
    }

    private fun drawScrollbar(graphics: GuiGraphics, x: Int, y: Int, height: Int, contentHeight: Int, ratio: Float) {
        graphics.fill(x, y, x + 3, y + height, 0x40000000)
        if (contentHeight <= height) return
        val thumb = (height * height / contentHeight).coerceAtLeast(6)
        val thumbY = y + ((height - thumb) * ratio).roundToInt()
        graphics.fill(x, thumbY, x + 3, thumbY + thumb, RetroPalette.ACCENT_BATTLE)
        graphics.fill(x, thumbY, x + 1, thumbY + thumb, Colors.lighten(RetroPalette.ACCENT_BATTLE, 0.4f))
    }

    /** Seta de expandir/recolher na área 5x5 que o Cobblemon usa para o clique. */
    private fun drawToggle(graphics: GuiGraphics, x: Int, y: Int, expanded: Boolean) {
        val widths = if (expanded) intArrayOf(5, 3, 1) else intArrayOf(1, 3, 5)
        widths.forEachIndexed { index, rowWidth ->
            val startX = x + (5 - rowWidth) / 2
            graphics.fill(startX, y + 1 + index, startX + rowWidth, y + 2 + index, RetroPalette.LABEL)
        }
    }

    private fun font(): Font = Minecraft.getInstance().font
}
