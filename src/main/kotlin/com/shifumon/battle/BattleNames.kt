package com.shifumon.battle

import com.shifumon.util.TextUtil
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component

/** Nomes de clima/terreno/efeitos: tradução do ShifuMon quando existe, id formatado como fallback. */
object BattleNames {
    fun weather(id: String): Component = translatedOr("shifumon.weather.$id", id)

    fun terrain(id: String): Component = translatedOr("shifumon.terrain.$id", id)

    fun field(id: String): Component = translatedOr("shifumon.field.$id", id)

    private fun translatedOr(key: String, id: String): Component =
        if (Language.getInstance().has(key)) Component.translatable(key) else Component.literal(TextUtil.prettifyId(id))
}
