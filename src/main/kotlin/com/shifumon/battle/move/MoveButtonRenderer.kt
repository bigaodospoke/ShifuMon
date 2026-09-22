package com.shifumon.battle.move

import com.cobblemon.mod.common.api.moves.categories.DamageCategories
import com.cobblemon.mod.common.battles.InBattleMove
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection
import com.shifumon.battle.BattleReader
import com.shifumon.battle.Effectiveness
import com.shifumon.battle.TypeChart
import com.shifumon.config.ConfigManager
import com.shifumon.hud.render.Alpha
import com.shifumon.hud.render.CategoryStyle
import com.shifumon.hud.render.PixelUi
import com.shifumon.hud.render.RetroPalette
import com.shifumon.hud.render.TinyFont
import com.shifumon.hud.render.TypeColors
import com.shifumon.util.Colors
import com.shifumon.util.FeatureGuard
import com.shifumon.util.TexturePackCheck
import com.shifumon.util.TypeNames
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth

/**
 * Botão de golpe pixel art no lugar do botão do Cobblemon (92x24):
 * ```
 * [FOG] Lança-chamas        |▲|
 * [ESP] 12/15      SUPER    |▲|
 * ```
 * A eficácia aparece de três formas ao mesmo tempo: faixa colorida na borda, setas (uma para x2,
 * duas para x4, para baixo quando resiste, X quando é imune) e a palavra na fonte do jogo.
 * Golpe neutro não mostra nada, para o botão não ficar poluído.
 */
object MoveButtonRenderer {
    private const val WIDTH = BattleMoveSelection.MOVE_WIDTH
    private const val HEIGHT = BattleMoveSelection.MOVE_HEIGHT
    private const val STRIPE_WIDTH = 7

    @JvmStatic
    fun render(tile: BattleMoveSelection.MoveTile, graphics: GuiGraphics, mouseX: Int, mouseY: Int): Boolean {
        val config = ConfigManager.config.battleHud
        if (!config.restyleMoveButtons || TexturePackCheck.shouldYieldTo(TexturePackCheck.MOVE_BUTTON)) return false
        return FeatureGuard.guard("battle.move_buttons", false) {
            draw(tile, graphics, mouseX, mouseY, config.showMoveEffectiveness)
            true
        }
    }

    private fun draw(tile: BattleMoveSelection.MoveTile, graphics: GuiGraphics, mouseX: Int, mouseY: Int, showEffectiveness: Boolean) {
        val font = Minecraft.getInstance().font
        val template = tile.moveTemplate
        val type = tile.elementalType
        val selectable = tile.selectable
        val hovered = selectable && tile.isHovered(mouseX.toDouble(), mouseY.toDouble())
        val x = Mth.floor(tile.x)
        val y = Mth.floor(tile.y)
        val typeColor = TypeColors.of(type)
        val opacity = tile.moveSelection.opacity * if (selectable) 1f else 0.5f

        val multipliers = if (showEffectiveness) effectiveness(tile) else emptyList()
        val highlight = multipliers.maxOrNull()?.takeIf { multipliers.size > 1 || it != 1.0 }
        val stripe = highlight != null

        Alpha.draw(graphics, opacity) {
            val body = if (hovered) Colors.lighten(typeColor, 0.12f) else Colors.darken(typeColor, 0.78f)
            PixelUi.box(graphics, x, y, WIDTH, HEIGHT, body)
            graphics.fill(x + 1, y + 13, x + WIDTH - 1, y + HEIGHT - 1, Colors.darken(body, 0.65f))
            if (hovered) PixelUi.outline(graphics, x - 1, y - 1, WIDTH + 2, HEIGHT + 2, RetroPalette.TEXT)

            // Linha 1: tipo + nome
            val typeLabel = TypeNames.short(type.name)
            PixelUi.chip(graphics, typeLabel, x + 3, y + 2, Colors.darken(typeColor, 0.55f), RetroPalette.TEXT)
            val nameX = x + 3 + TinyFont.width(typeLabel) + 4 + 3
            val nameLimit = x + WIDTH - 3 - (if (stripe) STRIPE_WIDTH else 0) - nameX
            graphics.drawString(font, fit(font, template.displayName.string, nameLimit), nameX, y + 3, RetroPalette.TEXT, true)

            // Linha 2: categoria + PP
            val category = CategoryStyle.label(template.damageCategory)
            PixelUi.chip(graphics, category, x + 3, y + 14, CategoryStyle.color(template.damageCategory), RetroPalette.TEXT)
            val ppX = x + 3 + TinyFont.width(category) + 4 + 3
            TinyFont.draw(graphics, ppText(tile.move), ppX, y + 16, ppColor(tile.move), shadow = true)

            if (highlight != null) {
                drawStripe(graphics, x, y, highlight)
                drawLabel(graphics, font, x, y, highlight, multipliers)
            }
        }
    }

    /** Faixa colorida na borda direita, com setas indicando o quanto o golpe é efetivo. */
    private fun drawStripe(graphics: GuiGraphics, x: Int, y: Int, multiplier: Double) {
        val color = Effectiveness.color(multiplier)
        val stripeX = x + WIDTH - 1 - STRIPE_WIDTH
        graphics.fill(stripeX, y + 1, stripeX + STRIPE_WIDTH, y + HEIGHT - 1, color)
        graphics.fill(stripeX, y + 1, stripeX + 1, y + HEIGHT - 1, Colors.lighten(color, 0.35f))
        graphics.fill(stripeX + STRIPE_WIDTH - 1, y + 1, stripeX + STRIPE_WIDTH, y + HEIGHT - 1, Colors.darken(color, 0.6f))

        val iconX = stripeX + 1
        when {
            multiplier == 0.0 -> cross(graphics, iconX, y + 9)
            multiplier >= 4.0 -> {
                arrow(graphics, iconX, y + 5, up = true)
                arrow(graphics, iconX, y + 13, up = true)
            }
            multiplier >= 2.0 -> arrow(graphics, iconX, y + 9, up = true)
            multiplier <= 0.25 -> {
                arrow(graphics, iconX, y + 5, up = false)
                arrow(graphics, iconX, y + 13, up = false)
            }
            else -> arrow(graphics, iconX, y + 9, up = false)
        }
    }

    /** SUPER / FRACO / IMUNE na fonte do jogo; em batalha dupla, os multiplicadores de cada alvo. */
    private fun drawLabel(graphics: GuiGraphics, font: Font, x: Int, y: Int, multiplier: Double, multipliers: List<Double>) {
        val right = x + WIDTH - 3 - STRIPE_WIDTH
        if (multipliers.size > 1) {
            val text = multipliers.joinToString(" ") { Effectiveness.label(it) }
            TinyFont.draw(graphics, TinyFont.sanitize(text), right - TinyFont.width(TinyFont.sanitize(text)), y + 16, Effectiveness.color(multiplier), shadow = true)
            return
        }
        val key = when {
            multiplier == 0.0 -> "shifumon.effectiveness.immune"
            multiplier >= 2.0 -> "shifumon.effectiveness.super"
            else -> "shifumon.effectiveness.weak"
        }
        val label = Component.translatable(key)
        graphics.drawString(font, label, right - font.width(label), y + 15, Effectiveness.textColor(multiplier), true)
    }

    private fun arrow(graphics: GuiGraphics, x: Int, y: Int, up: Boolean) {
        val widths = if (up) intArrayOf(1, 3, 5) else intArrayOf(5, 3, 1)
        widths.forEachIndexed { index, width ->
            val startX = x + (5 - width) / 2
            graphics.fill(startX, y + index, startX + width, y + index + 1, RetroPalette.TEXT)
        }
    }

    private fun cross(graphics: GuiGraphics, x: Int, y: Int) {
        for (i in 0 until 5) {
            graphics.fill(x + i, y + i, x + i + 1, y + i + 1, RetroPalette.TEXT)
            graphics.fill(x + 4 - i, y + i, x + 5 - i, y + i + 1, RetroPalette.TEXT)
        }
    }

    /** Multiplicador contra cada oponente alvo; vazio para golpes de status. */
    private fun effectiveness(tile: BattleMoveSelection.MoveTile): List<Double> {
        if (tile.moveTemplate.damageCategory == DamageCategories.STATUS) return emptyList()
        return opponentTargets(tile).map { multiplier(tile, it) }
    }

    /** Oponentes que o golpe pode atingir. */
    internal fun opponentTargets(tile: BattleMoveSelection.MoveTile): List<ClientBattlePokemon> {
        val user = tile.moveSelection.request.activePokemon
        return tile.targetList.orEmpty()
            .filterIsInstance<ActiveClientBattlePokemon>()
            .filter { !it.isAllied(user) }
            .mapNotNull { it.battlePokemon }
    }

    internal fun multiplier(tile: BattleMoveSelection.MoveTile, target: ClientBattlePokemon): Double =
        TypeChart.multiplier(tile.elementalType.name, BattleReader.types(target).map { it.name })

    internal fun ppText(move: InBattleMove): String =
        if (move.pp == 100 && move.maxpp == 100) "--/--" else "${move.pp}/${move.maxpp}"

    internal fun ppColor(move: InBattleMove): Int = when {
        move.pp == 0 -> RetroPalette.NEGATIVE
        move.pp <= move.maxpp / 2 -> RetroPalette.WARNING
        else -> RetroPalette.TEXT
    }

    private fun fit(font: Font, text: String, maxWidth: Int): String {
        if (font.width(text) <= maxWidth) return text
        return font.plainSubstrByWidth(text, (maxWidth - font.width(".")).coerceAtLeast(0)) + "."
    }
}
