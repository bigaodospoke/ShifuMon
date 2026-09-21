package com.shifumon.capture

import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.item.PokeBallItem
import com.shifumon.util.FeatureGuard
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Estimativa da chance de captura, reproduzindo a fórmula da `CobblemonCaptureCalculator` do Cobblemon.
 *
 * O servidor não envia HP atual nem status de Pokémon selvagem, então saem dois números:
 * [worst] com HP cheio e sem status (a conta é exata, porque o HP máximo se cancela na fórmula)
 * e [best] com 1 de HP e dormindo (o HP máximo é estimado pela base da espécie).
 */
data class CaptureEstimate(val ballName: Component, val worst: Double, val best: Double)

object CaptureEstimator {
    private const val SLEEP_BONUS = 2.5f
    private val statuslessBonus = 1f

    fun estimate(entity: PokemonEntity): CaptureEstimate? = FeatureGuard.guard("capture.estimate", null) {
        val player = Minecraft.getInstance().player ?: return@guard null
        val stack = heldPokeBall(player.mainHandItem) ?: heldPokeBall(player.offhandItem) ?: return@guard null
        val ball = (stack.item as PokeBallItem).pokeBall
        val pokemon = entity.pokemon
        val modifier = ball.catchRateModifier
        val name = stack.hoverName

        if (modifier.isGuaranteed()) return@guard CaptureEstimate(name, 1.0, 1.0)

        val catchRate = pokemon.form.catchRate.toFloat()
        // Fora de batalha o Cobblemon corta a taxa pela metade
        val inBattleModifier = if (entity.battleId != null) 1f else 0.5f
        val valid = runCatching { modifier.isValid(player, pokemon) }.getOrDefault(false)
        val ballBonus = if (valid) runCatching { modifier.value(player, pokemon) }.getOrDefault(1f) else 1f
        val mutator: (Float, Float) -> Float = runCatching { modifier.behavior(player, pokemon).mutator }
            .getOrNull() ?: { base, bonus -> base * bonus }

        val level = entity.entityData.get(PokemonEntity.LABEL_LEVEL).takeIf { it > 0 } ?: pokemon.level
        val levelBonus = if (level < 13) max((36 - 2 * level) / 10, 1) else 1
        val maxHealth = estimateMaxHealth(entity, level)

        fun modifiedRate(currentHealth: Float, statusBonus: Float): Float {
            val base = (3f * maxHealth - 2f * currentHealth) * catchRate * inBattleModifier
            return mutator(base, ballBonus) / (3f * maxHealth) * statusBonus * levelBonus
        }

        CaptureEstimate(
            ballName = name,
            worst = successChance(modifiedRate(maxHealth.toFloat(), statuslessBonus)),
            best = successChance(modifiedRate(1f, SLEEP_BONUS)),
        )
    }

    /** Quatro balanços bem-sucedidos, como no cálculo do Cobblemon. */
    private fun successChance(modifiedCatchRate: Float): Double {
        if (modifiedCatchRate <= 0f) return 0.0
        if (modifiedCatchRate >= 255f) return 1.0
        val shakeProbability = (65536.0 / (255.0 / modifiedCatchRate).pow(0.1875)).roundToInt().coerceIn(0, 65536)
        return (shakeProbability / 65536.0).pow(4)
    }

    /** Fórmula padrão de HP, com IVs máximos e sem EVs (o cliente não conhece os de um selvagem). */
    private fun estimateMaxHealth(entity: PokemonEntity, level: Int): Int {
        val baseHp = entity.pokemon.form.baseStats[Stats.HP] ?: 50
        return ((2 * baseHp + 31) * level / 100 + level + 10).coerceAtLeast(1)
    }

    private fun heldPokeBall(stack: ItemStack): ItemStack? = stack.takeIf { it.item is PokeBallItem }
}
