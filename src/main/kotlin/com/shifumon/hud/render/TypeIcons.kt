package com.shifumon.hud.render

import com.shifumon.ShifuMon
import com.shifumon.config.ConfigManager
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * Símbolos dos 18 tipos em 12x12.
 *
 * Cada tipo tem seu desenho sobre um tile na cor do tipo, porque só a cor confundia (Terra, Pedra
 * e Elétrico ficavam parecidos). Tipos de datapacks continuam com as três letras, que é o que dá
 * para montar sem um desenho próprio.
 */
object TypeIcons {
    const val SIZE = 12

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

    /** O tile já vem com fundo e moldura na cor do tipo, então é só desenhar. */
    fun draw(graphics: GuiGraphics, texture: ResourceLocation, x: Int, y: Int) {
        graphics.blit(texture, x, y, SIZE, SIZE, 0f, 0f, SIZE, SIZE, SIZE, SIZE)
    }
}
