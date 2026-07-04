package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.keksuccino.panoramica.menu.PanoramaMenuManager;
import net.minecraft.client.renderer.Panorama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Panorama.class)
public class MixinPanorama {

    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/Panorama;shouldSpin:Z"))
    private boolean wrap_shouldSpin_Panoramica(boolean original) {
        return original && PanoramaMenuManager.shouldSpinMenuPanorama();
    }

}
