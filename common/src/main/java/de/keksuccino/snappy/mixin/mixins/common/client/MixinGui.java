package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import de.keksuccino.snappy.capture.NormalScreenshotCaptureManager;
import de.keksuccino.snappy.photo.PhotoModeManager;
import de.keksuccino.snappy.preview.ScreenshotPreviewManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {

    @Shadow @Final private GuiRenderState guiRenderState;
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;reset()V", shift = At.Shift.AFTER))
    private void after_resetGuiRenderState_Snappy(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo info) {
        if (NormalScreenshotCaptureManager.shouldForceHideHud() || PhotoModeManager.shouldHideHud()) {
            this.guiRenderState.isHudHidden = true;
        }
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void wrap_extractHud_Snappy(Hud instance, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        if (shouldExtractNonPhotoGui_Snappy()) {
            original.call(instance, graphics, deltaTracker);
        }
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void wrap_extractSavingIndicator_Snappy(Hud instance, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        if (shouldExtractNonPhotoGui_Snappy()) {
            original.call(instance, graphics, deltaTracker);
        }
    }

    // NeoForge replaces the vanilla Screen call with ClientHooks.extractScreen. These alternatives must remain at the same render phase, with exactly one present per loader.
    @Inject(method = "extractRenderState", at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", shift = At.Shift.BEFORE), @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;extractScreen(Lnet/minecraft/client/gui/screens/Screen;Ljava/util/Stack;Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", shift = At.Shift.BEFORE)})
    private void before_extractScreen_Snappy(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo info, @Local @NotNull GuiGraphicsExtractor graphics) {
        PhotoModeManager.extractVignette(graphics, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void wrap_extractToastRenderState_Snappy(ToastManager instance, GuiGraphicsExtractor graphics, Operation<Void> original) {
        if (shouldExtractNonPhotoGui_Snappy()) {
            original.call(instance, graphics);
        }
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractDebugOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void wrap_extractDebugOverlay_Snappy(Hud instance, GuiGraphicsExtractor graphics, Operation<Void> original) {
        if (shouldExtractNonPhotoGui_Snappy()) {
            original.call(instance, graphics);
        }
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractDeferredSubtitles()V"))
    private void wrap_extractDeferredSubtitles_Snappy(Hud instance, Operation<Void> original) {
        if (shouldExtractNonPhotoGui_Snappy()) {
            original.call(instance);
        }
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;applyCursor(Lcom/mojang/blaze3d/platform/Window;)V", shift = At.Shift.BEFORE))
    private void before_applyCursor_Snappy(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo info, @Local @NotNull GuiGraphicsExtractor graphics) {
        if (shouldExtractNonPhotoGui_Snappy()) {
            ScreenshotPreviewManager.extractRenderState(graphics, shouldRenderLevel);
        }
    }

    @Unique
    private static boolean shouldExtractNonPhotoGui_Snappy() {
        return !PhotoModeManager.shouldHideAllNonPhotoGui();
    }

}
