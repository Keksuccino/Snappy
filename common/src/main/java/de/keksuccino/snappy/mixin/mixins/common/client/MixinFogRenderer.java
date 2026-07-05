package de.keksuccino.snappy.mixin.mixins.common.client;

import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void after_setupFog_Snappy(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<FogData> info) {
        PhotoModeManager.applyFogOverrides(info.getReturnValue(), renderDistanceInChunks);
    }

}
