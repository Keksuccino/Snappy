package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import de.keksuccino.panoramica.capture.NormalScreenshotCaptureManager;
import de.keksuccino.panoramica.capture.PanoramaCaptureManager;
import de.keksuccino.panoramica.photo.PhotoModeManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
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
    @Shadow @Final private RenderTarget mainRenderTarget;
    @Shadow @Final private CrossFrameResourcePool resourcePool;

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_Panoramica(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        PanoramaCaptureManager.beforeRender((GameRenderer) (Object) this);
    }

    @Inject(method = "extractWindow", at = @At("TAIL"))
    private void after_extractWindow_Panoramica(CallbackInfo info) {
        PanoramaCaptureManager.overrideWindowRenderState(this.gameRenderState.windowRenderState);
    }

    @Inject(method = "extract", at = @At("HEAD"))
    private void before_extract_Panoramica(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        PhotoModeManager.beginEnvironmentOverrideScope();
    }

    @Inject(method = "extract", at = @At("TAIL"))
    private void after_extract_Panoramica(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        PhotoModeManager.afterExtractRenderState(this.gameRenderState);
        PhotoModeManager.endEnvironmentOverrideScope();
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void before_renderLevel_Panoramica(DeltaTracker deltaTracker, CallbackInfo info) {
        PhotoModeManager.beginEnvironmentOverrideScope();
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void after_renderLevel_Panoramica(DeltaTracker deltaTracker, CallbackInfo info) {
        PanoramaCaptureManager.afterRenderLevel();
        PhotoModeManager.endEnvironmentOverrideScope();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER))
    private void after_renderGui_Panoramica(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        if (!PanoramaCaptureManager.isRenderCaptureActive()) {
            NormalScreenshotCaptureManager.captureQueuedScreenshots();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
    private void before_renderEndFrame_Panoramica(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        PhotoModeManager.processColorizeEffect(Minecraft.getInstance(), this.mainRenderTarget, this.resourcePool);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void after_render_Panoramica(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        PanoramaCaptureManager.afterRender((GameRenderer) (Object) this);
    }

}
