package com.shifumon.util

import com.shifumon.hud.render.TinyFont
import net.minecraft.locale.Language

/** Abreviações de 3 letras dos tipos (FOG, AGU, PLA...), traduzidas e prontas para a fonte pixel. */
object TypeNames {
    fun short(typeId: String): String {
        val key = "shifumon.type.short.${typeId.lowercase()}"
        val language = Language.getInstance()
        return TinyFont.sanitize(if (language.has(key)) language.getOrDefault(key) else typeId.take(3))
    }
}
