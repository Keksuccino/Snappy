package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.panoramica.photo.PhotoModeManager;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class MixinAvatarRenderer {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void after_extractRenderState_Panoramica(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo info) {
        PhotoModeManager.applySelfPlayerRenderStateOverrides(entity, state, partialTicks);
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
    private void after_setupRotations_Panoramica(AvatarRenderState state, PoseStack poseStack, float bodyRot, float entityScale, CallbackInfo info) {
        PhotoModeManager.applySelfPlayerModelRotation(state, poseStack, entityScale);
    }

}
