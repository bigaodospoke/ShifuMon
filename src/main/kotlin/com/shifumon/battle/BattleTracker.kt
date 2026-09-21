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
 * Estado do campo de batalha (turno, clima, terreno, salas) reconstruído a partir das mensagens
 * que o servidor já envia. O Cobblemon não expõe esses dados no cliente, mas as mensagens chegam
 * como chaves de tradução estáveis, independentes do idioma do jogador.
 */
object BattleTracker {
    class FieldState(val battleId: UUID?) {
        var turn = 0
        var weather: String? = null
        var terrain: String? = null
        val fieldEffects: MutableSet<String> = LinkedHashSet()
    }

    private const val TURN_KEY = "cobblemon.battle.turn"
    private const val WEATHER_PREFIX = "cobblemon.battle.weather."
    private const val FIELD_START_PREFIX = "cobblemon.battle.fieldstart."
    private const val FIELD_END_PREFIX = "cobblemon.battle.fieldend."

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
            key == TURN_KEY -> args.firstOrNull()?.let(::asInt)?.let { state.turn = it }

            // cobblemon.battle.weather.<clima>.<start|upkeep|end>
            key.startsWith(WEATHER_PREFIX) -> {
                val parts = key.removePrefix(WEATHER_PREFIX).split('.')
                val weather = parts.first()
                if (parts.getOrNull(1) == "end") {
                    if (state.weather == weather) state.weather = null
                } else {
                    state.weather = weather
                }
            }

            key.startsWith(FIELD_START_PREFIX) -> {
                val effect = key.removePrefix(FIELD_START_PREFIX).substringBefore('.')
                if (effect.endsWith("terrain")) state.terrain = effect else state.fieldEffects += effect
            }

            key.startsWith(FIELD_END_PREFIX) -> {
                val effect = key.removePrefix(FIELD_END_PREFIX).substringBefore('.')
                if (state.terrain == effect) state.terrain = null
                state.fieldEffects -= effect
            }
        }
    }

    private fun asInt(arg: Any): Int? = when (arg) {
        is Number -> arg.toInt()
        is Component -> arg.string.trim().toIntOrNull()
        else -> arg.toString().trim().toIntOrNull()
    }
}
