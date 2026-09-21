package com.shifumon.resource

import com.shifumon.ShifuMon
import com.shifumon.util.FeatureGuard
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.SharedConstants
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.AbstractPackResources
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.repository.RepositorySource
import net.minecraft.server.packs.resources.IoSupplier
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.util.Optional

/**
 * Pacote de recursos "ShifuMon Retro", com as texturas geradas por [RetroTheme].
 *
 * É opcional (vem desligado): servidores e modpacks costumam ter texturas próprias, e um pacote
 * ligado por padrão passaria por cima delas. Quem quiser ativa em Opções > Pacotes de Recursos.
 * Entra na lista de pacotes do cliente pelo `PackRepositoryMixin`.
 */
object RetroResourcePack {
    // Mesmo id das versões anteriores, para quem já tinha ativado continuar com o pacote ativo
    private const val PACK_ID = "${ShifuMon.MOD_ID}:shifumon_retro"

    private val packSource = PackSource.create(PackSource.NO_DECORATION, false)

    @JvmField
    val SOURCE = RepositorySource { consumer ->
        FeatureGuard.guard("resourcepack.retro", Unit) {
            val location = PackLocationInfo(PACK_ID, Component.translatable("shifumon.resourcepack.retro"), packSource, Optional.empty())
            val resources = object : Pack.ResourcesSupplier {
                override fun openPrimary(location: PackLocationInfo): PackResources = RetroPackResources(location)
                override fun openFull(location: PackLocationInfo, metadata: Pack.Metadata): PackResources = RetroPackResources(location)
            }
            val selection = PackSelectionConfig(false, Pack.Position.TOP, false)
            Pack.readMetaAndCreate(location, resources, PackType.CLIENT_RESOURCES, selection)?.let(consumer::accept)
        }
    }
}

/** Serve as texturas repintadas; nada é gravado em disco. */
private class RetroPackResources(location: PackLocationInfo) : AbstractPackResources(location) {
    override fun getRootResource(vararg elements: String): IoSupplier<InputStream>? = when (elements.joinToString("/")) {
        PackResources.PACK_META -> IoSupplier { ByteArrayInputStream(packMeta()) }
        "pack.png" -> FabricLoader.getInstance().getModContainer(ShifuMon.MOD_ID)
            .flatMap { it.findPath("assets/${ShifuMon.MOD_ID}/icon.png") }
            .map { path -> IoSupplier { Files.newInputStream(path) } }
            .orElse(null)
        else -> null
    }

    override fun getResource(type: PackType, location: ResourceLocation): IoSupplier<InputStream>? {
        if (type != PackType.CLIENT_RESOURCES) return null
        val texture = RetroTheme.texture(location) ?: return null
        return IoSupplier { ByteArrayInputStream(texture) }
    }

    override fun listResources(type: PackType, namespace: String, path: String, output: PackResources.ResourceOutput) {
        if (type != PackType.CLIENT_RESOURCES) return
        for (location in RetroTheme.locations()) {
            if (location.namespace != namespace || !location.path.startsWith("$path/")) continue
            val texture = RetroTheme.texture(location) ?: continue
            output.accept(location, IoSupplier { ByteArrayInputStream(texture) })
        }
    }

    override fun getNamespaces(type: PackType): Set<String> =
        if (type == PackType.CLIENT_RESOURCES) RetroTheme.namespaces() else emptySet()

    override fun close() {}

    private fun packMeta(): ByteArray {
        val format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES)
        return """{"pack":{"pack_format":$format,"description":{"translate":"shifumon.resourcepack.retro.description"}}}"""
            .toByteArray(Charsets.UTF_8)
    }
}
