package com.shifumon.mixin.minecraft;

import com.shifumon.resource.RetroResourcePack;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/** Adiciona o pacote "ShifuMon Retro" à lista de pacotes de recursos do cliente. */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    @Shadow
    @Final
    @Mutable
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void shifumon$addRetroPack(RepositorySource[] providers, CallbackInfo ci) {
        // Só o repositório de pacotes de recursos do cliente tem a fonte vanilla do cliente;
        // os de data packs (servidor integrado) ficam como estão
        for (RepositorySource provider : providers) {
            if (provider instanceof ClientPackSource) {
                Set<RepositorySource> extended = new LinkedHashSet<>(sources);
                extended.add(RetroResourcePack.SOURCE);
                sources = extended;
                return;
            }
        }
    }
}
