package com.shifumon.hud.render

import com.shifumon.util.Colors
import net.minecraft.client.gui.GuiGraphics
import java.text.Normalizer
import java.util.Locale

/** Fonte pixel 3x5 para rótulos pequenos (HP, PP, stats, status), no estilo das HUDs de GBA. */
object TinyFont {
    const val HEIGHT = 5
    private const val GLYPH_WIDTH = 3
    private const val SPACING = 1

    private val diacritics = Regex("\\p{M}+")

    /** Cada glifo: 5 linhas de 3 bits, de cima para baixo. */
    private val glyphs: Map<Char, Array<BooleanArray>> = mapOf(
        '0' to "111101101101111", '1' to "010110010010111", '2' to "111001111100111",
        '3' to "111001111001111", '4' to "101101111001001", '5' to "111100111001111",
        '6' to "111100111101111", '7' to "111001001001001", '8' to "111101111101111",
        '9' to "111101111001111",
        'A' to "010101111101101", 'B' to "110101110101110", 'C' to "011100100100011",
        'D' to "110101101101110", 'E' to "111100110100111", 'F' to "111100110100100",
        'G' to "011100101101011", 'H' to "101101111101101", 'I' to "111010010010111",
        'J' to "001001001101010", 'K' to "101101110101101", 'L' to "100100100100111",
        'M' to "101111111101101", 'N' to "110101101101101", 'O' to "010101101101010",
        'P' to "110101110100100", 'Q' to "010101101110011", 'R' to "110101110101101",
        'S' to "011100010001110", 'T' to "111010010010010", 'U' to "101101101101111",
        'V' to "101101101101010", 'W' to "101101111111101", 'X' to "101101010101101",
        'Y' to "101101010010010", 'Z' to "111001010100111",
        '+' to "000010111010000", '-' to "000000111000000", '/' to "001001010100100",
        '%' to "101001010100101", '.' to "000000000000010", ':' to "000010000010000",
        '>' to "100010001010100", '<' to "001010100010001", '!' to "010010010000010",
        '?' to "111001010000010", ' ' to "000000000000000",
    ).mapValues { (_, bits) ->
        Array(HEIGHT) { row -> BooleanArray(GLYPH_WIDTH) { column -> bits[row * GLYPH_WIDTH + column] == '1' } }
    }

    /** Converte para o conjunto suportado: maiúsculas, sem acento, desconhecidos viram '?'. */
    fun sanitize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .uppercase(Locale.ROOT)
            .map { if (it in glyphs) it else '?' }
            .joinToString("")

    fun width(text: String): Int = if (text.isEmpty()) 0 else text.length * (GLYPH_WIDTH + SPACING) - SPACING

    fun draw(graphics: GuiGraphics, text: String, x: Int, y: Int, color: Int, shadow: Boolean = false) {
        if (shadow) drawGlyphs(graphics, text, x + 1, y + 1, Colors.darken(color, 0.25f))
        drawGlyphs(graphics, text, x, y, color)
    }

    private fun drawGlyphs(graphics: GuiGraphics, text: String, x: Int, y: Int, color: Int) {
        var cursorX = x
        for (char in text) {
            val glyph = glyphs[char] ?: glyphs.getValue('?')
            for (row in 0 until HEIGHT) {
                // Agrupa pixels vizinhos da mesma linha num único retângulo
                var column = 0
                while (column < GLYPH_WIDTH) {
                    if (!glyph[row][column]) {
                        column++
                        continue
                    }
                    val start = column
                    while (column < GLYPH_WIDTH && glyph[row][column]) column++
                    graphics.fill(cursorX + start, y + row, cursorX + column, y + row + 1, color)
                }
            }
            cursorX += GLYPH_WIDTH + SPACING
        }
    }
}
