package com.shifumon.util

import com.shifumon.ShifuMon
import java.util.concurrent.ConcurrentHashMap

/**
 * Isola cada recurso que depende de classes internas do Cobblemon.
 *
 * Se uma atualização do Cobblemon renomear ou remover algo, só o recurso afetado é desligado
 * (com um único log de erro), em vez de derrubar o jogo ou lotar o log a cada frame.
 */
object FeatureGuard {
    private val failed: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun isBroken(feature: String): Boolean = feature in failed

    fun brokenFeatures(): Set<String> = failed.toSet()

    fun <T> guard(feature: String, fallback: T, block: () -> T): T {
        if (feature in failed) return fallback
        return try {
            block()
        } catch (error: VirtualMachineError) {
            throw error
        } catch (error: Throwable) {
            if (failed.add(feature)) {
                ShifuMon.LOGGER.error(
                    "Recurso '{}' desativado após erro. Possível incompatibilidade com esta versão do Cobblemon.",
                    feature,
                    error,
                )
            }
            fallback
        }
    }
}
