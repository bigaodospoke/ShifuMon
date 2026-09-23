package com.shifumon.hud.render

import com.shifumon.ShifuMon
import com.shifumon.config.ConfigManager
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * Símbolos dos 18 tipos em 8x8.
 *
 * As cores de Terra, Pedra e Elétrico ficam parecidas na tela, ainda mais em etiquetas pequenas,
 * então cada tipo tem um desenho bem diferente (raio, montanha, camadas de terra...). Tipos de
 * datapacks continuam com as três letras, que é o que dá para montar sem um desenho próprio.
 */
object TypeIcons {
    const val SIZE = 8

    private val known = setOf(
        "normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison", "ground",
        "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy",
    )

    private val textures = HashMap<String, ResourceLocation>()

    /** Textura do tipo, ou `null` quando não há desenho (tipo de datapack) ou a opção está desligada. */
    fun of(typeId: String): ResourceLocation? {
        if (!ConfigManager.config.interfaceTweaks.typeIcons) return null
        val id = typeId.lowercase()
        if (id !in known) return null
        return textures.getOrPut(id) { ShifuMon.id("textures/gui/types/$id.png") }
    }

    /** Desenha o símbolo com uma sombra atrás, para ele aparecer bem sobre qualquer cor de fundo. */
    fun draw(graphics: GuiGraphics, texture: ResourceLocation, x: Int, y: Int) {
        graphics.setColor(0f, 0f, 0f, 0.55f)
        graphics.blit(texture, x + 1, y + 1, SIZE, SIZE, 0f, 0f, SIZE, SIZE, SIZE, SIZE)
        graphics.setColor(1f, 1f, 1f, 1f)
        graphics.blit(texture, x, y, SIZE, SIZE, 0f, 0f, SIZE, SIZE, SIZE, SIZE)
    }
}
