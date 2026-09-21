package com.shifumon.util

object Colors {
    fun opaque(rgb: Int): Int = rgb or 0xFF000000.toInt()

    fun darken(argb: Int, factor: Float): Int = mapChannels(argb) { (it * factor).toInt() }

    fun lighten(argb: Int, factor: Float): Int = mapChannels(argb) { (it + (255 - it) * factor).toInt() }

    private inline fun mapChannels(argb: Int, transform: (Int) -> Int): Int {
        val a = argb ushr 24 and 0xFF
        val r = transform(argb shr 16 and 0xFF).coerceIn(0, 255)
        val g = transform(argb shr 8 and 0xFF).coerceIn(0, 255)
        val b = transform(argb and 0xFF).coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
