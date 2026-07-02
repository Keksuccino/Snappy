package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.preview.ScreenshotPreviewManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public class MixinScreenshot {

    @WrapOperation(method = "grab(Lnet/minecraft/client/Minecraft;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"))
    private static void wrap_grabNormalScreenshot_Panoramica(
            File workDir,
            RenderTarget target,
            Consumer<Component> callback,
            Operation<Void> original,
            Minecraft minecraft,
            boolean debugPanoramaRequested
    ) {
        if (Panoramica.getOptions().areScreenshotChatMessagesEnabled()) {
            original.call(workDir, target, callback);
            return;
        }

        original.call(workDir, target, (Consumer<Component>) message -> minecraft.execute(() -> ScreenshotPreviewManager.acceptDebugChatMessage(message)));
    }

    @WrapOperation(method = "grab(Lnet/minecraft/client/Minecraft;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;showDebugChat(Lnet/minecraft/network/chat/Component;)V"))
    private static void wrap_showDebugChat_Panoramica(
            Minecraft instance,
            Component message,
            Operation<Void> original
    ) {
        if (Panoramica.getOptions().areScreenshotChatMessagesEnabled()) {
            original.call(instance, message);
        } else {
            ScreenshotPreviewManager.acceptDebugChatMessage(message);
        }
    }

    @WrapOperation(method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Screenshot;takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V"))
    private static void wrap_takeScreenshot_Panoramica(
            RenderTarget target,
            int downscaleFactor,
            Consumer<NativeImage> callback,
            Operation<Void> original,
            File workDir,
            @Nullable String forceName,
            RenderTarget originalTarget,
            int originalDownscaleFactor,
            Consumer<Component> resultCallback
    ) {
        if (forceName == null && downscaleFactor == 1 && ScreenshotPreviewManager.shouldShowNormalScreenshots()) {
            original.call(target, downscaleFactor, (Consumer<NativeImage>) image -> {
                ScreenshotPreviewManager.showNormalScreenshot(image);
                callback.accept(image);
            });
            return;
        }

        original.call(target, downscaleFactor, callback);
    }

}
