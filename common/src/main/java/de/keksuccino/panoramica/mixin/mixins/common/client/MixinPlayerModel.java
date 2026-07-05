package de.keksuccino.panoramica.mixin.mixins.common.client;

import de.keksuccino.panoramica.photo.PhotoModeManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Run after default-priority animation mixins so photo poses stay authoritative. This fixes a conflict with "Not Enough Animations".
@Mixin(value = PlayerModel.class, priority = 1100)
public abstract class MixinPlayerModel extends HumanoidModel<AvatarRenderState> {

    @SuppressWarnings("unused")
    private MixinPlayerModel(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void after_setupAnim_Panoramica(AvatarRenderState state, CallbackInfo info) {
        PhotoModeManager.applySelfPose((PlayerModel) (Object) this, state);
    }

}
