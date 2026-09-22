package com.shifumon.battle

import net.minecraft.core.registries.BuiltInRegistries

/** Quantos turnos dura cada clima, terreno, sala e condição de lado, pelas regras do Showdown. */
internal object EffectDurations {
    /** Duração normal e, quando um item segurado por quem criou o efeito estende, a duração com ele. */
    class Duration(val base: Int, val extended: Int? = null, val extenderItem: String? = null)

    /** Podem ser empilhados (Spikes até 3 camadas, Toxic Spikes até 2). */
    val LAYERED = setOf("spikes", "toxicspikes")

    private val weatherRocks = mapOf(
        "raindance" to "damp_rock", "sunnyday" to "heat_rock", "sandstorm" to "smooth_rock",
        "hail" to "icy_rock", "snow" to "icy_rock",
    )

    fun maxLayers(id: String): Int = if (id == "spikes") 3 else 2

    /** Climas primordiais (Desolate Land...) duram enquanto o Pokémon estiver em campo: sem conta. */
    fun weather(id: String): Duration? = weatherRocks[id]?.let { Duration(5, 8, it) }

    fun terrain(): Duration = Duration(5, 8, "terrain_extender")

    fun field(id: String): Duration? = when (id) {
        "trickroom", "magicroom", "wonderroom", "gravity", "mudsport", "watersport" -> Duration(5)
        else -> null
    }

    /** Armadilhas como Spikes e Stealth Rock ficam até alguém remover: sem conta. */
    fun side(id: String): Duration? = when (id) {
        "reflect", "lightscreen", "auroraveil" -> Duration(5, 8, "light_clay")
        "safeguard", "mist", "luckychant" -> Duration(5)
        "tailwind", "firepledge", "grasspledge", "waterpledge" -> Duration(4)
        else -> null
    }

    /**
     * Duração em turnos. Se quem criou o efeito é um Pokémon do jogador, o item dele decide a conta
     * exata; do oponente o item é desconhecido, então vira uma faixa (menos em batalha contra selvagem).
     */
    fun resolve(duration: Duration, actorNames: List<String>): IntRange {
        val base = duration.base
        val extended = duration.extended ?: return base..base
        val actor = BattleReader.activeFromNames(actorNames)
        val owned = actor?.let(BattleReader::ownedPokemon)
        if (owned != null) {
            val item = BuiltInRegistries.ITEM.getKey(owned.heldItem().item).path
            return if (item == duration.extenderItem) extended..extended else base..base
        }
        return if (BattleReader.isWildBattle()) base..base else base..extended
    }
}
