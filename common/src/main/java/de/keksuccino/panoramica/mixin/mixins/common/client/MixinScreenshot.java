package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager.CaptureContext;
import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.capture.NormalScreenshotCaptureManager;
import de.keksuccino.panoramica.photo.PhotoModeManager;
import de.keksuccino.panoramica.preview.ScreenshotPreviewManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent.OpenFile;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        Consumer<Component> effectiveCallback = callback;
        if (!Panoramica.getOptions().areScreenshotChatMessagesEnabled()) {
            effectiveCallback = (Consumer<Component>) message -> minecraft.execute(() -> ScreenshotPreviewManager.acceptDebugChatMessage(message));
        }

        if (PhotoModeManager.isActive()) {
            PhotoModeManager.requestScreenshot(minecraft, workDir, target);
            return;
        }

        if (Panoramica.getOptions().shouldHideHudInNormalScreenshots()) {
            CaptureContext context = ScreenshotMetadataManager.captureNormalScreenshot(minecraft, true);
            NormalScreenshotCaptureManager.requestHiddenHudScreenshot(minecraft, workDir, target, effectiveCallback, context);
            return;
        }

        ScreenshotMetadataManager.enqueueNormalContext(ScreenshotMetadataManager.captureNormalScreenshot(minecraft, minecraft.gui.hud.isHidden()));
        original.call(workDir, target, effectiveCallback);
    }

    @Inject(method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    private static void on_grabScreenshotWithMetadata_Panoramica(
            File workDir,
            @Nullable String forceName,
            RenderTarget target,
            int downscaleFactor,
            Consumer<Component> callback,
            CallbackInfo ci
    ) {
        CaptureContext context = ScreenshotMetadataManager.pollNormalContext();
        if (context == null) {
            return;
        }

        ci.cancel();
        Screenshot.takeScreenshot(target, downscaleFactor, image -> {
            if (forceName == null && downscaleFactor == 1 && ScreenshotPreviewManager.shouldShowNormalScreenshots()) {
                ScreenshotPreviewManager.showNormalScreenshot(image);
            }

            File picDir = new File(workDir, Screenshot.SCREENSHOT_DIR);
            picDir.mkdir();
            File file = forceName == null ? getFile_Panoramica(picDir) : new File(picDir, forceName);

            Util.ioPool().execute(() -> {
                try (NativeImage closableImage = image) {
                    int width = closableImage.getWidth();
                    int height = closableImage.getHeight();
                    closableImage.writeToFile(file);
                    ScreenshotMetadataManager.saveNormalScreenshotMetadata(file.toPath(), context, width, height);
                    Component component = Component.literal(file.getName())
                            .withStyle(ChatFormatting.UNDERLINE)
                            .withStyle(style -> style.withClickEvent(new OpenFile(file.getAbsoluteFile())));
                    callback.accept(Component.translatable("screenshot.success", component));
                } catch (Exception ex) {
                    Panoramica.getLogger().warn("[PANORAMICA] Could not save screenshot.", ex);
                    callback.accept(Component.translatable("screenshot.failure", ex.getMessage()));
                }
            });
        });
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

    @Unique
    private static File getFile_Panoramica(File picDir) {
        String name = Util.getFilenameFormattedDateTime();
        int count = 1;

        while (true) {
            File file = new File(picDir, name + (count == 1 ? "" : "_" + count) + ".png");
            if (!file.exists()) {
                return file;
            }

            count++;
        }
    }

}
