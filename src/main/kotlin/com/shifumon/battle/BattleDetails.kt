package com.shifumon.battle

import com.shifumon.config.ConfigManager
import com.shifumon.keybind.ShifuMonKeys

/**
 * Mostrar ou esconder o miolo dos painéis de batalha (fraquezas, resistências, atributos, stats base).
 *
 * Em batalha simples tudo isso ocupa bastante tela, e em duplas seriam quatro painéis ao mesmo tempo:
 * por isso dá para recolher com uma tecla, e a escolha fica salva. Em duplas os painéis são pequenos
 * e ficam empilhados, então o miolo aparece só no painel sob o mouse, desenhado por cima dos outros.
 */
object BattleDetails {
    fun collapsed(): Boolean = ConfigManager.config.interfaceTweaks.detailsCollapsed

    fun toggle() {
        val config = ConfigManager.config.interfaceTweaks
        config.detailsCollapsed = !config.detailsCollapsed
        ConfigManager.save()
    }

    /** "V" para o jogador saber qual tecla recolhe; vazio se o atalho não tiver tecla. */
    fun keyHint(): String? = ShifuMonKeys.toggleDetailsKeyName()
}
