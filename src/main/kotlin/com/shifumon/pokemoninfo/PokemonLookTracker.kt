package com.shifumon.pokemoninfo

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.shifumon.config.ConfigManager
import com.shifumon.keybind.ShifuMonKeys
import com.shifumon.util.FeatureGuard
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult

/** Descobre para qual Pokémon o jogador está olhando (a cada tick, com alcance configurável). */
object PokemonLookTracker {
    /** Mantém o painel alguns ticks após desviar o olhar, para não piscar. */
    private const val LINGER_TICKS = 8

    var target: PokemonEntity? = null
        private set

    private var lingerTicks = 0

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register(::onEndTick)
    }

    private fun onEndTick(client: Minecraft) {
        val config = ConfigManager.config.pokemonInfo
        val active = config.enabled && ShifuMonKeys.infoHudVisible &&
            (!config.requireSneak || client.player?.isShiftKeyDown == true)
        val found = if (active) {
            FeatureGuard.guard("pokemon_info.raycast", null) { raycast(client, config.maxDistance.toDouble()) }
        } else {
            null
        }

        when {
            found != null -> {
                target = found
                lingerTicks = LINGER_TICKS
            }
            active && lingerTicks > 0 && target?.isRemoved == false -> lingerTicks--
            else -> {
                target = null
                lingerTicks = 0
            }
        }
    }

    private fun raycast(client: Minecraft, maxDistance: Double): PokemonEntity? {
        val camera = client.cameraEntity ?: return null
        val level = client.level ?: return null
        val eye = camera.getEyePosition(1f)
        val reach = camera.getViewVector(1f).scale(maxDistance)

        // Blocos bloqueiam a visão: a busca por entidades para no primeiro bloco atingido
        val blockHit = level.clip(ClipContext(eye, eye.add(reach), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, camera))
        val end = if (blockHit.type == HitResult.Type.MISS) eye.add(reach) else blockHit.location

        val searchBox = camera.boundingBox.expandTowards(reach).inflate(1.0)
        val hit = ProjectileUtil.getEntityHitResult(
            camera, eye, end, searchBox,
            { it is PokemonEntity && !it.isInvisible },
            eye.distanceToSqr(end),
        )
        return hit?.entity as? PokemonEntity
    }
}
