package com.shifumon.battle

import com.shifumon.util.TextUtil
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component

/** Nomes de clima/terreno/efeitos: tradução do ShifuMon quando existe, id formatado como fallback. */
object BattleNames {
    fun weather(id: String): Component = translatedOr("shifumon.weather.$id", id)

    fun terrain(id: String): Component = translatedOr("shifumon.terrain.$id", id)

    fun field(id: String): Component = translatedOr("shifumon.field.$id", id)

    /** Condições de lado têm o nome do golpe que as cria (Reflect, Tailwind...), exceto os Pledges. */
    fun side(id: String): Component =
        if (Language.getInstance().has("shifumon.side.$id")) Component.translatable("shifumon.side.$id") else translatedOr("cobblemon.move.$id", id)

    private fun translatedOr(key: String, id: String): Component =
        if (Language.getInstance().has(key)) Component.translatable(key) else Component.literal(TextUtil.prettifyId(id))
}
