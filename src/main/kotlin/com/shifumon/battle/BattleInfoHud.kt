package com.shifumon.battle

import com.shifumon.config.ConfigManager
import com.shifumon.config.HudAnchor
import com.shifumon.config.HudPosition
import com.shifumon.hud.HudElement
import com.shifumon.hud.PreviewData
import com.shifumon.hud.panel.Panel
import com.shifumon.hud.panel.panel
import com.shifumon.hud.render.HudIcons
import com.shifumon.hud.render.RetroPalette
import net.minecraft.network.chat.Component

/**
 * Battle HUD: turno, clima, terreno e efeitos de campo. Golpes ficam nos botões do próprio
 * Cobblemon (redesenhados) e os dados do oponente ficam no bloco dele.
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
                        HudIcons.weather(weather)?.let { icon(it) }
                        text(BattleNames.weather(weather))
                    }
                }
            }
            if (config.showTerrain) {
                view.terrain?.let { terrain ->
                    row {
                        HudIcons.terrain(terrain)?.let { icon(it) }
                        text(BattleNames.terrain(terrain))
                    }
                }
            }
            if (config.showFieldEffects) {
                view.fieldEffects.forEach { effect ->
                    row {
                        chip(">", RetroPalette.ACCENT_BATTLE)
                        text(BattleNames.field(effect), RetroPalette.TEXT_DIM)
                    }
                }
            }
        }
    }
}
