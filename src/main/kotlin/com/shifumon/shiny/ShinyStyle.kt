package com.shifumon.shiny

/** Estilos do ícone de shiny. [textureName] aponta para `textures/gui/shiny/<nome>.png`. */
enum class ShinyStyle(val textureName: String?) {
    COBBLEMON(null),
    STAR("star"),
    SQUARE("square"),
    DIAMOND("diamond"),
    CROWN("crown"),
    CUSTOM(null);

    val translationKey: String get() = "shifumon.shiny.style.${name.lowercase()}"
}
