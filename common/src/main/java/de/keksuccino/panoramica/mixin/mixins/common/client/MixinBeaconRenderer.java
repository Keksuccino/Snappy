package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.panoramica.photo.PhotoModeManager;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconRenderer.class)
public class MixinBeaconRenderer {

    /** @reason Hide real beacon beams visually while keeping beacon block entity state untouched. */
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void before_submit_Panoramica(BeaconRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo info) {
        if (state.blockEntityType == BlockEntityTypes.BEACON && PhotoModeManager.shouldHideBeaconBeams()) {
            info.cancel();
        }
    }

}
