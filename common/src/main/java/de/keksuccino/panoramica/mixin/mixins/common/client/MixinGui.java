package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.sugar.Local;
import de.keksuccino.panoramica.preview.ScreenshotPreviewManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;applyCursor(Lcom/mojang/blaze3d/platform/Window;)V", shift = At.Shift.BEFORE))
    private void before_applyCursor_Panoramica(
            DeltaTracker deltaTracker,
            boolean shouldRenderLevel,
            boolean resourcesLoaded,
            CallbackInfo info,
            @Local @NotNull GuiGraphicsExtractor graphics
    ) {
        ScreenshotPreviewManager.extractRenderState(graphics, shouldRenderLevel);
    }

}
