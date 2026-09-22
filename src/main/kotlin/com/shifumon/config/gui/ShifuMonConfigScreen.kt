package com.shifumon.config.gui

import com.shifumon.config.ConfigManager
import com.shifumon.hud.HudManager
import com.shifumon.hud.render.PixelUi
import com.shifumon.hud.render.RetroPalette
import com.shifumon.shiny.ShinyIcons
import com.shifumon.shiny.ShinyStyle
import net.minecraft.Util
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import java.nio.file.Files
import kotlin.math.abs

/** Tela de configuração do ShifuMon (Mod Menu ou tecla O). Edita a config ao vivo e salva ao fechar. */
class ShifuMonConfigScreen(private val parent: Screen?) : Screen(Component.translatable("shifumon.config.title")) {

    private enum class Page(val key: String) {
        SHINY("shiny"), POKEMON_INFO("pokemon_info"), INTERFACE("interface"), BATTLE("battle"), BOX_SEARCH("box_search"), HUD("hud")
    }

    /** Cria o widget de uma opção na posição calculada pela grade. */
    private fun interface OptionWidget {
        fun create(x: Int, y: Int, width: Int): AbstractWidget
    }

    private val config get() = ConfigManager.config
    private var contentBottom = 0

    override fun init() {
        addTabs()
        contentBottom = addOptionGrid(options(lastPage), TABS_Y + 28)
        addFooter()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        renderTitle(graphics)
        when (lastPage) {
            Page.SHINY -> renderShinyPreview(graphics)
            Page.BOX_SEARCH -> renderLines(graphics, BOX_SEARCH_HELP)
            Page.HUD -> renderLines(graphics, HUD_HELP)
            else -> Unit
        }
    }

    override fun onClose() {
        ConfigManager.save()
        minecraft?.setScreen(parent)
    }

    // ------------------------------------------------------------------ Layout

    private fun addTabs() {
        val pages = Page.entries
        val tabWidth = ((width - 20) / pages.size - TAB_GAP).coerceIn(40, 72)
        var x = (width - (pages.size * (tabWidth + TAB_GAP) - TAB_GAP)) / 2
        for (page in pages) {
            val button = Button.builder(Component.translatable("shifumon.config.page.${page.key}")) {
                lastPage = page
                rebuildWidgets()
            }.bounds(x, TABS_Y, tabWidth, 20).build()
            button.active = page != lastPage
            addRenderableWidget(button)
            x += tabWidth + TAB_GAP
        }
    }

    /** Grade de 2 colunas (1 em telas estreitas). Retorna o Y logo abaixo da última linha. */
    private fun addOptionGrid(options: List<OptionWidget>, top: Int): Int {
        val columns = if (width >= 2 * OPTION_WIDTH + OPTION_GAP + 20) 2 else 1
        val left = (width - (columns * OPTION_WIDTH + (columns - 1) * OPTION_GAP)) / 2
        options.forEachIndexed { index, option ->
            val x = left + (index % columns) * (OPTION_WIDTH + OPTION_GAP)
            val y = top + (index / columns) * ROW_STEP
            addRenderableWidget(option.create(x, y, OPTION_WIDTH))
        }
        return top + (options.size + columns - 1) / columns * ROW_STEP
    }

    private fun addFooter() {
        val y = height - 26
        addRenderableWidget(Button.builder(Component.translatable("shifumon.config.reset")) { confirmReset() }.bounds(width / 2 - 154, y, 100, 20).build())
        addRenderableWidget(
            Button.builder(Component.translatable("shifumon.config.open_hud_editor")) { minecraft?.setScreen(HudEditorScreen(this)) }
                .bounds(width / 2 - 50, y, 100, 20).build(),
        )
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE) { onClose() }.bounds(width / 2 + 54, y, 100, 20).build())
    }

    // ------------------------------------------------------------------ Opções por página

    private fun options(page: Page): List<OptionWidget> = when (page) {
        Page.SHINY -> {
            val shiny = config.shiny
            listOf(
                toggle("shifumon.config.shiny.enabled", shiny.enabled) { shiny.enabled = it },
                OptionWidget { x, y, width ->
                    CycleButton.builder<ShinyStyle> { Component.translatable(it.translationKey) }
                        .withValues(ShinyStyle.entries)
                        .withInitialValue(shiny.style)
                        .create(x, y, width, 20, Component.translatable("shifumon.config.shiny.style")) { _, style ->
                            shiny.style = style
                            if (style == ShinyStyle.CUSTOM) ShinyIcons.reloadCustomIcon()
                        }
                },
                action("shifumon.config.shiny.reload_custom") { ShinyIcons.reloadCustomIcon() },
                action("shifumon.config.open_folder") { openConfigFolder() },
            )
        }

        Page.POKEMON_INFO -> {
            val info = config.pokemonInfo
            listOf(
                toggle("shifumon.config.pokemon_info.enabled", info.enabled) { info.enabled = it },
                toggle("shifumon.config.pokemon_info.require_sneak", info.requireSneak) { info.requireSneak = it },
                OptionWidget { x, y, width ->
                    CycleButton.builder<Int> { Component.translatable("shifumon.config.blocks", it) }
                        .withValues(DISTANCES)
                        .withInitialValue(DISTANCES.minBy { abs(it - info.maxDistance) })
                        .create(x, y, width, 20, Component.translatable("shifumon.config.pokemon_info.max_distance")) { _, value ->
                            info.maxDistance = value
                        }
                },
                toggle("shifumon.config.pokemon_info.show_level", info.showLevel) { info.showLevel = it },
                toggle("shifumon.config.pokemon_info.show_shiny", info.showShiny) { info.showShiny = it },
                toggle("shifumon.config.pokemon_info.show_gender", info.showGender) { info.showGender = it },
                toggle("shifumon.config.pokemon_info.show_types", info.showTypes) { info.showTypes = it },
                toggle("shifumon.config.pokemon_info.show_nature", info.showNature) { info.showNature = it },
                toggle("shifumon.config.pokemon_info.show_ability", info.showAbility) { info.showAbility = it },
                toggle("shifumon.config.pokemon_info.show_hidden_ability", info.showHiddenAbility) { info.showHiddenAbility = it },
                toggle("shifumon.config.pokemon_info.show_size", info.showSize) { info.showSize = it },
                toggle("shifumon.config.pokemon_info.show_weight", info.showWeight) { info.showWeight = it },
                toggle("shifumon.config.pokemon_info.show_form", info.showForm) { info.showForm = it },
                toggle("shifumon.config.pokemon_info.show_capture_chance", info.showCaptureChance) { info.showCaptureChance = it },
            )
        }

        Page.INTERFACE -> {
            val ui = config.interfaceTweaks
            listOf(
                toggle("shifumon.config.interface.enabled", ui.enabled) { ui.enabled = it },
                toggle("shifumon.config.interface.enhanced_hp_bar", ui.enhancedHpBar) { ui.enhancedHpBar = it },
                toggle("shifumon.config.interface.show_hp_numbers", ui.showHpNumbers) { ui.showHpNumbers = it },
                toggle("shifumon.config.interface.show_types", ui.showTypes) { ui.showTypes = it },
                toggle("shifumon.config.interface.highlight_status", ui.highlightStatus) { ui.highlightStatus = it },
                toggle("shifumon.config.interface.show_stat_boosts", ui.showStatBoosts) { ui.showStatBoosts = it },
                toggle("shifumon.config.interface.replace_battle_tiles", ui.replaceBattleTiles) { ui.replaceBattleTiles = it },
                toggle("shifumon.config.interface.respect_texture_packs", ui.respectTexturePacks) { ui.respectTexturePacks = it },
                toggle("shifumon.config.interface.show_weaknesses", ui.showWeaknesses) { ui.showWeaknesses = it },
                toggle("shifumon.config.interface.show_resistances", ui.showResistances) { ui.showResistances = it },
                toggle("shifumon.config.interface.show_strengths", ui.showStrengths) { ui.showStrengths = it },
                toggle("shifumon.config.interface.show_current_stats", ui.showCurrentStats) { ui.showCurrentStats = it },
            )
        }

        Page.BATTLE -> {
            val battle = config.battleHud
            listOf(
                toggle("shifumon.config.battle.enabled", battle.enabled) { battle.enabled = it },
                toggle("shifumon.config.battle.show_turn_counter", battle.showTurnCounter) { battle.showTurnCounter = it },
                toggle("shifumon.config.battle.show_weather", battle.showWeather) { battle.showWeather = it },
                toggle("shifumon.config.battle.show_terrain", battle.showTerrain) { battle.showTerrain = it },
                toggle("shifumon.config.battle.show_field_effects", battle.showFieldEffects) { battle.showFieldEffects = it },
                toggle("shifumon.config.battle.show_side_conditions", battle.showSideConditions) { battle.showSideConditions = it },
                toggle("shifumon.config.battle.show_effect_turns", battle.showEffectTurns) { battle.showEffectTurns = it },
                toggle("shifumon.config.battle.show_competitive_info", battle.showCompetitiveInfo) { battle.showCompetitiveInfo = it },
                toggle("shifumon.config.battle.restyle_move_buttons", battle.restyleMoveButtons) { battle.restyleMoveButtons = it },
                toggle("shifumon.config.battle.show_move_effectiveness", battle.showMoveEffectiveness) { battle.showMoveEffectiveness = it },
                toggle("shifumon.config.battle.show_move_tooltip", battle.showMoveTooltip) { battle.showMoveTooltip = it },
                toggle("shifumon.config.battle.restyle_battle_log", battle.restyleBattleLog) { battle.restyleBattleLog = it },
            )
        }

        Page.BOX_SEARCH -> {
            val search = config.boxSearch
            val pc = config.pc
            listOf(
                toggle("shifumon.config.box_search.enabled", search.enabled) { search.enabled = it },
                toggle("shifumon.config.box_search.show_results_panel", search.showResultsPanel) { search.showResultsPanel = it },
                toggle("shifumon.config.pc.show_shiny_icon", pc.showShinyIcon) { pc.showShinyIcon = it },
                toggle("shifumon.config.pc.show_iv_badge", pc.showIvBadge) { pc.showIvBadge = it },
                toggle("shifumon.config.pc.shifumon_wallpaper", pc.shifumonWallpaper) { pc.shifumonWallpaper = it },
                OptionWidget { x, y, width ->
                    CycleButton.builder<Int> { Component.literal("F$it+") }
                        .withValues(listOf(1, 2, 3, 4, 5, 6))
                        .withInitialValue(pc.minPerfectIvs)
                        .create(x, y, width, 20, Component.translatable("shifumon.config.pc.min_perfect_ivs")) { _, value ->
                            pc.minPerfectIvs = value
                        }
                },
            )
        }

        Page.HUD -> listOf(
            action("shifumon.config.open_hud_editor") { minecraft?.setScreen(HudEditorScreen(this)) },
            action("shifumon.config.hud.reset_positions") { HudManager.resetAllPositions() },
        )
    }

    private fun toggle(key: String, initial: Boolean, onChange: (Boolean) -> Unit) = OptionWidget { x, y, width ->
        CycleButton.onOffBuilder(initial).create(x, y, width, 20, Component.translatable(key)) { _, value -> onChange(value) }
    }

    private fun action(key: String, onPress: () -> Unit) = OptionWidget { x, y, width ->
        Button.builder(Component.translatable(key)) { onPress() }.bounds(x, y, width, 20).build()
    }

    // ------------------------------------------------------------------ Desenho

    private fun renderTitle(graphics: GuiGraphics) {
        val titleWidth = font.width(title)
        val x = (width - titleWidth) / 2
        ShinyIcons.draw(graphics, null, x - 20, 8, 16)
        graphics.drawString(font, title, x, 12, RetroPalette.TEXT, true)
        ShinyIcons.draw(graphics, null, x + titleWidth + 4, 8, 16)
    }

    /** Todos os estilos lado a lado em 32x32 (escala 2x exata do sprite 16x16). */
    private fun renderShinyPreview(graphics: GuiGraphics) {
        val styles = ShinyStyle.entries
        val cell = 40
        val panelWidth = styles.size * cell + 12
        val panelHeight = 72
        val panelX = (width - panelWidth) / 2
        val panelY = contentBottom + 6
        if (panelY + panelHeight > height - 30) return

        PixelUi.panel(graphics, panelX, panelY, panelWidth, panelHeight, RetroPalette.SHINY)
        val active = ShinyIcons.activeStyle()
        styles.forEachIndexed { index, style ->
            val iconX = panelX + 6 + index * cell + (cell - 32) / 2
            val iconY = panelY + 10
            if (style == active) PixelUi.box(graphics, iconX - 3, iconY - 3, 38, 38, 0xFF4A4A78.toInt())
            ShinyIcons.draw(graphics, style, iconX, iconY, 32)
        }
        graphics.drawCenteredString(font, Component.translatable(active.translationKey), width / 2, panelY + 48, RetroPalette.SHINY)

        val status = if (active == ShinyStyle.CUSTOM) {
            ShinyIcons.customIconStatus
        } else {
            Component.translatable("shifumon.config.shiny.custom_hint", ShinyIcons.customIconPath().fileName.toString())
        }
        status?.let { graphics.drawCenteredString(font, it, width / 2, panelY + 59, RetroPalette.TEXT_DIM) }
    }

    private fun renderLines(graphics: GuiGraphics, keys: List<String>) {
        keys.forEachIndexed { index, key ->
            val color = if (index == 0) RetroPalette.LABEL else RetroPalette.TEXT_DIM
            graphics.drawCenteredString(font, Component.translatable(key), width / 2, contentBottom + 10 + index * 12, color)
        }
    }

    // ------------------------------------------------------------------ Ações

    private fun confirmReset() {
        minecraft?.setScreen(
            ConfirmScreen(
                { confirmed ->
                    if (confirmed) {
                        ConfigManager.resetToDefaults()
                        ShinyIcons.reloadCustomIcon()
                    }
                    minecraft?.setScreen(this)
                },
                Component.translatable("shifumon.config.reset.confirm.title"),
                Component.translatable("shifumon.config.reset.confirm.message"),
            ),
        )
    }

    private fun openConfigFolder() {
        val directory = ConfigManager.configDir
        runCatching { Files.createDirectories(directory) }
        Util.getPlatform().openFile(directory.toFile())
    }

    companion object {
        private const val TABS_Y = 30
        private const val TAB_GAP = 2
        private const val OPTION_WIDTH = 150
        private const val OPTION_GAP = 8
        private const val ROW_STEP = 22

        private val DISTANCES = listOf(6, 8, 12, 16, 24, 32, 48)
        private val BOX_SEARCH_HELP = listOf(
            "shifumon.search.help.title", "shifumon.search.help.1", "shifumon.search.help.2", "shifumon.search.help.3",
        )
        private val HUD_HELP = listOf("shifumon.config.hud.help.1", "shifumon.config.hud.help.2")

        /** Lembra a última aba aberta enquanto o jogo estiver aberto. */
        private var lastPage = Page.SHINY
    }
}
