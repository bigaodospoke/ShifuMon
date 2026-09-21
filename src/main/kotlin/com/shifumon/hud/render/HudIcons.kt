package com.shifumon.hud.render

import com.cobblemon.mod.common.pokemon.Gender
import com.shifumon.ShifuMon
import net.minecraft.resources.ResourceLocation

/** Ícones pixel art 9x9 da HUD (gerados por tools/PixelArtGenerator.java). */
object HudIcons {
    const val SIZE = 9

    val TURN = texture("turn")
    private val MALE = texture("gender_male")
    private val FEMALE = texture("gender_female")
    private val SUN = texture("weather_sun")
    private val RAIN = texture("weather_rain")
    private val SAND = texture("weather_sand")
    private val SNOW = texture("weather_snow")
    private val ELECTRIC = texture("terrain_electric")
    private val GRASSY = texture("terrain_grassy")
    private val MISTY = texture("terrain_misty")
    private val PSYCHIC = texture("terrain_psychic")

    fun gender(gender: Gender): ResourceLocation? = when (gender) {
        Gender.MALE -> MALE
        Gender.FEMALE -> FEMALE
        else -> null
    }

    /** [id] é o id do Showdown usado nas chaves `cobblemon.battle.weather.<id>.*`. */
    fun weather(id: String): ResourceLocation? = when (id) {
        "raindance", "primordialsea" -> RAIN
        "sunnyday", "desolateland" -> SUN
        "sandstorm" -> SAND
        "hail", "snow", "snowscape" -> SNOW
        else -> null
    }

    fun terrain(id: String): ResourceLocation? = when (id) {
        "electricterrain" -> ELECTRIC
        "grassyterrain" -> GRASSY
        "mistyterrain" -> MISTY
        "psychicterrain" -> PSYCHIC
        else -> null
    }

    private fun texture(name: String): ResourceLocation = ShifuMon.id("textures/gui/hud/$name.png")
}
