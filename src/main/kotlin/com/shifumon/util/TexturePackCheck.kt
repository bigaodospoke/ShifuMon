package com.shifumon.util

import com.shifumon.ShifuMon
import com.shifumon.config.ConfigManager
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager

/**
 * Descobre se o jogador (ou o servidor) usa um pacote de recursos que retexturiza uma tela do
 * Cobblemon, como os pacotes de barra de HP. Nesse caso o ShifuMon não desenha por cima daquele
 * elemento e a textura do pacote continua aparecendo.
 */
object TexturePackCheck {
    val BATTLE_TILE: ResourceLocation = cobblemon("textures/gui/battle/battle_info_base.png")
    val BATTLE_LOG: ResourceLocation = cobblemon("textures/gui/battle/battle_log.png")
    val MOVE_BUTTON: ResourceLocation = cobblemon("textures/gui/battle/battle_move.png")

    private val cache = HashMap<ResourceLocation, Boolean>()

    fun register() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(object : SimpleSynchronousResourceReloadListener {
            override fun getFabricId(): ResourceLocation = ShifuMon.id("texture_pack_check")
            override fun onResourceManagerReload(manager: ResourceManager) = cache.clear()
        })
    }

    /** true = a opção de respeitar pacotes está ligada e algum pacote retexturiza [texture]. */
    fun shouldYieldTo(texture: ResourceLocation): Boolean =
        ConfigManager.config.interfaceTweaks.respectTexturePacks && isCustomized(texture)

    /** Pacotes do jogador ("file/...") e do servidor ("server...") contam; mods e o próprio Cobblemon não. */
    fun isCustomized(texture: ResourceLocation): Boolean = cache.getOrPut(texture) {
        val resource = Minecraft.getInstance().resourceManager.getResource(texture).orElse(null) ?: return@getOrPut false
        val source = resource.sourcePackId()
        source.startsWith("file/") || source.startsWith("server")
    }

    private fun cobblemon(path: String) = ResourceLocation.fromNamespaceAndPath("cobblemon", path)
}
