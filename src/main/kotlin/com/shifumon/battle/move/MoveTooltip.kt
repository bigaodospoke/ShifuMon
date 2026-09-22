package com.shifumon.battle.move

import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.categories.DamageCategories
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection
import com.shifumon.battle.Effectiveness
import com.shifumon.config.ConfigManager
import com.shifumon.hud.TooltipLayer
import com.shifumon.hud.panel.Panel
import com.shifumon.hud.panel.RowBuilder
import com.shifumon.hud.panel.panel
import com.shifumon.hud.render.CategoryStyle
import com.shifumon.hud.render.RetroPalette
import com.shifumon.hud.render.TinyFont
import com.shifumon.hud.render.TypeColors
import com.shifumon.util.FeatureGuard
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.locale.Language
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.TranslatableContents

/**
 * Dica ao passar o mouse num golpe: poder, precisão, PP, prioridade, chance de crítico e de efeito,
 * STAB, eficácia contra cada alvo e a descrição. Aparece com ou sem os botões redesenhados.
 */
object MoveTooltip {
    private const val MIN_WIDTH = 120
    private const val DESCRIPTION_WIDTH = 150
    private const val EFFECT_CHIP = 0xFF5A5A70.toInt()
    private const val STAB_CHIP = 0xFF7A4FB0.toInt()

    /** Chamado pelo mixin a cada quadro em que o botão é desenhado. */
    @JvmStatic
    fun onRender(tile: BattleMoveSelection.MoveTile, mouseX: Int, mouseY: Int) {
        if (!ConfigManager.config.battleHud.showMoveTooltip) return
        if (!tile.isHovered(mouseX.toDouble(), mouseY.toDouble())) return
        FeatureGuard.guard("battle.move_tooltip", Unit) {
            build(tile)?.let { TooltipLayer.show(it, mouseX, mouseY) }
        }
    }

    private fun build(tile: BattleMoveSelection.MoveTile): Panel? {
        val template = tile.moveTemplate
        val move = tile.move
        val type = tile.elementalType
        val category = template.damageCategory
        val damaging = category != DamageCategories.STATUS

        return panel(TypeColors.of(type), minWidth = MIN_WIDTH) {
            row {
                text(template.displayName)
                right { badge(type.displayName, TypeColors.of(type)) }
            }
            row(gap = 2) {
                chip(CategoryStyle.label(category), CategoryStyle.color(category))
                if (move.disabled) chip(label("shifumon.move.disabled"), RetroPalette.BOOST_DOWN)
            }
            separator()
            row {
                value("shifumon.move.power", if (damaging && template.power > 0) template.power.toInt().toString() else "--")
                space(3)
                value("shifumon.move.accuracy", accuracy(template))
                space(3)
                tiny("PP", RetroPalette.LABEL)
                tiny(MoveButtonRenderer.ppText(move), MoveButtonRenderer.ppColor(move))
            }
            row(gap = 2) {
                if (template.priority != 0) {
                    val sign = if (template.priority > 0) "+" else ""
                    chip("${label("shifumon.move.priority")} $sign${template.priority}", if (template.priority > 0) RetroPalette.BOOST_UP else RetroPalette.BOOST_DOWN)
                }
                if (template.critRatio > 1.0) chip(label("shifumon.move.high_crit"), RetroPalette.SHINY_DARK)
                effectChance(template)?.let { chip(label("shifumon.move.effect_chance", it), EFFECT_CHIP) }
                if (damaging && hasStab(tile)) chip(label("shifumon.move.stab"), STAB_CHIP)
            }

            if (damaging) {
                MoveButtonRenderer.opponentTargets(tile).forEach { target ->
                    val multiplier = MoveButtonRenderer.multiplier(tile, target)
                    row {
                        text(target.displayName, RetroPalette.TEXT_DIM)
                        right { chip(Effectiveness.label(multiplier), Effectiveness.color(multiplier)) }
                    }
                }
            }

            description(template)?.let { lines ->
                separator()
                lines.forEach { line -> row { text(line, RetroPalette.TEXT_DIM) } }
            }
        }
    }

    private fun RowBuilder.value(labelKey: String, value: String) {
        tiny(label(labelKey), RetroPalette.LABEL)
        tiny(value)
    }

    /** Golpes que nunca erram chegam com precisão -1 (ou 0). */
    private fun accuracy(template: MoveTemplate): String =
        if (template.accuracy <= 0.0 || template.accuracy > 100.0) "--" else "${template.accuracy.toInt()}%"

    /** Chance de efeito secundário (queimar, paralisar...); 100% é garantido e não precisa aparecer. */
    private fun effectChance(template: MoveTemplate): Int? =
        template.effectChances.firstOrNull()?.toInt()?.takeIf { it in 1..99 }

    /** Mesmo tipo do Pokémon que usa: dano x1,5. */
    private fun hasStab(tile: BattleMoveSelection.MoveTile): Boolean =
        runCatching { tile.pokemon?.types?.any { it == tile.elementalType } ?: false }.getOrDefault(false)

    private fun description(template: MoveTemplate): List<String>? {
        val description = template.description
        val key = (description.contents as? TranslatableContents)?.key
        if (key != null && !Language.getInstance().has(key)) return null
        val font = Minecraft.getInstance().font
        return font.splitter.splitLines(description, DESCRIPTION_WIDTH, Style.EMPTY)
            .map { it.string }
            .filter { it.isNotBlank() }
            .ifEmpty { null }
    }

    private fun label(key: String, vararg args: Any): String = TinyFont.sanitize(I18n.get(key, *args))
}
