package com.shifumon.hud

import com.shifumon.config.HudPosition
import com.shifumon.hud.panel.Panel
import net.minecraft.network.chat.Component

/**
 * Um elemento posicionável da HUD. O elemento só descreve o conteúdo; o [HudManager] cuida de
 * posição, visibilidade e isolamento de erros, e o editor de HUD reaproveita o mesmo [build].
 */
interface HudElement {
    /** Chave usada na config (`hud.positions`) e nas traduções `shifumon.hud.<id>`. */
    val id: String
    val displayName: Component
    val defaultPosition: HudPosition

    fun isEnabled(): Boolean

    /** Monta o painel do frame atual. `preview = true` usa dados de exemplo (editor de HUD). */
    fun build(preview: Boolean): Panel?
}
