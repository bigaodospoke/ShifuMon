package com.shifumon.config

import com.shifumon.shiny.ShinyStyle

/**
 * Modelo da configuração, salvo em `config/shifumon/shifumon.json`.
 *
 * Todo campo tem valor padrão: quando o mod ganhar opções novas, configs antigas continuam
 * válidas e as chaves novas aparecem sozinhas no próximo save.
 */
class ShifuMonConfig {
    var configVersion: Int = CURRENT_VERSION
    var shiny = ShinyConfig()
    var pokemonInfo = PokemonInfoConfig()
    var interfaceTweaks = InterfaceConfig()
    var battleHud = BattleHudConfig()
    var boxSearch = BoxSearchConfig()
    var pc = PcConfig()
    var hud = HudLayoutConfig()

    /** O Gson ignora a nulabilidade do Kotlin; corrige campos ausentes ou inválidos após carregar. */
    @Suppress("SENSELESS_COMPARISON")
    fun sanitize() {
        if (shiny == null) shiny = ShinyConfig()
        if (pokemonInfo == null) pokemonInfo = PokemonInfoConfig()
        if (interfaceTweaks == null) interfaceTweaks = InterfaceConfig()
        if (battleHud == null) battleHud = BattleHudConfig()
        if (boxSearch == null) boxSearch = BoxSearchConfig()
        if (pc == null) pc = PcConfig()
        if (hud == null) hud = HudLayoutConfig()
        pc.minPerfectIvs = pc.minPerfectIvs.coerceIn(1, 6)
        if (pc.shifumonWallpaperBoxes == null) pc.shifumonWallpaperBoxes = LinkedHashSet()
        pc.shifumonWallpaperBoxes.removeIf { it == null || it < 0 }

        if (shiny.style == null) shiny.style = ShinyStyle.STAR
        if (shiny.customIconFile.isNullOrBlank()) shiny.customIconFile = ShinyConfig.DEFAULT_CUSTOM_ICON
        pokemonInfo.maxDistance = pokemonInfo.maxDistance.coerceIn(4, 48)

        if (hud.positions == null) hud.positions = LinkedHashMap()
        hud.positions.values.removeIf { it == null }
        hud.positions.values.forEach { if (it.anchor == null) it.anchor = HudAnchor.TOP_LEFT }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}

class ShinyConfig {
    var enabled = true
    var style = ShinyStyle.STAR
    var customIconFile = DEFAULT_CUSTOM_ICON

    companion object {
        const val DEFAULT_CUSTOM_ICON = "custom_shiny.png"
    }
}

class PokemonInfoConfig {
    var enabled = true
    var requireSneak = false
    var maxDistance = 12
    var showLevel = true
    var showShiny = true
    var showGender = true
    var showTypes = true
    var showNature = true
    var showAbility = true
    var showHiddenAbility = true
    var showSize = true
    var showWeight = true
    var showForm = true
    /** Chance estimada de captura com a Poké Bola na mão. */
    var showCaptureChance = true
}

class InterfaceConfig {
    var enabled = true
    /** Desenha o painel do ShifuMon no lugar do bloco de HP do Cobblemon (senão, painel flutuante). */
    var replaceBattleTiles = true
    /** Se um pacote de recursos retexturiza bloco de HP, histórico ou botões de golpe, mantém o do pacote. */
    var respectTexturePacks = true
    var enhancedHpBar = true
    var showHpNumbers = true
    var showTypes = true
    var highlightStatus = true
    var showStatBoosts = true
    var showWeaknesses = true
    var showResistances = true
    var showStrengths = true
    /** Atributos atuais (exatos nos seus Pokémon, faixa estimada nos do oponente). */
    var showCurrentStats = true
}

class BattleHudConfig {
    /** Painel de turno/clima/terreno. */
    var enabled = true
    var showTurnCounter = true
    var showWeather = true
    var showTerrain = true
    var showFieldEffects = true
    /** Reflect, Tailwind, Spikes... de cada lado do campo. */
    var showSideConditions = true
    /** Quantos turnos faltam para cada clima, terreno e efeito acabar. */
    var showEffectTurns = true
    /** Stats base e habilidades possíveis no painel do oponente. */
    var showCompetitiveInfo = true
    var restyleMoveButtons = true
    var showMoveEffectiveness = true
    /** Poder, precisão, PP e descrição ao passar o mouse num golpe. */
    var showMoveTooltip = true
    var restyleBattleLog = true
}

class BoxSearchConfig {
    var enabled = true
    var showResultsPanel = true
}

class PcConfig {
    var showShinyIcon = true
    var showIvBadge = true
    /** A partir de quantos IVs máximos o selo aparece (6 = só F6). */
    var minPerfectIvs = 5
    /** Oferece o papel de parede do ShifuMon como mais uma opção na lista do PC. */
    var shifumonWallpaper = true
    /** Caixas que estão usando o papel de parede do ShifuMon, escolhido na lista do PC.
     *  Fica no cliente porque o servidor só conhece os papéis de parede dele. */
    var shifumonWallpaperBoxes: MutableSet<Int> = LinkedHashSet()
}

class HudLayoutConfig {
    /** id do elemento -> posição. Ausente = posição padrão do elemento. */
    var positions: MutableMap<String, HudPosition> = LinkedHashMap()
}
