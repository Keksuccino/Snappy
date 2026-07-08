package de.keksuccino.snappy.mixin.mixins.common.client;

import de.keksuccino.snappy.client.render.PhotoSeasonManager;
import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public class MixinLevelExtractor {

    @Shadow @Final private LevelRenderState levelRenderState;
    @Shadow @Nullable private ClientLevel level;

    @Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
    private void before_isEntityVisible_Snappy(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> info) {
        if (PhotoModeManager.shouldHidePlayerEntity(entity)) {
            info.setReturnValue(false);
        } else if (PhotoModeManager.shouldForceRenderSelfPlayerEntity(entity)) {
            info.setReturnValue(true);
        }
    }

    /** @reason Winter photo mode can render snow into sections vanilla still classifies as empty. */
    @Inject(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientChunkCache;flipUpdateTrackingSets()V", shift = At.Shift.AFTER))
    private void after_extractChunkUpdateTrackingSets_Snappy(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo info) {
        ClientLevel level = this.level;
        if (level != null) {
            PhotoSeasonManager.reclassifyWinterEmptySections(level, this.levelRenderState.chunkLoadingRenderState.addedEmptySections, this.levelRenderState.chunkLoadingRenderState.removedEmptySections);
        }
    }

}
