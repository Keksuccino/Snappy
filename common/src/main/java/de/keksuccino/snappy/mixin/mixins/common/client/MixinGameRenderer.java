package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import de.keksuccino.snappy.capture.NormalScreenshotCaptureManager;
import de.keksuccino.snappy.capture.PanoramaCaptureManager;
import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.joml.Matrix4f;
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
    private void before_render_Snappy(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        PanoramaCaptureManager.beforeRender((GameRenderer) (Object) this);
    }

    @Inject(method = "extractWindow", at = @At("TAIL"))
    private void after_extractWindow_Snappy(CallbackInfo info) {
        PanoramaCaptureManager.overrideWindowRenderState(this.gameRenderState.windowRenderState);
    }

    @Inject(method = "extract", at = @At("HEAD"))
    private void before_extract_Snappy(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        PhotoModeManager.beginWorldOverrideScope();
    }

    @Inject(method = "extract", at = @At("TAIL"))
    private void after_extract_Snappy(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        PhotoModeManager.afterExtractRenderState(this.gameRenderState);
        PhotoModeManager.endWorldOverrideScope();
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void before_renderLevel_Snappy(DeltaTracker deltaTracker, CallbackInfo info) {
        PhotoModeManager.beginWorldOverrideScope();
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void after_renderLevel_Snappy(DeltaTracker deltaTracker, CallbackInfo info) {
        PanoramaCaptureManager.afterRenderLevel();
        PhotoModeManager.endWorldOverrideScope();
    }

    @WrapOperation(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
            )
    )
    private GpuBufferSlice wrap_getLevelProjectionBuffer_Snappy(ProjectionMatrixBuffer instance, Matrix4f projectionMatrix, Operation<GpuBufferSlice> original) {
        PhotoModeManager.captureDepthOfFieldProjection(projectionMatrix);
        return original.call(instance, projectionMatrix);
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void after_renderLevelWorld_Snappy(DeltaTracker deltaTracker, CallbackInfo info) {
        PhotoModeManager.processDepthOfFieldEffect(
                Minecraft.getInstance(),
                this.mainRenderTarget,
                this.resourcePool,
                this.gameRenderState.levelRenderState.cameraRenderState
        );
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER))
    private void after_renderGui_Snappy(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        if (!PanoramaCaptureManager.isRenderCaptureActive()) {
            NormalScreenshotCaptureManager.captureQueuedScreenshots();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
    private void before_renderEndFrame_Snappy(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        PhotoModeManager.processColorizeEffect(Minecraft.getInstance(), this.mainRenderTarget, this.resourcePool);
        PhotoModeManager.processColorAdjustmentEffect(Minecraft.getInstance(), this.mainRenderTarget, this.resourcePool);
        PhotoModeManager.processBloomEffect(Minecraft.getInstance(), this.mainRenderTarget, this.resourcePool);
        PhotoModeManager.processStylizeEffect(Minecraft.getInstance(), this.mainRenderTarget, this.resourcePool);
        PhotoModeManager.processFilmEffects(Minecraft.getInstance(), this.mainRenderTarget, this.resourcePool);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void after_render_Snappy(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo info) {
        PanoramaCaptureManager.afterRender((GameRenderer) (Object) this);
    }

}
