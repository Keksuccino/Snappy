package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.keksuccino.panoramica.photo.PhotoModeManager;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class MixinLightmapRenderStateExtractor {

    @Shadow private boolean needsUpdate;

    @Inject(method = "extract", at = @At("HEAD"))
    private void before_extract_Panoramica(LightmapRenderState renderState, float partialTicks, CallbackInfo info) {
        if (PhotoModeManager.isActive()) {
            this.needsUpdate = true;
        }
    }

    @ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0))
    private float modify_brightnessOption_Panoramica(float original) {
        return PhotoModeManager.overrideBrightness(original);
    }

}
