package com.shifumon.shiny

import com.mojang.blaze3d.platform.NativeImage
import com.shifumon.ShifuMon
import com.shifumon.config.ConfigManager
import com.shifumon.config.ShinyConfig
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.nio.file.Files
import java.nio.file.Path

/** Resolve qual textura de shiny usar e carrega o ícone customizado do usuário. */
object ShinyIcons {
    /** Ícone 16x16 que o Cobblemon usa no Summary, PC e Trade. */
    @JvmField
    val COBBLEMON_ICON: ResourceLocation = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/summary/icon_shiny.png")

    const val ICON_SIZE = 16

    private val CUSTOM_ICON: ResourceLocation = ShifuMon.id("dynamic/custom_shiny")
    private val builtInTextures: Map<ShinyStyle, ResourceLocation> = ShinyStyle.entries
        .mapNotNull { style -> style.textureName?.let { style to ShifuMon.id("textures/gui/shiny/$it.png") } }
        .toMap()

    private var customWidth = ICON_SIZE
    private var customHeight = ICON_SIZE

    var customIconLoaded = false
        private set

    /** Mensagem de status do ícone customizado, exibida na tela de config. */
    var customIconStatus: Component? = null
        private set

    fun register() {
        ClientLifecycleEvents.CLIENT_STARTED.register { reloadCustomIcon() }
    }

    fun texture(style: ShinyStyle): ResourceLocation = when (style) {
        ShinyStyle.COBBLEMON -> COBBLEMON_ICON
        ShinyStyle.CUSTOM -> if (customIconLoaded) CUSTOM_ICON else builtInTextures.getValue(ShinyStyle.STAR)
        else -> builtInTextures.getValue(style)
    }

    fun activeStyle(): ShinyStyle = ConfigManager.config.shiny.let { if (it.enabled) it.style else ShinyStyle.COBBLEMON }

    /** Hook do mixin em `GuiUtilsKt.blitk`: troca somente o ícone de shiny do Cobblemon. */
    @JvmStatic
    fun resolveTexture(original: ResourceLocation?): ResourceLocation? {
        if (original == null || original != COBBLEMON_ICON) return original
        return texture(activeStyle())
    }

    /** Desenha um estilo (ou o ativo, se `null`). Use tamanhos múltiplos de 8 para manter o pixel art nítido. */
    fun draw(graphics: GuiGraphics, style: ShinyStyle?, x: Int, y: Int, size: Int) {
        val texture = texture(style ?: activeStyle())
        val isCustom = texture == CUSTOM_ICON
        val width = if (isCustom) customWidth else ICON_SIZE
        val height = if (isCustom) customHeight else ICON_SIZE
        graphics.blit(texture, x, y, size, size, 0f, 0f, width, height, width, height)
    }

    fun customIconPath(): Path {
        val fileName = runCatching { Path.of(ConfigManager.config.shiny.customIconFile).fileName?.toString() }.getOrNull()
        return ConfigManager.configDir.resolve(fileName ?: ShinyConfig.DEFAULT_CUSTOM_ICON)
    }

    /** Recarrega `config/shifumon/<customIconFile>`. Deve rodar na thread de render. */
    fun reloadCustomIcon() {
        val textures = Minecraft.getInstance().textureManager
        textures.release(CUSTOM_ICON)
        customIconLoaded = false

        val path = customIconPath()
        val fileName = path.fileName.toString()
        if (!Files.isRegularFile(path)) {
            customIconStatus = Component.translatable("shifumon.shiny.custom.missing", fileName)
            return
        }
        try {
            val image = Files.newInputStream(path).use { NativeImage.read(it) }
            customWidth = image.width
            customHeight = image.height
            textures.register(CUSTOM_ICON, DynamicTexture(image))
            customIconLoaded = true
            customIconStatus = Component.translatable("shifumon.shiny.custom.loaded", fileName, image.width, image.height)
        } catch (error: Exception) {
            customIconStatus = Component.translatable("shifumon.shiny.custom.invalid", fileName)
            ShifuMon.LOGGER.warn("Não foi possível carregar o ícone shiny customizado {}", path, error)
        }
    }
}
