package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.keksuccino.panoramica.capture.NormalScreenshotCaptureManager;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Hud.class)
public class MixinHud {

    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Hud;isHidden:Z"))
    private boolean modify_isHiddenForNormalScreenshot_Panoramica(boolean original) {
        return original || NormalScreenshotCaptureManager.shouldForceHideHud();
    }

    @ModifyReturnValue(method = "isHidden", at = @At("RETURN"))
    private boolean modify_isHiddenReturnForNormalScreenshot_Panoramica(boolean original) {
        return original || NormalScreenshotCaptureManager.shouldForceHideHud();
    }

}
