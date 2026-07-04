package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.keksuccino.panoramica.capture.NormalScreenshotCaptureManager;
import de.keksuccino.panoramica.photo.PhotoModeManager;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Hud.class)
public class MixinHud {

    @ModifyReturnValue(method = "isHidden", at = @At("RETURN"))
    private boolean modify_isHiddenReturnForNormalScreenshot_Panoramica(boolean original) {
        return original || NormalScreenshotCaptureManager.shouldForceHideHud() || PhotoModeManager.shouldHideHud();
    }

}
