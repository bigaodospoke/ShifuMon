package com.shifumon.battle

import com.shifumon.config.ConfigManager
import com.shifumon.config.HudAnchor
import com.shifumon.config.HudPosition
import com.shifumon.hud.HudElement
import com.shifumon.hud.PreviewData
import com.shifumon.hud.panel.Panel
import com.shifumon.hud.panel.PanelBuilder
import com.shifumon.hud.panel.RowBuilder
import com.shifumon.hud.panel.panel
import com.shifumon.hud.render.HudIcons
import com.shifumon.hud.render.RetroPalette
import com.shifumon.hud.render.TinyFont
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component

/**
 * Battle HUD: turno, clima, terreno, efeitos de campo e condições de cada lado, com os turnos que
 * faltam para cada um acabar. Golpes ficam nos botões do próprio Cobblemon (redesenhados) e os
 * dados do oponente ficam no bloco dele.
 */
object BattleInfoHud : HudElement {
    override val id = "battle_info"
    override val displayName: Component = Component.translatable("shifumon.hud.battle_info")
    // Entre os blocos de batalha do Cobblemon (cantos superiores) e longe das opções (canto inferior esquerdo)
    override val defaultPosition = HudPosition(HudAnchor.TOP_CENTER, 0, 4)

    override fun isEnabled(): Boolean = ConfigManager.config.battleHud.enabled

    override fun build(preview: Boolean): Panel? {
        val view = if (preview) PreviewData.battleInfo() else BattleReader.battle()?.let(BattleReader::infoView) ?: return null
        return layout(view)
    }

    private fun layout(view: BattleInfoView): Panel? {
        val config = ConfigManager.config.battleHud
        return panel(RetroPalette.ACCENT_BATTLE, minWidth = 80) {
            if (config.showTurnCounter) {
                row {
                    icon(HudIcons.TURN)
                    text(Component.translatable("shifumon.battle.turn", if (view.turn > 0) view.turn else "-"), RetroPalette.LABEL)
                }
            }
            if (config.showWeather) {
                view.weather?.let { weather ->
                    row {
                        HudIcons.weather(weather.id)?.let { icon(it) }
                        text(BattleNames.weather(weather.id))
                        turnsLeft(weather)
                    }
                }
            }
            if (config.showTerrain) {
                view.terrain?.let { terrain ->
                    row {
                        HudIcons.terrain(terrain.id)?.let { icon(it) }
                        text(BattleNames.terrain(terrain.id))
                        turnsLeft(terrain)
                    }
                }
            }
            if (config.showFieldEffects) {
                view.fieldEffects.forEach { effect ->
                    row {
                        chip(">", RetroPalette.ACCENT_BATTLE)
                        text(BattleNames.field(effect.id), RetroPalette.TEXT_DIM)
                        turnsLeft(effect)
                    }
                }
            }
            if (config.showSideConditions) {
                sideRows("shifumon.battle.side.ally", RetroPalette.ACCENT_ALLY, view.allySide)
                sideRows("shifumon.battle.side.opponent", RetroPalette.ACCENT_OPPONENT, view.opponentSide)
            }
        }
    }

    /** "SEU LADO" / "OPONENTE" seguidos de Reflect, Tailwind, Spikes x2... */
    private fun PanelBuilder.sideRows(labelKey: String, color: Int, effects: List<FieldEffectView>) {
        if (effects.isEmpty()) return
        separator()
        row { tiny(TinyFont.sanitize(I18n.get(labelKey)), color) }
        effects.forEach { effect ->
            row {
                chip(">", color)
                text(BattleNames.side(effect.id), RetroPalette.TEXT_DIM)
                if (effect.layers > 1) tiny("X${effect.layers}")
                turnsLeft(effect)
            }
        }
    }

    /** "3 TURNOS" à direita; faixa ("3-6 TURNOS") quando um item do oponente pode estender o efeito. */
    private fun RowBuilder.turnsLeft(effect: FieldEffectView) {
        if (!ConfigManager.config.battleHud.showEffectTurns) return
        val range = effect.remaining ?: return
        val count = if (range.first == range.last) "${range.first}" else "${range.first}-${range.last}"
        val key = if (range.last == 1) "shifumon.battle.turns_left.one" else "shifumon.battle.turns_left"
        right { tiny(TinyFont.sanitize(I18n.get(key, count)), RetroPalette.TEXT_DIM) }
    }
}
