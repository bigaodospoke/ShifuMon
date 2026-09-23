package com.shifumon.battle

import com.shifumon.battle.tile.BattleTileRenderer
import com.shifumon.config.ConfigManager
import com.shifumon.config.HudAnchor
import com.shifumon.config.HudPosition
import com.shifumon.hud.HudElement
import com.shifumon.hud.PreviewData
import com.shifumon.hud.panel.Panel
import com.shifumon.hud.panel.panel
import com.shifumon.hud.render.RetroPalette
import net.minecraft.network.chat.Component

/**
 * Painel flutuante e arrastável dos Pokémon ativos de um lado. Só aparece com a substituição do
 * bloco do Cobblemon desligada; senão o mesmo conteúdo é desenhado no lugar do bloco.
 */
class BattlePokemonHud private constructor(private val allySide: Boolean) : HudElement {
    override val id: String = if (allySide) "battle_ally" else "battle_opponent"
    override val displayName: Component = Component.translatable("shifumon.hud.$id")
    // Logo abaixo do bloco de batalha do Cobblemon do mesmo lado (inset 10 + altura 40)
    override val defaultPosition: HudPosition =
        if (allySide) HudPosition(HudAnchor.TOP_LEFT, 12, 56) else HudPosition(HudAnchor.TOP_RIGHT, -12, 56)

    // Aparece sempre que o bloco do Cobblemon não está sendo substituído (opção desligada ou pacote de textura)
    override fun isEnabled(): Boolean = ConfigManager.config.interfaceTweaks.enabled && !BattleTileRenderer.isReplacing()

    override fun build(preview: Boolean): Panel? {
        val pokemon = if (preview) {
            PreviewData.battlePokemon(allySide)
        } else {
            val battle = BattleReader.battle() ?: return null
            val side = if (allySide) BattleReader.playerSide(battle) else BattleReader.opponentSide(battle)
            val withCompetitive = !allySide && ConfigManager.config.battleHud.showCompetitiveInfo
            BattleReader.active(side).map { BattleReader.pokemonView(it, withCompetitive) }
        }
        if (pokemon.isEmpty()) return null

        val config = ConfigManager.config.interfaceTweaks
        val accent = if (allySide) RetroPalette.ACCENT_ALLY else RetroPalette.ACCENT_OPPONENT
        return panel(accent, minWidth = PANEL_WIDTH) {
            pokemon.forEachIndexed { index, view ->
                if (index > 0) separator()
                BattlePokemonRows.nameRow(this, view, config, statusInline = false)
                BattlePokemonRows.typeRow(this, view, config)
                BattlePokemonRows.hpRow(this, view, config, PANEL_WIDTH - BattlePokemonRows.hpTextSpace(view, config))
                if (!BattleDetails.collapsed()) BattlePokemonRows.detailRows(this, view, config)
            }
        }
    }

    companion object {
        private const val PANEL_WIDTH = 124

        @JvmField
        val ALLY = BattlePokemonHud(allySide = true)

        @JvmField
        val OPPONENT = BattlePokemonHud(allySide = false)
    }
}
