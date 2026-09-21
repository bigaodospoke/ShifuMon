package com.shifumon.mixin.cobblemon;

import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBallDisplay;
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.status.PersistentStatus;
import com.shifumon.battle.tile.BattleTileRenderer;
import kotlin.Triple;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Troca o bloco de batalha do Cobblemon pelo painel do ShifuMon. A posição, a animação de entrada e
 * o retrato continuam vindo do Cobblemon; só o desenho do bloco é substituído. Se o renderizador do
 * ShifuMon recusar (desativado ou erro), o bloco original é desenhado normalmente.
 */
@Mixin(BattleOverlay.class)
public abstract class BattleOverlayMixin {
    @Inject(
            method = "drawTile(Lnet/minecraft/client/gui/GuiGraphics;FLcom/cobblemon/mod/common/client/battle/ActiveClientBattlePokemon;ZILcom/cobblemon/mod/common/api/pokedex/PokedexEntryProgress;ZZZ)V",
            at = @At("HEAD")
    )
    private void shifumon$captureTilePokemon(GuiGraphics context, float tickDelta, ActiveClientBattlePokemon activeBattlePokemon,
                                             boolean left, int rank, PokedexEntryProgress dexState, boolean hasCommand,
                                             boolean isHovered, boolean isCompact, CallbackInfo ci) {
        // drawBattleTile não recebe o Pokémon ativo; guardamos aqui para ler tipos e boosts
        BattleTileRenderer.capture(activeBattlePokemon);
    }

    @Inject(
            method = "drawBattleTile(Lnet/minecraft/client/gui/GuiGraphics;FFFZLcom/cobblemon/mod/common/pokemon/Species;ILnet/minecraft/network/chat/MutableComponent;Lcom/cobblemon/mod/common/pokemon/Gender;Lcom/cobblemon/mod/common/pokemon/status/PersistentStatus;Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableState;Lkotlin/Triple;FLcom/cobblemon/mod/common/client/battle/ClientBallDisplay;IFZZZLnet/minecraft/network/chat/MutableComponent;ZLcom/cobblemon/mod/common/api/pokedex/PokedexEntryProgress;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void shifumon$replaceBattleTile(GuiGraphics context, float x, float y, float partialTicks, boolean reversed,
                                            Species species, int level, MutableComponent displayName, Gender gender,
                                            PersistentStatus status, PosableState state, Triple<Float, Float, Float> colour,
                                            float opacity, ClientBallDisplay ballState, int maxHealth, float health,
                                            boolean isSelected, boolean isHovered, boolean isCompact,
                                            MutableComponent actorDisplayName, boolean isFlatHealth,
                                            PokedexEntryProgress dexState, CallbackInfo ci) {
        if (BattleTileRenderer.render((BattleOverlay) (Object) this, context, x, y, partialTicks, reversed, species, state,
                colour, opacity, ballState, isSelected, isHovered, isCompact, actorDisplayName, dexState)) {
            ci.cancel();
        }
    }
}
