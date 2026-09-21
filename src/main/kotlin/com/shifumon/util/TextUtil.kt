package com.shifumon.util

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import java.text.Normalizer
import java.util.Locale

object TextUtil {
    private val diacritics = Regex("\\p{M}+")
    private val separators = Regex("[\\s_\\-]+")
    private val translationCache = HashMap<String, String>()
    private var cachedLanguage: String? = null

    /** Minúsculas, sem acentos e sem espaços: "Psíquico" -> "psiquico". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .lowercase(Locale.ROOT)
            .replace(separators, "")

    /** Tradução já normalizada, com cache por idioma (usada nos filtros do PC a cada frame). */
    fun normalizedTranslation(key: String): String {
        val language = Minecraft.getInstance().languageManager.selected
        if (language != cachedLanguage) {
            translationCache.clear()
            cachedLanguage = language
        }
        return translationCache.getOrPut(key) { normalize(I18n.get(key)) }
    }

    fun prettifyId(id: String): String =
        id.split('_', ' ').filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { it.titlecase(Locale.ROOT) } }

    fun decimal(value: Float, decimals: Int): String = String.format(Locale.ROOT, "%.${decimals}f", value)
}
