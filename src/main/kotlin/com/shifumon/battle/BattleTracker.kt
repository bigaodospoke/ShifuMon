package com.shifumon.battle

import com.cobblemon.mod.common.client.CobblemonClient
import com.shifumon.battle.log.BattleLog
import com.shifumon.util.FeatureGuard
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import java.util.UUID

/**
 * Estado do campo de batalha (turno, clima, terreno, salas e condições de cada lado) reconstruído
 * a partir das mensagens que o servidor já envia. O Cobblemon não expõe esses dados no cliente, mas
 * as mensagens chegam como chaves de tradução estáveis, independentes do idioma do jogador.
 */
object BattleTracker {
    /** Clima, terreno, sala ou condição de um lado do campo, com a conta de turnos. */
    class TimedEffect(
        val id: String,
        /** Quantos turnos dura (mínimo..máximo, quando um item pode estender); `null` = não acaba sozinho. */
        private val duration: IntRange?,
        private val startTurn: Int,
        /** Começou depois do fim de turno (antes do turno 1 ou numa troca após desmaio): esse turno não conta. */
        private val afterResidual: Boolean,
        /** O clima avisa a cada fim de turno que continua; contar essas mensagens é exato. */
        private val countsUpkeep: Boolean,
    ) {
        var upkeeps = 0
        var layers = 1

        /** Turnos restantes, contando o atual; `null` se o efeito é permanente ou a conta se perdeu. */
        fun remaining(currentTurn: Int): IntRange? {
            val duration = duration ?: return null
            val passed = if (countsUpkeep) upkeeps else (currentTurn - startTurn - if (afterResidual) 1 else 0).coerceAtLeast(0)
            val max = duration.last - passed
            if (max <= 0) return null
            // Passou da duração normal e o efeito continua: foi estendido por item, só sobra a conta longa
            val min = (duration.first - passed).takeIf { it > 0 } ?: max
            return min..max
        }
    }

    class FieldState(val battleId: UUID?) {
        var turn = 0
        var weather: TimedEffect? = null
        var terrain: TimedEffect? = null
        val fieldEffects: MutableMap<String, TimedEffect> = LinkedHashMap()
        val allySide: MutableMap<String, TimedEffect> = LinkedHashMap()
        val opponentSide: MutableMap<String, TimedEffect> = LinkedHashMap()

        /** Trocas após desmaio acontecem depois do fim de turno. */
        internal var faintedThisTurn = false
        /** Nomes da última mensagem de golpe, habilidade ou troca: quem criou o próximo efeito. */
        internal var lastActor: List<String> = emptyList()
    }

    private const val BATTLE = "cobblemon.battle."
    private const val TURN_KEY = "${BATTLE}turn"
    private const val WEATHER_PREFIX = "${BATTLE}weather."
    private const val FIELD_START_PREFIX = "${BATTLE}fieldstart."
    private const val FIELD_END_PREFIX = "${BATTLE}fieldend."
    private const val SIDE_START_PREFIX = "${BATTLE}sidestart."
    private const val SIDE_END_PREFIX = "${BATTLE}sideend."

    private var state = FieldState(null)

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register {
            stateFor(FeatureGuard.guard("battle.tracker", null) { CobblemonClient.battle?.battleId })
        }
    }

    /** Estado da batalha informada; reinicia sozinho quando a batalha muda ou termina. */
    fun stateFor(battleId: UUID?): FieldState {
        if (state.battleId != battleId) state = FieldState(battleId)
        return state
    }

    /** Chamado pelo mixin em `BattleMessageHandler.handle`, antes da formatação das mensagens. */
    @JvmStatic
    fun onBattleMessages(messages: List<Component>) {
        val client = Minecraft.getInstance()
        if (!client.isSameThread) {
            val copy = messages.toList()
            client.execute { onBattleMessages(copy) }
            return
        }
        FeatureGuard.guard("battle.messages", Unit) {
            val current = stateFor(CobblemonClient.battle?.battleId)
            messages.forEach { visit(it, current) }
        }
        FeatureGuard.guard("battle.log.record", Unit) {
            BattleLog.record(CobblemonClient.battle?.battleId, messages)
        }
        FeatureGuard.guard("battle.boosts", Unit) {
            BattleBoostTracker.record(CobblemonClient.battle?.battleId, messages)
        }
    }

    private fun visit(component: Component, state: FieldState) {
        val contents = component.contents
        if (contents is TranslatableContents) {
            apply(contents.key, contents.args, state)
            contents.args.forEach { if (it is Component) visit(it, state) }
        }
        component.siblings.forEach { visit(it, state) }
    }

    private fun apply(key: String, args: Array<Any>, state: FieldState) {
        when {
            key == TURN_KEY -> args.firstOrNull()?.let(::asInt)?.let {
                state.turn = it
                state.faintedThisTurn = false
                state.lastActor = emptyList()
            }

            key == "${BATTLE}fainted" -> state.faintedThisTurn = true

            // Golpes criam salas e telas; habilidades de entrada (Drizzle, Surges) criam clima e terreno
            key == "${BATTLE}used_move" || key == "${BATTLE}used_move_on" || key == "${BATTLE}ability.generic" ||
                key.startsWith("${BATTLE}switch.") || key == "${BATTLE}dragged_out" ->
                state.lastActor = args.mapNotNull { (it as? Component)?.string }

            // cobblemon.battle.weather.<clima>.<start|upkeep|end>
            key.startsWith(WEATHER_PREFIX) -> {
                val parts = key.removePrefix(WEATHER_PREFIX).split('.')
                val weather = parts.first()
                when (parts.getOrNull(1)) {
                    "end" -> if (state.weather?.id == weather) state.weather = null
                    "upkeep" -> {
                        val current = state.weather
                        if (current?.id == weather) {
                            current.upkeeps++
                        } else {
                            // Entrou no meio da batalha: sem o início, não dá para contar os turnos
                            state.weather = TimedEffect(weather, null, state.turn, false, true)
                        }
                    }
                    else -> state.weather = start(weather, EffectDurations.weather(weather), state, countsUpkeep = true)
                }
            }

            key.startsWith(FIELD_START_PREFIX) -> {
                val effect = key.removePrefix(FIELD_START_PREFIX).substringBefore('.')
                if (effect.endsWith("terrain")) {
                    state.terrain = start(effect, EffectDurations.terrain(), state)
                } else {
                    state.fieldEffects[effect] = start(effect, EffectDurations.field(effect), state)
                }
            }

            key.startsWith(FIELD_END_PREFIX) -> {
                val effect = key.removePrefix(FIELD_END_PREFIX).substringBefore('.')
                if (state.terrain?.id == effect) state.terrain = null
                state.fieldEffects.remove(effect)
            }

            // cobblemon.battle.sidestart.<ally|opponent>.<efeito>, do ponto de vista de quem recebe
            key.startsWith(SIDE_START_PREFIX) -> {
                val (side, effect) = sideAndEffect(key.removePrefix(SIDE_START_PREFIX)) ?: return
                val effects = if (side == "ally") state.allySide else state.opponentSide
                val existing = effects[effect]
                if (existing != null && effect in EffectDurations.LAYERED) {
                    existing.layers = (existing.layers + 1).coerceAtMost(EffectDurations.maxLayers(effect))
                } else {
                    effects[effect] = start(effect, EffectDurations.side(effect), state)
                }
            }

            key.startsWith(SIDE_END_PREFIX) -> {
                val (side, effect) = sideAndEffect(key.removePrefix(SIDE_END_PREFIX)) ?: return
                (if (side == "ally") state.allySide else state.opponentSide).remove(effect)
            }
        }
    }

    private fun start(id: String, duration: EffectDurations.Duration?, state: FieldState, countsUpkeep: Boolean = false): TimedEffect {
        val turns = duration?.let { EffectDurations.resolve(it, state.lastActor) }
        return TimedEffect(id, turns, state.turn, state.turn == 0 || state.faintedThisTurn, countsUpkeep)
    }

    private fun sideAndEffect(rest: String): Pair<String, String>? {
        val parts = rest.split('.')
        return if (parts.size >= 2) parts[0] to parts[1] else null
    }

    private fun asInt(arg: Any): Int? = when (arg) {
        is Number -> arg.toInt()
        is Component -> arg.string.trim().toIntOrNull()
        else -> arg.toString().trim().toIntOrNull()
    }
}
