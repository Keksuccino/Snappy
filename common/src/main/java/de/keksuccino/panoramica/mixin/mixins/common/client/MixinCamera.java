package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.keksuccino.panoramica.capture.PanoramaCaptureManager;
import de.keksuccino.panoramica.photo.PhotoModeManager;
import net.minecraft.client.Camera;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow protected abstract void setPosition(Vec3 position);

    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f panoramicForwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private EnvironmentAttributeProbe attributeProbe;
    @Shadow @Nullable private Level level;
    @Shadow private boolean detached;
    @Shadow private int matrixPropertiesDirty;

    @ModifyExpressionValue(method = {"update", "createProjectionMatrixForCulling"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I"))
    private int wrap_getWidth_Panoramica(int original) {
        return PanoramaCaptureManager.overrideWindowWidth(original);
    }

    @ModifyExpressionValue(method = {"update", "createProjectionMatrixForCulling"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I"))
    private int wrap_getHeight_Panoramica(int original) {
        return PanoramaCaptureManager.overrideWindowHeight(original);
    }

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void after_alignWithEntity_Panoramica(float partialTicks, CallbackInfo info) {
        PhotoModeManager.CameraState state = PhotoModeManager.cameraState();
        if (state == null) {
            return;
        }

        this.xRot = state.pitch();
        this.yRot = state.yaw();
        this.rotation.rotationYXZ(
                (float) Math.PI - state.yaw() * (float) (Math.PI / 180.0),
                -state.pitch() * (float) (Math.PI / 180.0),
                state.roll() * (float) (Math.PI / 180.0)
        );
        this.forwards.set(0.0F, 0.0F, -1.0F).rotate(this.rotation);
        this.up.set(0.0F, 1.0F, 0.0F).rotate(this.rotation);
        this.left.set(-1.0F, 0.0F, 0.0F).rotate(this.rotation);
        this.panoramicForwards.set(this.forwards);
        this.matrixPropertiesDirty |= 3;
        this.setPosition(state.position());
        if (this.level != null) {
            this.attributeProbe.tick(this.level, state.position());
        }
        this.detached = true;
    }

    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float modify_calculateFovReturn_Panoramica(float original) {
        return PhotoModeManager.overrideFieldOfView(original);
    }

}
