package com.shifumon

import net.minecraft.resources.ResourceLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Constantes globais do mod. */
object ShifuMon {
    const val MOD_ID = "shifumon"
    const val MOD_NAME = "ShifuMon"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_NAME)

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}
