package com.shifumon.config

import kotlin.math.roundToInt

/**
 * Âncora de um elemento da HUD. A posição salva é relativa à âncora, então a HUD continua no
 * lugar certo ao mudar a resolução ou a escala da interface.
 */
enum class HudAnchor(private val factorX: Float, private val factorY: Float) {
    TOP_LEFT(0f, 0f), TOP_CENTER(0.5f, 0f), TOP_RIGHT(1f, 0f),
    CENTER_LEFT(0f, 0.5f), CENTER(0.5f, 0.5f), CENTER_RIGHT(1f, 0.5f),
    BOTTOM_LEFT(0f, 1f), BOTTOM_CENTER(0.5f, 1f), BOTTOM_RIGHT(1f, 1f);

    fun originX(screenWidth: Int, elementWidth: Int): Int = ((screenWidth - elementWidth) * factorX).roundToInt()

    fun originY(screenHeight: Int, elementHeight: Int): Int = ((screenHeight - elementHeight) * factorY).roundToInt()

    companion object {
        /** Âncora do terço da tela onde está o centro do elemento. */
        fun nearest(centerX: Float, centerY: Float, screenWidth: Int, screenHeight: Int): HudAnchor {
            val column = thirdOf(centerX, screenWidth)
            val row = thirdOf(centerY, screenHeight)
            return entries[row * 3 + column]
        }

        private fun thirdOf(value: Float, size: Int): Int = when {
            value < size / 3f -> 0
            value < size * 2f / 3f -> 1
            else -> 2
        }
    }
}
