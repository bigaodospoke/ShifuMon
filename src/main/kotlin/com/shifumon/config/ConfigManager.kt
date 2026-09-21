package com.shifumon.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.shifumon.ShifuMon
import net.fabricmc.loader.api.FabricLoader
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CopyOnWriteArrayList

/** Carrega, valida e salva a configuração em JSON. */
object ConfigManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val saveListeners = CopyOnWriteArrayList<(ShifuMonConfig) -> Unit>()

    val configDir: Path = FabricLoader.getInstance().configDir.resolve(ShifuMon.MOD_ID)
    private val configFile: Path = configDir.resolve("${ShifuMon.MOD_ID}.json")

    var config: ShifuMonConfig = ShifuMonConfig()
        private set

    fun load() {
        config = readFromDisk() ?: ShifuMonConfig()
        config.sanitize()
        migrate(config)
        save()
    }

    fun save() {
        try {
            Files.createDirectories(configDir)
            val temp = configFile.resolveSibling("${configFile.fileName}.tmp")
            Files.newBufferedWriter(temp, StandardCharsets.UTF_8).use { gson.toJson(config, it) }
            try {
                Files.move(temp, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (unsupported: AtomicMoveNotSupportedException) {
                Files.move(temp, configFile, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (error: Exception) {
            ShifuMon.LOGGER.error("Falha ao salvar a configuração em {}", configFile, error)
        }
        saveListeners.forEach { it(config) }
    }

    fun resetToDefaults() {
        config = ShifuMonConfig()
        save()
    }

    fun onSave(listener: (ShifuMonConfig) -> Unit) {
        saveListeners += listener
    }

    private fun readFromDisk(): ShifuMonConfig? {
        if (!Files.exists(configFile)) return null
        return try {
            Files.newBufferedReader(configFile, StandardCharsets.UTF_8).use { gson.fromJson(it, ShifuMonConfig::class.java) }
        } catch (error: Exception) {
            val backup = configFile.resolveSibling("${configFile.fileName}.broken")
            runCatching { Files.move(configFile, backup, StandardCopyOption.REPLACE_EXISTING) }
            ShifuMon.LOGGER.error("Configuração inválida; backup em {} e valores padrão restaurados.", backup, error)
            null
        }
    }

    /** Ponto único para migrações de formato entre versões do mod. */
    private fun migrate(config: ShifuMonConfig) {
        if (config.configVersion < ShifuMonConfig.CURRENT_VERSION) {
            config.configVersion = ShifuMonConfig.CURRENT_VERSION
        }
    }
}
