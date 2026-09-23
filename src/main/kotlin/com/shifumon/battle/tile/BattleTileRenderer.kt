package com.shifumon.battle.tile

import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon
import com.cobblemon.mod.common.client.battle.ClientBallDisplay
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.pokemon.Species
import com.shifumon.battle.BattleDetails
import com.shifumon.battle.BattlePokemonRows
import com.shifumon.battle.BattleReader
import com.shifumon.config.ConfigManager
import com.shifumon.hud.TooltipLayer
import com.shifumon.hud.panel.panel
import com.shifumon.hud.render.Alpha
import com.shifumon.hud.render.PixelUi
import com.shifumon.hud.render.RetroPalette
import com.shifumon.hud.render.TinyFont
import com.shifumon.util.Colors
import com.shifumon.util.FeatureGuard
import com.shifumon.util.TexturePackCheck
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth

/**
 * Substitui o bloco de batalha do Cobblemon (nome, nível, HP) pelo painel do ShifuMon.
 *
 * Posição, animação de entrada e seleção de comando continuam vindo do Cobblemon. O retrato
 * animado é desenhado com a mesma função do Cobblemon, dentro de uma moldura pixel art.
 */
object BattleTileRenderer {
    private const val TILE_WIDTH = 140
    private const val COMPACT_TILE_WIDTH = 128
    private const val PORTRAIT_SIZE = 28
    private const val COMPACT_PORTRAIT_SIZE = 19
    private const val MIN_HP_BAR_WIDTH = 60

    private val caughtIndicator = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/battle/battle_owned_indicator.png")

    private var currentPokemon: ActiveClientBattlePokemon? = null

    /** Chamado no início de `drawTile`, que conhece o Pokémon ativo. */
    @JvmStatic
    fun capture(active: ActiveClientBattlePokemon?) {
        currentPokemon = active
    }

    /**
     * O bloco do Cobblemon está sendo substituído? Não, se a opção estiver desligada ou se um pacote
     * de textura (barra de HP personalizada) retexturizar o bloco e o jogador pediu para respeitá-lo.
     */
    fun isReplacing(): Boolean {
        val config = ConfigManager.config.interfaceTweaks
        return config.enabled && config.replaceBattleTiles && !TexturePackCheck.shouldYieldTo(TexturePackCheck.BATTLE_TILE)
    }

    /** Chamado no início de `drawBattleTile`. `true` = bloco desenhado, cancelar o original. */
    @JvmStatic
    fun render(
        overlay: BattleOverlay,
        graphics: GuiGraphics,
        x: Float,
        y: Float,
        partialTicks: Float,
        reversed: Boolean,
        species: Species,
        state: PosableState,
        colour: Triple<Float, Float, Float>?,
        opacity: Float,
        ballState: ClientBallDisplay?,
        isSelected: Boolean,
        isHovered: Boolean,
        isCompact: Boolean,
        actorDisplayName: MutableComponent?,
        dexState: PokedexEntryProgress,
    ): Boolean {
        if (!isReplacing()) return false
        val config = ConfigManager.config.interfaceTweaks
        val battlePokemon = currentPokemon?.battlePokemon ?: return false

        return FeatureGuard.guard("battle.tile", false) {
            // Dados competitivos só no bloco do oponente e fora do modo compacto (duplas/triplas)
            val withCompetitive = reversed && !isCompact && ConfigManager.config.battleHud.showCompetitiveInfo
            val view = BattleReader.pokemonView(battlePokemon, withCompetitive)

            val portraitSize = if (isCompact) COMPACT_PORTRAIT_SIZE else PORTRAIT_SIZE
            val padding = if (isCompact) 4 else 5
            val top = if (isCompact) 4 else 6
            val baseWidth = if (isCompact) COMPACT_TILE_WIDTH else TILE_WIDTH

            // 1ª passada: mede nome/tipos e detalhes para decidir a largura; a barra de HP ocupa o que sobrar
            val identity = panel(null) {
                BattlePokemonRows.nameRow(this, view, config, statusInline = isCompact)
                if (!isCompact) BattlePokemonRows.typeRow(this, view, config)
            } ?: return@guard false
            // Em duplas o painel é pequeno e fica empilhado: o miolo sai numa dica por cima (mais abaixo),
            // e não dentro do painel, senão cobriria o painel de baixo
            val showDetails = !BattleDetails.collapsed()
            val details = if (isCompact || !showDetails) null else panel(null) { BattlePokemonRows.detailRows(this, view, config) }
            val hpTextSpace = BattlePokemonRows.hpTextSpace(view, config)

            val infoWidth = maxOf(identity.contentWidth, MIN_HP_BAR_WIDTH + hpTextSpace, baseWidth - portraitSize - padding * 3)
            val width = maxOf(padding * 3 + portraitSize + infoWidth, (details?.contentWidth ?: 0) + padding * 2)
            val finalInfoWidth = width - portraitSize - padding * 3

            val header = panel(null) {
                BattlePokemonRows.nameRow(this, view, config, statusInline = isCompact)
                if (!isCompact) BattlePokemonRows.typeRow(this, view, config)
                BattlePokemonRows.hpRow(this, view, config, finalInfoWidth - hpTextSpace)
            } ?: return@guard false

            val headerHeight = maxOf(portraitSize, header.contentHeight)
            val detailsHeight = details?.let { it.contentHeight + 7 } ?: 0
            val height = top + headerHeight + detailsHeight + if (isCompact) 3 else 5

            // O Cobblemon alinha o bloco do oponente pela direita; o painel mais largo cresce para a esquerda
            val left = if (reversed) Mth.floor(x) + baseWidth - width else Mth.floor(x)
            val topY = Mth.floor(y)
            val portraitX = if (reversed) left + width - padding - portraitSize else left + padding
            val portraitY = topY + top
            val infoX = if (reversed) left + padding else portraitX + portraitSize + padding
            val accent = colour?.let(::toColor) ?: if (reversed) RetroPalette.ACCENT_OPPONENT else RetroPalette.ACCENT_ALLY

            // O Cobblemon sempre manda "mouse em cima = falso" para os painéis do HUD, então o mod
            // descobre sozinho: é o que abre os detalhes em duplas e destaca o painel sob o cursor
            val pointed = isHovered || mouseOver(left, topY, width, height)

            Alpha.draw(graphics, opacity) {
                actorDisplayName?.let { name ->
                    val font = Minecraft.getInstance().font
                    val nameX = if (reversed) left + width - 4 - font.width(name) else left + 4
                    graphics.drawString(font, name, nameX, topY - 10, RetroPalette.TEXT_DIM, true)
                }
                PixelUi.panel(graphics, left, topY, width, height, accent)
                if (pointed) graphics.fill(left + 1, topY + 1, left + width - 1, topY + height - 1, 0x18FFFFFF)
                if (isSelected && (Util.getMillis() / 400) % 2 == 0L) {
                    PixelUi.outline(graphics, left - 1, topY - 1, width + 2, height + 2, RetroPalette.SHINY)
                }
                portraitWindow(graphics, portraitX, portraitY, portraitSize)
                header.renderRows(graphics, infoX, topY + top + (headerHeight - header.contentHeight) / 2, finalInfoWidth)

                details?.let {
                    val separatorY = topY + top + headerHeight + 3
                    graphics.fill(left + padding, separatorY, left + width - padding, separatorY + 1, RetroPalette.SEPARATOR)
                    it.renderRows(graphics, left + padding, separatorY + 4, width - padding * 2)
                }

                // Dica da tecla que recolhe e mostra o miolo do painel
                if (!isCompact) {
                    BattleDetails.keyHint()?.let { key ->
                        val hint = TinyFont.sanitize(if (showDetails) "$key-" else "$key+")
                        TinyFont.draw(graphics, hint, left + width - 3 - TinyFont.width(hint), topY + height - 6, RetroPalette.TEXT_DIM, shadow = true)
                    }
                }
            }

            CobblemonPortrait.draw(overlay, graphics, portraitX, portraitY, portraitSize, isCompact, partialTicks, reversed, species, state, ballState)

            // O miolo sai numa dica por cima quando não cabe dentro do painel: em duplas (painel compacto,
            // empilhado) e quando os detalhes estão recolhidos, onde serve de espiada rápida
            if (pointed && (isCompact || !showDetails)) {
                val hoverView = BattleReader.pokemonView(battlePokemon, reversed && ConfigManager.config.battleHud.showCompetitiveInfo)
                panel(accent) { BattlePokemonRows.detailRows(this, hoverView, config) }
                    ?.let { TooltipLayer.show(it, left + width - 12, topY + 12) }
            }

            if (dexState == PokedexEntryProgress.OWNED) {
                Alpha.draw(graphics, opacity) {
                    graphics.blit(caughtIndicator, portraitX + 1, portraitY + portraitSize - 6, 5, 5, 0f, 0f, 10, 10, 10, 10)
                }
            }
            true
        }
    }

    /** O mouse só existe quando alguma tela está aberta; no jogo ele fica preso à câmera. */
    private fun mouseOver(left: Int, top: Int, width: Int, height: Int): Boolean {
        val client = Minecraft.getInstance()
        if (client.screen == null) return false
        val window = client.window
        if (window.screenWidth == 0 || window.screenHeight == 0) return false
        val mouseX = client.mouseHandler.xpos() * window.guiScaledWidth / window.screenWidth
        val mouseY = client.mouseHandler.ypos() * window.guiScaledHeight / window.screenHeight
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height
    }

    private fun portraitWindow(graphics: GuiGraphics, x: Int, y: Int, size: Int) {
        PixelUi.outline(graphics, x - 1, y - 1, size + 2, size + 2, RetroPalette.OUTLINE)
        graphics.fill(x, y, x + size, y + size, 0xFF34405C.toInt())
        graphics.fill(x, y + size / 2, x + size, y + size, 0xFF2A3350.toInt())
        graphics.fill(x, y + size - 3, x + size, y + size, 0xFF222A42.toInt())
    }

    private fun toColor(colour: Triple<Float, Float, Float>): Int {
        val (r, g, b) = colour
        return Colors.opaque(((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt())
    }
}
