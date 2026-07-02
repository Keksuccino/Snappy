package de.keksuccino.panoramica.mixin.mixins.common.client;

import de.keksuccino.panoramica.capture.PanoramaCaptureManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Shadow @Final private GameRenderState gameRenderState;

    @Inject(method = "extractWindow", at = @At("TAIL"))
    private void after_extractWindow_Panoramica(CallbackInfo info) {
        PanoramaCaptureManager.overrideWindowRenderState(this.gameRenderState.windowRenderState);
    }

}
