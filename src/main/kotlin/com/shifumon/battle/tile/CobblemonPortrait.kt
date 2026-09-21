/*
 * Adaptado de BattleOverlay.kt do Cobblemon.
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.shifumon.battle.tile

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.gui.drawPosablePortrait
import com.cobblemon.mod.common.client.battle.ClientBallDisplay
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity
import com.cobblemon.mod.common.pokemon.Species
import com.shifumon.mixin.cobblemon.BattleOverlayInvoker
import net.minecraft.client.gui.GuiGraphics

/**
 * Retrato animado do bloco de batalha, com a mesma lógica de `BattleOverlay.drawBattleTile` do
 * Cobblemon, desenhado dentro da moldura do ShifuMon.
 *
 * Este arquivo segue a licença MPL-2.0 do Cobblemon; o restante do ShifuMon é MIT.
 */
internal object CobblemonPortrait {
    fun draw(
        overlay: BattleOverlay,
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        size: Int,
        compact: Boolean,
        partialTicks: Float,
        reversed: Boolean,
        species: Species,
        state: PosableState,
        ballState: ClientBallDisplay?,
    ) {
        graphics.enableScissor(x, y, x + size, y + size)
        val pose = graphics.pose()
        pose.pushPose()
        try {
            pose.translate(x + size / 2.0, y - if (compact) 15.0 else 5.0, 0.0)
            if (ballState != null && ballState.currentPose != "shut") ballState.currentPose = "shut"

            if (ballState != null && ballState.stateEmitter.get() == EmptyPokeBallEntity.CaptureState.SHAKE) {
                (overlay as Any as BattleOverlayInvoker).`shifumon$drawPokeBall`(ballState, pose, 5f, partialTicks, reversed)
            } else {
                drawPosablePortrait(
                    identifier = species.resourceIdentifier,
                    matrixStack = pose,
                    scale = 18f * (ballState?.scale ?: 1f) * if (compact) 0.65f else 1f,
                    contextScale = species.getForm(state.currentAspects).baseScale,
                    reversed = reversed,
                    state = state,
                    partialTicks = if (Cobblemon.config.animateBattleTiles) partialTicks else 0f,
                    doQuirks = false,
                )
            }
        } finally {
            pose.popPose()
            graphics.disableScissor()
        }
    }
}
