package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.snappy.capture.NormalScreenshotCaptureManager;
import de.keksuccino.snappy.capture.PanoramaCaptureManager;
import de.keksuccino.snappy.photo.PhotoModeManager;
import de.keksuccino.snappy.preview.ScreenshotPreviewManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow public HitResult hitResult;
    @Shadow public Entity crosshairPickEntity;

    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_Snappy(CallbackInfo info) {
        PanoramaCaptureManager.clientTick((Minecraft) (Object) this);
        PhotoModeManager.clientTick((Minecraft) (Object) this);
        ScreenshotPreviewManager.clientTick();
    }

    @Inject(method = "isPaused", at = @At("HEAD"), cancellable = true)
    private void before_isPaused_Snappy(CallbackInfoReturnable<Boolean> info) {
        if (PanoramaCaptureManager.shouldFreezeGameForCapture()) {
            info.setReturnValue(true);
        }
    }

    @WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker$Timer;advanceGameTime(J)I"))
    private int wrap_runTickAdvanceGameTime_Snappy(DeltaTracker.Timer instance, long currentMs, Operation<Integer> original) {
        if (PanoramaCaptureManager.shouldFreezeGameForCapture()) {
            return 0;
        }

        return original.call(instance, currentMs);
    }

    @WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketProcessor;processQueuedPackets()V"))
    private void wrap_runTickProcessQueuedPackets_Snappy(PacketProcessor instance, Operation<Void> original) {
        if (!PanoramaCaptureManager.shouldFreezeGameForCapture()) {
            original.call(instance);
        }
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;update()V"))
    private void wrap_renderFrameUpdateClientLevel_Snappy(ClientLevel instance, Operation<Void> original) {
        if (!PanoramaCaptureManager.shouldFreezeGameForCapture()) {
            original.call(instance);
        }
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;update(Lnet/minecraft/client/DeltaTracker;)V"))
    private void wrap_renderFrameUpdateGameRenderer_Snappy(GameRenderer instance, DeltaTracker deltaTracker, Operation<Void> original) {
        PanoramaCaptureManager.beforeRenderFrame((Minecraft) (Object) this, deltaTracker);
        original.call(instance, PanoramaCaptureManager.freezeRenderDelta(deltaTracker));
        PanoramaCaptureManager.afterGameRendererUpdate(instance);
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;extract(Lnet/minecraft/client/DeltaTracker;Z)V"))
    private void wrap_renderFrameExtractGameRenderer_Snappy(GameRenderer instance, DeltaTracker deltaTracker, boolean advanceGameTime, Operation<Void> original) {
        original.call(instance, PanoramaCaptureManager.freezeRenderDelta(deltaTracker), advanceGameTime);
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"))
    private void wrap_renderFrameRenderGameRenderer_Snappy(GameRenderer instance, DeltaTracker deltaTracker, boolean advanceGameTime, Operation<Void> original) {
        original.call(instance, PanoramaCaptureManager.freezeRenderDelta(deltaTracker), advanceGameTime);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void before_close_Snappy(CallbackInfo info) {
        NormalScreenshotCaptureManager.close();
        PhotoModeManager.close();
        ScreenshotPreviewManager.close();
    }

    @Inject(method = "pick", at = @At("TAIL"))
    private void after_pick_Snappy(float partialTicks, CallbackInfo info) {
        HitResult photoHitResult = PhotoModeManager.pick((Minecraft) (Object) this);
        if (photoHitResult == null) {
            return;
        }

        this.hitResult = photoHitResult;
        this.crosshairPickEntity = photoHitResult instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
    }

    @Inject(method = "showDebugChat", at = @At("HEAD"))
    private void before_showDebugChat_Snappy(Component message, CallbackInfo info) {
        ScreenshotPreviewManager.acceptDebugChatMessage(message);
    }

}
