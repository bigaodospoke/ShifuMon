package com.shifumon.pokemoninfo

import com.shifumon.capture.CaptureEstimate
import com.shifumon.capture.CaptureEstimator
import com.shifumon.config.ConfigManager
import com.shifumon.config.HudAnchor
import com.shifumon.config.HudPosition
import com.shifumon.hud.HudElement
import com.shifumon.hud.PreviewData
import com.shifumon.hud.panel.Panel
import com.shifumon.hud.panel.panel
import com.shifumon.hud.render.HudIcons
import com.shifumon.hud.render.RetroPalette
import com.shifumon.hud.render.TinyFont
import com.shifumon.hud.render.TypeColors
import com.shifumon.util.StatNames
import com.shifumon.util.TextUtil
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

/** Painel com os detalhes do Pokémon na mira. */
object PokemonInfoHud : HudElement {
    override val id = "pokemon_info"
    override val displayName: Component = Component.translatable("shifumon.hud.pokemon_info")
    override val defaultPosition = HudPosition(HudAnchor.CENTER_RIGHT, -8, 0)

    override fun isEnabled(): Boolean = ConfigManager.config.pokemonInfo.enabled

    override fun build(preview: Boolean): Panel? {
        val config = ConfigManager.config.pokemonInfo
        if (preview) return layout(PreviewData.pokemonInfo(), PreviewData.captureEstimate())

        val entity = PokemonLookTracker.target ?: return null
        val capture = if (config.showCaptureChance) CaptureEstimator.estimate(entity) else null
        return layout(PokemonInfoResolver.resolve(entity), capture)
    }

    private fun layout(info: PokemonInfo, capture: CaptureEstimate?): Panel? {
        val config = ConfigManager.config.pokemonInfo
        val unknown = Component.translatable("shifumon.info.unknown")
        val accent = if (info.shiny) RetroPalette.SHINY else RetroPalette.ACCENT_INFO

        return panel(accent, minWidth = 100) {
            row {
                if (config.showShiny && info.shiny) shiny()
                text(info.displayName)
                if (config.showGender) HudIcons.gender(info.gender)?.let { icon(it) }
                if (config.showLevel) right {
                    text(Component.translatable("shifumon.info.level", info.level ?: "?"), RetroPalette.LABEL)
                }
            }
            info.speciesName?.let { species -> row { text(species, RetroPalette.TEXT_DIM) } }
            if (config.showTypes && info.types.isNotEmpty()) {
                row(gap = 2) { info.types.forEach { badge(it.displayName, TypeColors.of(it)) } }
            }

            separator()

            if (config.showNature) {
                val nature = info.nature
                if (nature == null) {
                    labeled(label("nature"), unknown, RetroPalette.UNKNOWN)
                } else {
                    labeled(label("nature"), nature.name) {
                        val raised = nature.raised
                        val lowered = nature.lowered
                        if (raised != null && lowered != null && raised != lowered) {
                            chip("+" + StatNames.short(raised), RetroPalette.BOOST_UP)
                            chip("-" + StatNames.short(lowered), RetroPalette.BOOST_DOWN)
                        }
                    }
                }
            }
            if (config.showAbility) {
                val ability = info.ability
                if (ability == null) {
                    labeled(label("ability"), unknown, RetroPalette.UNKNOWN)
                } else {
                    labeled(label("ability"), ability) { if (info.abilityIsHidden) chip("HA", RetroPalette.SHINY_DARK) }
                }
            }
            if (config.showHiddenAbility) info.hiddenAbility?.let { labeled(label("hidden_ability"), it) }
            if (config.showSize) {
                labeled(
                    label("size"),
                    Component.translatable("shifumon.info.size.value", TextUtil.decimal(info.scale, 2), TextUtil.decimal(info.heightMeters, 2)),
                )
            }
            if (config.showWeight) {
                labeled(label("weight"), Component.translatable("shifumon.info.weight.value", TextUtil.decimal(info.weightKg, 1)))
            }
            if (config.showForm) labeled(label("form"), Component.literal(info.form))

            if (!info.owned && (config.showNature || config.showAbility)) {
                row { text(Component.translatable("shifumon.info.hidden_hint"), RetroPalette.UNKNOWN) }
            }

            capture?.let { estimate ->
                separator()
                labeled(
                    label("capture"),
                    Component.translatable("shifumon.info.capture.value", percent(estimate.worst), percent(estimate.best)),
                    captureColor(estimate.worst),
                ) {
                    right { text(estimate.ballName, RetroPalette.TEXT_DIM) }
                }
                row { tiny(TinyFont.sanitize(I18n.get("shifumon.info.capture.hint")), RetroPalette.UNKNOWN) }
            }
        }
    }

    private fun percent(chance: Double): String {
        val value = chance * 100
        return if (value > 0.0 && value < 1.0) "<1" else value.roundToInt().toString()
    }

    private fun captureColor(worst: Double): Int = when {
        worst >= 0.5 -> RetroPalette.POSITIVE
        worst >= 0.15 -> RetroPalette.WARNING
        else -> RetroPalette.NEGATIVE
    }

    private fun label(key: String): Component = Component.translatable("shifumon.info.$key")
}
