package de.keksuccino.panoramica.mixin.mixins.common.client;

import de.keksuccino.panoramica.capture.PanoramaCaptureManager;
import de.keksuccino.panoramica.preview.ScreenshotPreviewManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_Panoramica(CallbackInfo info) {
        PanoramaCaptureManager.clientTick((Minecraft) (Object) this);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void before_close_Panoramica(CallbackInfo info) {
        ScreenshotPreviewManager.close();
    }

    @Inject(method = "showDebugChat", at = @At("HEAD"))
    private void before_showDebugChat_Panoramica(Component message, CallbackInfo info) {
        ScreenshotPreviewManager.acceptDebugChatMessage(message);
    }

}
