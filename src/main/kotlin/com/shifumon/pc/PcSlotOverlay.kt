package com.shifumon.pc

import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.client.gui.pc.StorageSlot
import com.cobblemon.mod.common.pokemon.IVs
import com.cobblemon.mod.common.pokemon.Pokemon
import com.shifumon.config.ConfigManager
import com.shifumon.shiny.ShinyIcons
import com.shifumon.util.Colors
import com.shifumon.util.FeatureGuard
import net.minecraft.client.gui.GuiGraphics

/**
 * Marcas extras nos slots do PC: ícone de shiny e uma bolinha indicando a qualidade dos IVs.
 *
 * O Cobblemon já desenha o nível no canto superior esquerdo, o sexo no superior direito e o item
 * segurado no inferior direito, então o shiny fica no meio da direita e a bolinha no inferior esquerdo.
 */
object PcSlotOverlay {
    private const val SLOT_SIZE = StorageSlot.SIZE
    private const val SHINY_SIZE = 8
    private const val ORB_SIZE = 6
    private const val ORB_SHADE_FROM_ROW = 4

    private val statOrder = listOf(
        Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED,
    )

    /** Linhas da bolinha 6x6: contorno, corpo e a parte de baixo sombreada. */
    private val orbOutline = listOf(1 to 5, 0 to 6, 0 to 6, 0 to 6, 0 to 6, 1 to 5)
    private val orbBody = listOf(2 to 4, 1 to 5, 1 to 5, 1 to 5, 1 to 5, 2 to 4)

    /** Chamado pelo mixin no fim de `StorageSlot.renderSlot`. */
    @JvmStatic
    fun render(slot: StorageSlot, graphics: GuiGraphics) {
        val config = ConfigManager.config.pc
        if (!config.showShinyIcon && !config.showIvBadge) return
        FeatureGuard.guard("pc.slot_overlay", Unit) {
            val pokemon = slot.getPokemon() ?: return@guard
            val x = slot.x
            val y = slot.y

            if (config.showShinyIcon && pokemon.shiny) {
                ShinyIcons.draw(graphics, null, x + SLOT_SIZE - SHINY_SIZE - 1, y + 8, SHINY_SIZE)
            }
            if (config.showIvBadge) {
                ivTier(pokemon, config.minPerfectIvs)?.let { tier ->
                    drawOrb(graphics, x + 2, y + SLOT_SIZE - ORB_SIZE - 2, tier)
                }
            }
        }
    }

    /** Dourada: seis IVs máximos. Azul: quase perfeito. Roxa: todos os IVs zerados. */
    private enum class IvTier(val color: Int, val sparkle: Boolean) {
        PERFECT(0xFFF8D050.toInt(), true),
        HIGH(0xFF3C9BF0.toInt(), false),
        ZERO(0xFFA85CF0.toInt(), false),
    }

    private fun ivTier(pokemon: Pokemon, minimumPerfect: Int): IvTier? {
        val values = statOrder.map { pokemon.ivs.getOrDefault(it) }
        val perfect = values.count { it >= IVs.MAX_VALUE }
        return when {
            perfect >= statOrder.size -> IvTier.PERFECT
            values.all { it == 0 } -> IvTier.ZERO
            perfect >= minimumPerfect.coerceAtLeast(1) -> IvTier.HIGH
            else -> null
        }
    }

    /** Esfera 6x6: contorno escuro, sombra embaixo e brilho em cima à esquerda. */
    private fun drawOrb(graphics: GuiGraphics, x: Int, y: Int, tier: IvTier) {
        val outline = Colors.darken(tier.color, 0.28f)
        val shade = Colors.darken(tier.color, 0.68f)
        val light = Colors.lighten(tier.color, 0.55f)

        orbOutline.forEachIndexed { row, (start, end) ->
            graphics.fill(x + start, y + row, x + end, y + row + 1, outline)
        }
        orbBody.forEachIndexed { row, (start, end) ->
            graphics.fill(x + start, y + row, x + end, y + row + 1, if (row >= ORB_SHADE_FROM_ROW) shade else tier.color)
        }
        // brilho: um ponto claro, como uma bolinha de vidro
        graphics.fill(x + 2, y + 1, x + 3, y + 2, light)
        graphics.fill(x + 1, y + 2, x + 2, y + 3, light)
        if (tier.sparkle) graphics.fill(x + 3, y + 3, x + 4, y + 4, Colors.lighten(tier.color, 0.9f))
    }
}
