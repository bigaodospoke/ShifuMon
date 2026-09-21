package com.shifumon.compat.modmenu

import com.shifumon.config.gui.ShifuMonConfigScreen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

/** Botão "Configurar" do ShifuMon no Mod Menu. Só é carregado se o Mod Menu estiver instalado. */
class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory<ShifuMonConfigScreen> { parent -> ShifuMonConfigScreen(parent) }
}
