package com.shifumon.battle

import com.cobblemon.mod.common.api.types.ElementalTypes
import com.shifumon.config.InterfaceConfig
import com.shifumon.hud.panel.PanelBuilder
import com.shifumon.hud.render.HudIcons
import com.shifumon.hud.render.RetroPalette
import com.shifumon.hud.render.StatusStyle
import com.shifumon.hud.render.TinyFont
import com.shifumon.hud.render.TypeColors
import com.shifumon.util.StatNames
import com.shifumon.util.TypeNames
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component

/** Linhas dos painéis de Pokémon em batalha, compartilhadas pelo bloco substituto e pelo painel flutuante. */
internal object BattlePokemonRows {
    private const val CHIPS_PER_ROW = 5
    private val matchupLabels = listOf(
        "shifumon.matchup.weak4", "shifumon.matchup.weak", "shifumon.matchup.resist",
        "shifumon.matchup.immune", "shifumon.matchup.strong",
    )

    fun nameRow(panel: PanelBuilder, view: BattlePokemonView, config: InterfaceConfig, statusInline: Boolean) {
        panel.row {
            if (view.shiny) shiny()
            text(view.name)
            HudIcons.gender(view.gender)?.let { icon(it) }
            right {
                if (statusInline && config.highlightStatus) view.status?.let { chip(StatusStyle.label(it), StatusStyle.color(it)) }
                text(Component.translatable("shifumon.info.level", view.level), RetroPalette.LABEL)
            }
        }
    }

    fun typeRow(panel: PanelBuilder, view: BattlePokemonView, config: InterfaceConfig) {
        panel.row(gap = 2) {
            if (config.showTypes) view.types.forEach { badge(it.displayName, TypeColors.of(it)) }
            right {
                if (config.highlightStatus) view.status?.let { chip(StatusStyle.label(it), StatusStyle.color(it)) }
            }
        }
    }

    fun hpRow(panel: PanelBuilder, view: BattlePokemonView, config: InterfaceConfig, barWidth: Int) {
        if (!config.enhancedHpBar) return
        panel.row {
            hpBar(barWidth, view.hpRatio)
            if (config.showHpNumbers) right { tiny(view.hpText) }
        }
    }

    /** Largura reservada para o texto de HP à direita da barra (0 se desativado). */
    fun hpTextSpace(view: BattlePokemonView, config: InterfaceConfig): Int =
        if (config.enhancedHpBar && config.showHpNumbers) TinyFont.width(TinyFont.sanitize(view.hpText)) + 1 + 8 else 0

    fun detailRows(panel: PanelBuilder, view: BattlePokemonView, config: InterfaceConfig) {
        if (config.showStatBoosts) {
            view.boosts.chunked(4).forEach { chunk ->
                panel.row(gap = 2) {
                    chunk.forEach { (stat, stages) ->
                        val sign = if (stages > 0) "+" else ""
                        chip("${StatNames.short(stat)}$sign$stages", if (stages > 0) RetroPalette.BOOST_UP else RetroPalette.BOOST_DOWN)
                    }
                }
            }
        }

        val matchups = TypeMatchups.of(view.types)
        val labelWidth = matchupLabels.maxOf { TinyFont.width(label(it)) } + 1
        if (config.showWeaknesses) {
            typeChipRows(panel, "shifumon.matchup.weak4", matchups.weak4, labelWidth)
            typeChipRows(panel, "shifumon.matchup.weak", matchups.weak2, labelWidth)
        }
        if (config.showResistances) {
            typeChipRows(panel, "shifumon.matchup.resist", matchups.resist, labelWidth)
            typeChipRows(panel, "shifumon.matchup.immune", matchups.immune, labelWidth)
        }
        if (config.showStrengths) typeChipRows(panel, "shifumon.matchup.strong", matchups.strongAgainst, labelWidth)

        if (config.showCurrentStats) view.stats?.let { statRows(panel, it) }
        view.competitive?.let { competitiveRows(panel, it) }
    }

    /** Atributos atuais: verde quando aumentado, vermelho quando reduzido; faixa no oponente. */
    private fun statRows(panel: PanelBuilder, stats: StatsView) {
        panel.separator()
        panel.row {
            tiny(label("shifumon.battle.stats"), RetroPalette.LABEL)
            if (stats.estimated) right { tiny(label("shifumon.battle.stats.estimated"), RetroPalette.TEXT_DIM) }
        }
        stats.values.chunked(3).forEach { chunk ->
            panel.row(gap = 2) {
                chunk.forEachIndexed { index, value ->
                    if (index > 0) space(3)
                    tiny(StatNames.short(value.stat), RetroPalette.TEXT_DIM)
                    val text = if (value.min == value.max) "${value.min}" else "${value.min}-${value.max}"
                    tiny(text, trendColor(value.trend))
                }
            }
        }
    }

    private fun trendColor(trend: Int): Int = when {
        trend > 0 -> RetroPalette.POSITIVE
        trend < 0 -> RetroPalette.NEGATIVE
        else -> RetroPalette.TEXT
    }

    /** "FRACO [LUT][PED]..." com os chips alinhados numa coluna, quebrando a cada 5 tipos. */
    private fun typeChipRows(panel: PanelBuilder, labelKey: String, typeIds: List<String>, labelWidth: Int) {
        if (typeIds.isEmpty()) return
        val label = label(labelKey)
        typeIds.chunked(CHIPS_PER_ROW).forEachIndexed { index, chunk ->
            panel.row(gap = 2) {
                if (index == 0) {
                    tiny(label, RetroPalette.LABEL)
                    space(labelWidth - TinyFont.width(label) - 1)
                } else {
                    space(labelWidth + 2)
                }
                chunk.forEach { id -> typeChip(id, TypeNames.short(id), TypeColors.of(ElementalTypes.get(id))) }
            }
        }
    }

    private fun competitiveRows(panel: PanelBuilder, competitive: CompetitiveView) {
        panel.separator()
        panel.row {
            tiny(label("shifumon.battle.base_stats"), RetroPalette.LABEL)
            right { tiny("BST ${competitive.baseStats.sumOf { it.second }}", RetroPalette.TEXT_DIM) }
        }
        competitive.baseStats.chunked(3).forEach { chunk ->
            panel.row(gap = 2) {
                chunk.forEachIndexed { index, (stat, value) ->
                    if (index > 0) space(3)
                    tiny(StatNames.short(stat), RetroPalette.TEXT_DIM)
                    tiny(value.toString(), baseStatColor(value))
                }
            }
        }
        competitive.abilities.sortedBy { it.second }.forEach { (name, hidden) ->
            panel.row {
                text(name, RetroPalette.TEXT_DIM)
                if (hidden) chip("HA", RetroPalette.SHINY_DARK)
            }
        }
    }

    private fun label(key: String): String = TinyFont.sanitize(I18n.get(key))

    private fun baseStatColor(value: Int): Int = when {
        value >= 120 -> RetroPalette.POSITIVE
        value >= 80 -> RetroPalette.TEXT
        value >= 60 -> RetroPalette.WARNING
        else -> RetroPalette.NEGATIVE
    }
}
