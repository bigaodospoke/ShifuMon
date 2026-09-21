package com.shifumon.battle.log

import com.cobblemon.mod.common.api.moves.Moves
import com.shifumon.hud.render.TypeColors
import com.shifumon.util.Colors
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents

/** Classifica mensagens de batalha pela chave de tradução (independe do idioma). */
object BattleLogClassifier {
    private const val BATTLE = "cobblemon.battle."
    private const val STATUS = "cobblemon.status."
    private const val MOVE = "cobblemon.move."

    private val statusIds = mapOf(
        "poison" to "psn", "poisonbadly" to "tox", "paralysis" to "par",
        "sleep" to "slp", "frozen" to "frz", "freeze" to "frz", "burn" to "brn",
    )

    fun classify(message: Component): LogEntry {
        val contents = findTranslatable(message) ?: return LogEntry(LogKind.OTHER, message)
        val key = contents.key
        val args = contents.args
        val firstArg = args.firstOrNull() as? Component
        val spreadTarget = if (key.endsWith("_spread")) firstArg else null

        return when {
            key == "${BATTLE}turn" -> LogEntry(LogKind.TURN, message, turn = asInt(args.firstOrNull()) ?: 0)
            key == "${BATTLE}used_move" || key == "${BATTLE}used_move_on" -> moveEntry(message, key, args)
            key.startsWith("${BATTLE}superEffective") -> LogEntry(LogKind.SUPER_EFFECTIVE, message, target = spreadTarget)
            key.startsWith("${BATTLE}resisted") -> LogEntry(LogKind.NOT_EFFECTIVE, message, target = spreadTarget)
            key == "${BATTLE}immune" -> LogEntry(LogKind.IMMUNE, message, target = firstArg)
            key.startsWith("${BATTLE}crit") -> LogEntry(LogKind.CRITICAL, message, target = spreadTarget)
            key == "${BATTLE}missed" -> LogEntry(LogKind.MISS, message)
            key == "${BATTLE}fainted" -> LogEntry(LogKind.FAINT, message)
            key.startsWith("${BATTLE}switch.") || key == "${BATTLE}dragged_out" -> LogEntry(LogKind.SWITCH_IN, message)
            key.startsWith("${BATTLE}withdraw.") -> LogEntry(LogKind.SWITCH_OUT, message)
            key.startsWith("${BATTLE}boost.") || key.startsWith("${BATTLE}setboost.") -> LogEntry(LogKind.BOOST, message)
            key.startsWith("${BATTLE}unboost.") -> LogEntry(LogKind.UNBOOST, message)
            key.startsWith(STATUS) && key.endsWith(".apply") ->
                LogEntry(LogKind.STATUS, message, detail = statusIds[key.removePrefix(STATUS).substringBefore('.')])
            key.startsWith("${BATTLE}heal") -> LogEntry(LogKind.HEAL, message)
            key.startsWith("${BATTLE}damage") || (key.startsWith(STATUS) && key.endsWith(".hurt")) -> LogEntry(LogKind.DAMAGE, message)
            key.startsWith("${BATTLE}weather.") ->
                LogEntry(LogKind.WEATHER, message, detail = key.removePrefix("${BATTLE}weather.").substringBefore('.'))
            key.startsWith("${BATTLE}fieldstart.") && key.endsWith("terrain") ->
                LogEntry(LogKind.TERRAIN, message, detail = key.removePrefix("${BATTLE}fieldstart."))
            key == "${BATTLE}win" || key == "${BATTLE}lose" || key == "${BATTLE}flee" -> LogEntry(LogKind.RESULT, message)
            else -> LogEntry(LogKind.OTHER, message)
        }
    }

    /** Recria "X usou Golpe!" com o nome do golpe na cor do tipo dele. */
    private fun moveEntry(original: Component, key: String, args: Array<Any>): LogEntry {
        val move = args.getOrNull(1) as? Component ?: return LogEntry(LogKind.MOVE, original)
        val moveId = findTranslatable(move)?.key?.takeIf { it.startsWith(MOVE) }?.removePrefix(MOVE)
        val template = moveId?.let { Moves.getByName(it) } ?: return LogEntry(LogKind.MOVE, original)
        val color = Colors.lighten(TypeColors.of(template.elementalType), 0.2f)

        val recolored = args.copyOf()
        recolored[1] = move.copy().withColor(color and 0xFFFFFF)
        return LogEntry(LogKind.MOVE, Component.translatable(key, *recolored), color = color)
    }

    private fun findTranslatable(component: Component): TranslatableContents? {
        val contents = component.contents
        if (contents is TranslatableContents && contents.key.startsWith("cobblemon.")) return contents
        for (sibling in component.siblings) findTranslatable(sibling)?.let { return it }
        return null
    }

    private fun asInt(arg: Any?): Int? = when (arg) {
        is Number -> arg.toInt()
        is Component -> arg.string.trim().toIntOrNull()
        null -> null
        else -> arg.toString().trim().toIntOrNull()
    }
}
