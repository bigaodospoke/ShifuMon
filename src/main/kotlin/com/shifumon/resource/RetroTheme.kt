package com.shifumon.resource

import com.mojang.blaze3d.platform.NativeImage
import com.shifumon.util.FeatureGuard
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.IoSupplier
import java.io.InputStream
import java.nio.file.Files
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * Texturas do pacote "ShifuMon Retro": HUD, inventário, widgets e botões de batalha do Cobblemon
 * repintados na paleta do mod.
 *
 * As texturas de origem são as originais do Minecraft e do Cobblemon instalados, repintadas em
 * memória; nenhuma textura de terceiros vem no jar. A geometria é preservada (os espaços de item
 * continuam no mesmo lugar) e só as cores mudam: cinzas viram a paleta escura do ShifuMon e cores
 * vivas (corações, comida, ícones) são mantidas para não perder legibilidade.
 */
internal object RetroTheme {
    private const val FEATURE = "resourcepack.retro"
    private const val MINECRAFT = "minecraft"
    private const val COBBLEMON = "cobblemon"

    private enum class Style {
        /** Cinza vira a paleta escura; cores vivas seguem quase iguais. */
        SLATE,
        /** Tudo vira dourado (seleção da hotbar). */
        GOLD,
        /** Mantém as cores, só escurece o contorno preto. */
        KEEP_COLORS,
    }

    /** Tons escuros do ShifuMon, do contorno ao brilho. */
    private val slate = intArrayOf(
        0xFF07070C.toInt(), 0xFF0F0F1A.toInt(), 0xFF16162A.toInt(), 0xFF1C1C2E.toInt(),
        0xFF2E2E44.toInt(), 0xFF3E3E5E.toInt(), 0xFF55557E.toInt(),
    )

    /** Dourado para a seleção da hotbar. */
    private val gold = intArrayOf(
        0xFF2A1F00.toInt(), 0xFF5A4200.toInt(), 0xFF8C6A08.toInt(), 0xFFC79A10.toInt(),
        0xFFE8BE3A.toInt(), 0xFFF8D878.toInt(), 0xFFFFF0B4.toInt(),
    )

    private val luminanceLimits = intArrayOf(24, 64, 110, 150, 190, 225)

    // Montado na primeira recarga de recursos, quando o pacote vanilla já existe
    private val targets: Map<ResourceLocation, Style> by lazy { FeatureGuard.guard(FEATURE, emptyMap(), ::buildTargets) }
    private val cache = ConcurrentHashMap<ResourceLocation, Optional<ByteArray>>()

    fun namespaces(): Set<String> = setOf(MINECRAFT, COBBLEMON)

    fun locations(): Set<ResourceLocation> = targets.keys

    /** PNG repintado, ou `null` se a textura não faz parte do pacote ou a original não existe. */
    fun texture(location: ResourceLocation): ByteArray? {
        val style = targets[location] ?: return null
        return cache.computeIfAbsent(location) { Optional.ofNullable(repaint(it, style)) }.orElse(null)
    }

    private fun buildTargets(): Map<ResourceLocation, Style> {
        val targets = LinkedHashMap<ResourceLocation, Style>()
        fun add(namespace: String, path: String, style: Style) {
            targets[ResourceLocation.fromNamespaceAndPath(namespace, path)] = style
        }

        // Telas: inventário, mesa de trabalho e baú
        for (container in listOf("inventory", "crafting_table", "generic_54")) {
            add(MINECRAFT, "textures/gui/container/$container.png", Style.SLATE)
        }
        // HUD
        for (hud in listOf(
            "hotbar", "hotbar_offhand_left", "hotbar_offhand_right",
            "hotbar_attack_indicator_background", "hotbar_attack_indicator_progress",
            "experience_bar_background", "jump_bar_background",
            "effect_background", "effect_background_ambient",
        )) {
            add(MINECRAFT, "textures/gui/sprites/hud/$hud.png", Style.SLATE)
        }
        add(MINECRAFT, "textures/gui/sprites/hud/hotbar_selection.png", Style.GOLD)
        for (colored in listOf(
            "experience_bar_progress", "jump_bar_progress", "jump_bar_cooldown",
            "air", "air_bursting", "armor_empty", "armor_half", "armor_full",
            "food_empty", "food_half", "food_full",
            "food_empty_hunger", "food_half_hunger", "food_full_hunger",
        )) {
            add(MINECRAFT, "textures/gui/sprites/hud/$colored.png", Style.KEEP_COLORS)
        }
        // Corações: mantêm o vermelho, contorno mais escuro
        Minecraft.getInstance().vanillaPackResources.listResources(
            PackType.CLIENT_RESOURCES, MINECRAFT, "textures/gui/sprites/hud/heart",
        ) { location, _ -> if (location.path.endsWith(".png")) targets[location] = Style.KEEP_COLORS }

        // Botões, campos de texto e abas (também usados pela tela de config do mod)
        for (widget in listOf(
            "button", "button_disabled", "button_highlighted", "text_field", "text_field_highlighted",
            "slider", "slider_highlighted", "slider_handle", "slider_handle_highlighted",
            "scroller", "scroller_background", "slot_frame",
            "tab", "tab_highlighted", "tab_selected", "tab_selected_highlighted",
            "checkbox", "checkbox_highlighted", "checkbox_selected", "checkbox_selected_highlighted",
            "cross_button", "cross_button_highlighted", "page_forward", "page_forward_highlighted",
            "page_backward", "page_backward_highlighted",
        )) {
            add(MINECRAFT, "textures/gui/sprites/widget/$widget.png", Style.SLATE)
        }

        // Botões de batalha do Cobblemon (o texto é desenhado pelo Cobblemon, a textura é só a moldura)
        for (battle in listOf(
            "battle_menu_fight", "battle_menu_bag", "battle_menu_switch", "battle_menu_run", "battle_menu_forfeit",
            "battle_back", "party_select", "party_select_disabled", "target_select", "target_select_disabled",
        )) {
            add(COBBLEMON, "textures/gui/battle/$battle.png", Style.SLATE)
        }
        return targets
    }

    private fun repaint(location: ResourceLocation, style: Style): ByteArray? = FeatureGuard.guard(FEATURE, null) {
        val original = original(location) ?: return@guard null
        original.get().use { input ->
            NativeImage.read(input).use { image ->
                for (y in 0 until image.height) {
                    for (x in 0 until image.width) {
                        // NativeImage guarda os pixels em ABGR; trocar R e B converte nos dois sentidos
                        val argb = swapRedBlue(image.getPixelRGBA(x, y))
                        image.setPixelRGBA(x, y, swapRedBlue(convert(argb, style)))
                    }
                }
                image.asByteArray()
            }
        }
    }

    /** Textura original: a do pacote vanilla ou a de dentro do jar do Cobblemon. */
    private fun original(location: ResourceLocation): IoSupplier<InputStream>? = when (location.namespace) {
        MINECRAFT -> Minecraft.getInstance().vanillaPackResources.getResource(PackType.CLIENT_RESOURCES, location)
        else -> FabricLoader.getInstance().getModContainer(location.namespace)
            .flatMap { it.findPath("${PackType.CLIENT_RESOURCES.directory}/${location.namespace}/${location.path}") }
            .filter(Files::isRegularFile)
            .map { path -> IoSupplier { Files.newInputStream(path) } }
            .orElse(null)
    }

    private fun convert(argb: Int, style: Style): Int {
        val alpha = argb ushr 24
        if (alpha == 0) return 0

        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val saturation = if (max == 0) 0f else (max - min).toFloat() / max
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

        if (style == Style.KEEP_COLORS) {
            // só o preto do contorno vira o tom escuro do mod
            return if (luminance < 24) withAlpha(slate[0], alpha) else argb
        }
        if (style == Style.SLATE) {
            // ícones coloridos (espada, poké bola...) ficam como estão; tons escuros levemente
            // tingidos, como o fundo da hotbar, entram na repintura
            if (saturation >= 0.35f && max >= 100) return argb
            val exact = exactSlate(argb and 0xFFFFFF)
            if (exact != 0) return withAlpha(exact, alpha)
        }
        return withAlpha(ramp(if (style == Style.GOLD) gold else slate, luminance), alpha)
    }

    /** Cinzas exatos das telas do Minecraft, para o painel e os espaços de item ficarem iguais ao HUD do mod. */
    private fun exactSlate(rgb: Int): Int = when (rgb) {
        0xFFFFFF -> 0xFF55557E.toInt() // brilho superior
        0xC6C6C6, 0xB0B0B0 -> 0xFF2E2E44.toInt() // corpo do painel / botão
        0x8B8B8B -> 0xFF1C1C2C.toInt() // fundo do espaço de item
        0x555555 -> 0xFF12121F.toInt() // sombra
        0x373737 -> 0xFF0D0D17.toInt() // sombra do espaço
        0x000000 -> 0xFF07070C.toInt() // contorno
        else -> 0
    }

    /** Escolhe o tom da paleta conforme o brilho do pixel original. */
    private fun ramp(palette: IntArray, luminance: Int): Int {
        val index = luminanceLimits.indexOfFirst { luminance < it }
        return palette[if (index < 0) palette.lastIndex else index]
    }

    private fun withAlpha(color: Int, alpha: Int): Int = (alpha shl 24) or (color and 0xFFFFFF)

    private fun swapRedBlue(color: Int): Int =
        (color and 0xFF00FF00.toInt()) or ((color and 0xFF) shl 16) or ((color shr 16) and 0xFF)
}
