package com.shifumon.pc

import com.shifumon.ShifuMon
import com.shifumon.config.ConfigManager
import net.minecraft.resources.ResourceLocation

/**
 * Papel de parede do ShifuMon no PC, como UMA opção a mais na lista — e não no lugar das outras.
 *
 * Antes o mod trocava a textura de qualquer papel de parede na hora de desenhar: todas as caixas
 * viravam a logo e escolher outro papel não adiantava nada.
 *
 * O papel de parede de cada caixa é dado do servidor — a lista de opções vem de lá e a escolha volta
 * por pacote, que o servidor valida. Sendo o mod client-side, não dá para registrar um papel novo lá,
 * então a escolha do ShifuMon fica guardada aqui mesmo, caixa por caixa (`pc.shifumonWallpaperBoxes`
 * na configuração), e a textura só é trocada ao desenhar uma caixa marcada. Escolher qualquer papel
 * de verdade tira a marca e devolve a caixa ao caminho normal do Cobblemon.
 *
 * Como a marca é do cliente, ela vale para você em qualquer servidor e ninguém mais a enxerga.
 */
object PcWallpaper {
    private const val WALLPAPER_PREFIX = "textures/gui/pc/wallpaper/"
    private const val GLOW = "/glow/"

    /** Textura do papel de parede e, ao mesmo tempo, a identidade do item na lista do PC. */
    @JvmStatic
    val wallpaper: ResourceLocation = ShifuMon.id("textures/gui/pc/wallpaper_shifumon.png")

    // A logo já tem brilho próprio; a camada de brilho do papel de parede original sai de cena
    private val empty = ShifuMon.id("textures/gui/pc/empty.png")

    private var renderingBoxes = false

    /**
     * Última caixa desenhada, que é também a caixa em que a escolha da lista se aplica: a tela do PC
     * desenha o quadro das caixas atrás do seletor, então esse número está sempre atualizado.
     */
    private var currentBox = -1

    private val markedBoxes: MutableSet<Int>
        get() = ConfigManager.config.pc.shifumonWallpaperBoxes

    /** Se o item do ShifuMon aparece na lista de papéis de parede. */
    @JvmStatic
    fun inList(): Boolean = ConfigManager.config.pc.shifumonWallpaper

    @JvmStatic
    fun isShifuMon(texture: ResourceLocation): Boolean = texture == wallpaper

    /** Chamado pelo mixin no início (com a caixa) e no fim (com -1) do desenho do quadro. */
    @JvmStatic
    fun renderingBox(box: Int) {
        renderingBoxes = box >= 0
        if (box >= 0) currentBox = box
    }

    @JvmStatic
    fun currentBox(): Int = currentBox

    /** Marca a caixa para usar o papel de parede do ShifuMon. */
    @JvmStatic
    fun choose(box: Int) {
        if (box < 0 || !markedBoxes.add(box)) return
        ConfigManager.save()
    }

    /** Tira a marca: a caixa volta ao papel de parede que o servidor guarda. */
    @JvmStatic
    fun clear(box: Int) {
        if (box < 0 || !markedBoxes.remove(box)) return
        ConfigManager.save()
    }

    @JvmStatic
    fun isMarked(box: Int): Boolean = inList() && box in markedBoxes

    /** Chamado pelo mixin do `blitk`, junto com a troca do ícone shiny. */
    @JvmStatic
    fun resolveTexture(texture: ResourceLocation): ResourceLocation {
        if (!renderingBoxes || !isMarked(currentBox)) return texture
        if (texture.namespace != "cobblemon" || !texture.path.startsWith(WALLPAPER_PREFIX)) return texture
        return if (GLOW in texture.path) empty else wallpaper
    }
}
