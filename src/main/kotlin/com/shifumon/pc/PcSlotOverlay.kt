package com.shifumon.pc

import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.client.gui.pc.StorageSlot
import com.cobblemon.mod.common.pokemon.IVs
import com.cobblemon.mod.common.pokemon.Pokemon
import com.shifumon.ShifuMon
import com.shifumon.config.ConfigManager
import com.shifumon.shiny.ShinyIcons
import com.shifumon.util.FeatureGuard
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * Marcas extras nos slots do PC: ícone de shiny e uma bolinha indicando a qualidade dos IVs.
 *
 * O Cobblemon já desenha o nível no canto superior esquerdo, o sexo no superior direito e o item
 * segurado no inferior direito, então o shiny fica no meio da direita e a bolinha no inferior esquerdo.
 */
object PcSlotOverlay {
    private const val SLOT_SIZE = StorageSlot.SIZE
    private const val SHINY_SIZE = 8
    private const val ORB_SIZE = 7

    private val statOrder = listOf(
        Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED,
    )

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

    /** Dourada: seis IVs máximos. Azul: cinco. Vermelha: quatro. Roxa: todos zerados. */
    private enum class IvTier(textureName: String) {
        PERFECT("iv_perfect"),
        HIGH("iv_high"),
        MID("iv_mid"),
        ZERO("iv_zero");

        val texture: ResourceLocation = ShifuMon.id("textures/gui/pc/$textureName.png")
    }

    /**
     * O limite da config decide a partir de quantos IVs máximos a bolinha aparece; seis máximos e
     * todos zerados aparecem sempre, porque são os dois extremos que interessam de longe.
     */
    private fun ivTier(pokemon: Pokemon, minimumPerfect: Int): IvTier? {
        val values = statOrder.map { pokemon.ivs.getOrDefault(it) }
        val perfect = values.count { it >= IVs.MAX_VALUE }
        return when {
            perfect >= statOrder.size -> IvTier.PERFECT
            values.all { it == 0 } -> IvTier.ZERO
            perfect < minimumPerfect.coerceIn(1, 6) -> null
            perfect >= 5 -> IvTier.HIGH
            else -> IvTier.MID
        }
    }

    private fun drawOrb(graphics: GuiGraphics, x: Int, y: Int, tier: IvTier) {
        graphics.blit(tier.texture, x, y, ORB_SIZE, ORB_SIZE, 0f, 0f, ORB_SIZE, ORB_SIZE, ORB_SIZE, ORB_SIZE)
    }
}
