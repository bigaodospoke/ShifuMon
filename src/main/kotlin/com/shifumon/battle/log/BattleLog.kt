package com.shifumon.battle.log

import net.minecraft.network.chat.Component
import java.util.UUID

enum class LogKind {
    TURN, MOVE, SUPER_EFFECTIVE, NOT_EFFECTIVE, IMMUNE, CRITICAL, MISS, FAINT,
    SWITCH_IN, SWITCH_OUT, BOOST, UNBOOST, STATUS, HEAL, DAMAGE, WEATHER, TERRAIN, RESULT, OTHER,
}

/**
 * Uma mensagem do histórico, já classificada.
 *
 * @param text mensagem original (em golpes, reconstruída com o nome do golpe na cor do tipo)
 * @param target alvo das mensagens curtas de área (crítico/eficácia em vários Pokémon)
 * @param color cor do tipo do golpe
 * @param detail id auxiliar: status do Showdown (psn, par...), clima ou terreno
 */
class LogEntry(
    val kind: LogKind,
    val text: Component,
    val target: Component? = null,
    val color: Int? = null,
    val detail: String? = null,
    val turn: Int = 0,
)

/** Histórico da batalha atual, alimentado pelo mixin em `BattleMessageHandler`. */
object BattleLog {
    private const val MAX_ENTRIES = 400

    private var battleId: UUID? = null
    private val entries = ArrayList<LogEntry>()

    /** Muda a cada alteração; o renderizador usa para saber quando refazer as linhas. */
    var revision = 0
        private set

    fun entries(battleId: UUID?): List<LogEntry> {
        sync(battleId)
        return entries
    }

    fun record(battleId: UUID?, messages: List<Component>) {
        sync(battleId)
        messages.forEach { entries += BattleLogClassifier.classify(it) }
        if (entries.size > MAX_ENTRIES) entries.subList(0, entries.size - MAX_ENTRIES).clear()
        revision++
    }

    private fun sync(battleId: UUID?) {
        if (battleId == this.battleId) return
        this.battleId = battleId
        entries.clear()
        revision++
    }
}
