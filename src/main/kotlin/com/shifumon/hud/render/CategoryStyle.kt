package com.shifumon.hud.render

import com.cobblemon.mod.common.api.moves.categories.DamageCategory
import net.minecraft.locale.Language

/** Etiqueta pixel da categoria do golpe (físico, especial, status). */
object CategoryStyle {
    fun label(category: DamageCategory): String {
        val key = "shifumon.category.short.${category.name}"
        val language = Language.getInstance()
        return TinyFont.sanitize(if (language.has(key)) language.getOrDefault(key) else category.name.take(3))
    }

    fun color(category: DamageCategory): Int = when (category.name) {
        "physical" -> 0xFFC8502C
        "special" -> 0xFF4C6CD8
        else -> 0xFF7C7C90
    }.toInt()
}
