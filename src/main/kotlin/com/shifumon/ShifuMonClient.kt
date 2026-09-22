package com.shifumon

import com.shifumon.battle.BattleTracker
import com.shifumon.boxsearch.BoxSearchOverlay
import com.shifumon.config.ConfigManager
import com.shifumon.hud.HudManager
import com.shifumon.hud.TooltipLayer
import com.shifumon.keybind.ShifuMonKeys
import com.shifumon.pokemoninfo.PokemonLookTracker
import com.shifumon.shiny.ShinyIcons
import com.shifumon.util.TexturePackCheck
import net.fabricmc.api.ClientModInitializer

/** Entrypoint client-side. Cada módulo registra os próprios eventos. */
object ShifuMonClient : ClientModInitializer {
    override fun onInitializeClient() {
        ConfigManager.load()
        TexturePackCheck.register()
        ShifuMonKeys.register()
        ShinyIcons.register()
        PokemonLookTracker.register()
        BattleTracker.register()
        BoxSearchOverlay.register()
        HudManager.register()
        TooltipLayer.register()
        ShifuMon.LOGGER.info("{} inicializado", ShifuMon.MOD_NAME)
    }
}
