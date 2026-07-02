package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.keksuccino.panoramica.capture.PanoramaCaptureManager;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class MixinCamera {

    @ModifyExpressionValue(method = {"update", "createProjectionMatrixForCulling"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I"))
    private int wrap_getWidth_Panoramica(int original) {
        return PanoramaCaptureManager.overrideWindowWidth(original);
    }

    @ModifyExpressionValue(method = {"update", "createProjectionMatrixForCulling"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I"))
    private int wrap_getHeight_Panoramica(int original) {
        return PanoramaCaptureManager.overrideWindowHeight(original);
    }

}
