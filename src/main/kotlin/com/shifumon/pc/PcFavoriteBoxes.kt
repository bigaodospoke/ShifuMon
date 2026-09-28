package com.shifumon.pc

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.client.gui.pc.PCGUI
import com.shifumon.ShifuMon
import com.shifumon.config.ConfigManager
import com.shifumon.hud.render.PixelUi
import com.shifumon.hud.render.RetroPalette
import com.shifumon.util.FeatureGuard
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.language.I18n
import net.minecraft.client.resources.sounds.SimpleSoundInstance

/**
 * Caixas salvas do PC: uma abinha com estrela no canto de cima abre a lista das caixas guardadas,
 * e clicar numa delas pula direto para lá.
 *
 * A aba fica fechada por padrão para não cobrir as setas de trocar de caixa, e a lista só aparece
 * enquanto está aberta. A escolha fica no cliente (`pc.favoriteBoxes`): vale em qualquer servidor
 * e ninguém mais enxerga.
 */
object PcFavoriteBoxes {
    /** Linha que age sobre a caixa aberta, em vez de pular para outra. */
    private const val CURRENT_BOX = -1

    private const val TAB_HEIGHT = 13
    private const val TAB_OFFSET_Y = 15
    private const val STAR_SIZE = 8
    private const val ROW_HEIGHT = 12
    private const val LIST_PADDING = 5
    private const val MIN_LIST_WIDTH = 86
    private const val MAX_LABEL_WIDTH = 96

    private val star = ShifuMon.id("textures/gui/shiny/sparkle.png")

    private var expanded = false

    private val favorites: MutableSet<Int>
        get() = ConfigManager.config.pc.favoriteBoxes

    fun register() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen !is PCGUI) return@register
            expanded = false
            ScreenEvents.afterRender(screen).register { _, graphics, mouseX, mouseY, _ ->
                FeatureGuard.guard("pc.favorites", Unit) { render(screen, graphics, mouseX, mouseY) }
            }
        }
    }

    private fun enabled(): Boolean = ConfigManager.config.pc.favoriteBoxesBar

    /** Linhas da lista: a primeira salva ou tira a caixa aberta, as outras são as caixas salvas. */
    private fun rows(screen: PCGUI): List<Int> =
        listOf(CURRENT_BOX) + favorites.sorted().filter { it < screen.pc.boxes.size }

    private fun tabBounds(screen: PCGUI): IntArray {
        val storage = screen.storage
        val width = STAR_SIZE + 8 + font().width(favorites.size.toString())
        return intArrayOf(storage.x, storage.y - TAB_OFFSET_Y, width, TAB_HEIGHT)
    }

    private fun listBounds(screen: PCGUI): IntArray {
        val tab = tabBounds(screen)
        val rows = rows(screen)
        val width = maxOf(MIN_LIST_WIDTH, rows.maxOf { font().width(label(screen, it)) } + LIST_PADDING * 2)
        val height = rows.size * ROW_HEIGHT + LIST_PADDING * 2
        return intArrayOf(tab[0], tab[1] + TAB_HEIGHT, width, height)
    }

    private fun render(screen: PCGUI, graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!enabled()) return
        val tab = tabBounds(screen)
        val hovered = inside(mouseX, mouseY, tab)
        PixelUi.box(graphics, tab[0], tab[1], tab[2], tab[3], if (expanded || hovered) RetroPalette.SHINY_DARK else RetroPalette.BODY)
        graphics.blit(star, tab[0] + 3, tab[1] + 3, STAR_SIZE, STAR_SIZE, 0f, 0f, 16, 16, 16, 16)
        graphics.drawString(font(), favorites.size.toString(), tab[0] + STAR_SIZE + 5, tab[1] + 3, RetroPalette.TEXT, true)
        if (!expanded) return

        // A lista cobre o quadro das caixas, então vem com moldura e fundo próprios. Os Pokémon dos
        // slots são desenhados numa camada alta, por isso a lista sobe junto para ficar na frente
        val list = listBounds(screen)
        graphics.pose().pushPose()
        graphics.pose().translate(0f, 0f, 400f)
        PixelUi.panel(graphics, list[0], list[1], list[2], list[3], RetroPalette.ACCENT_SEARCH)
        rows(screen).forEachIndexed { index, box ->
            val rowY = list[1] + LIST_PADDING + index * ROW_HEIGHT
            if (mouseX in list[0] until (list[0] + list[2]) && mouseY in rowY until (rowY + ROW_HEIGHT)) {
                graphics.fill(list[0] + 2, rowY - 1, list[0] + list[2] - 2, rowY + ROW_HEIGHT - 2, 0x40FFFFFF)
            }
            val color = when {
                box == CURRENT_BOX -> RetroPalette.LABEL
                box == screen.storage.box -> RetroPalette.ACCENT_SEARCH
                else -> RetroPalette.TEXT
            }
            graphics.drawString(font(), label(screen, box), list[0] + LIST_PADDING, rowY, color, true)
        }
        graphics.pose().popPose()
    }

    /** Chamado pelo mixin da tela do PC. `true` = o clique foi da abinha e a tela não recebe. */
    @JvmStatic
    fun click(screen: PCGUI, mouseX: Int, mouseY: Int): Boolean = FeatureGuard.guard("pc.favorites", false) {
        clickInternal(screen, mouseX, mouseY)
    }

    private fun clickInternal(screen: PCGUI, mouseX: Int, mouseY: Int): Boolean {
        if (!enabled()) return false
        if (inside(mouseX, mouseY, tabBounds(screen))) {
            expanded = !expanded
            playClick()
            return true
        }
        if (!expanded) return false

        val list = listBounds(screen)
        if (!inside(mouseX, mouseY, list)) {
            // Clique fora fecha a lista, e o clique segue valendo para a tela do Cobblemon
            expanded = false
            return false
        }
        val index = (mouseY - list[1] - LIST_PADDING) / ROW_HEIGHT
        val box = rows(screen).getOrNull(index) ?: return true
        if (box == CURRENT_BOX) toggle(screen.storage.box) else jumpTo(screen, box)
        playClick()
        return true
    }

    private fun toggle(box: Int) {
        if (!favorites.add(box)) favorites.remove(box)
        ConfigManager.save()
    }

    private fun jumpTo(screen: PCGUI, box: Int) {
        expanded = false
        val storage = screen.storage
        if (box == storage.box || box >= screen.pc.boxes.size) return
        storage.box = box
        storage.setupStorageSlots()
    }

    /**
     * Nome que o jogador deu à caixa; sem nome próprio, o número, como o PC mostra. Nomes longos
     * são cortados para a lista não crescer demais.
     */
    private fun label(screen: PCGUI, box: Int): String {
        if (box == CURRENT_BOX) {
            val key = if (screen.storage.box in favorites) "shifumon.pc.unsave_box" else "shifumon.pc.save_box"
            return I18n.get(key)
        }
        val name = screen.pc.boxes.getOrNull(box)?.name?.string?.trim().orEmpty()
        val text = name.ifEmpty { I18n.get("shifumon.pc.box_label", box + 1) }
        val font = font()
        if (font.width(text) <= MAX_LABEL_WIDTH) return text
        return font.plainSubstrByWidth(text, MAX_LABEL_WIDTH - font.width(".")) + "."
    }

    private fun inside(x: Int, y: Int, bounds: IntArray): Boolean =
        x >= bounds[0] && x < bounds[0] + bounds[2] && y >= bounds[1] && y < bounds[1] + bounds[3]

    private fun font() = Minecraft.getInstance().font

    private fun playClick() {
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.PC_CLICK, 1.0f))
    }
}
