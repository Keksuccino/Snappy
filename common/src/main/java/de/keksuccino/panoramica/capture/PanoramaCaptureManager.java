package de.keksuccino.panoramica.capture;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.panoramica.KeyMappings;
import de.keksuccino.panoramica.Options;
import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.menu.PanoramaMenuManager;
import de.keksuccino.panoramica.mixin.mixins.common.client.AccessorMixinGameRenderer;
import de.keksuccino.panoramica.preview.ScreenshotPreviewManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.network.chat.ClickEvent.OpenFile;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class PanoramaCaptureManager {

    public static final String DEDICATED_SCREENSHOT_DIR = "panorama_screenshots";

    private static final float[][] FACE_ROTATIONS = new float[][]{
            {0.0F, 0.0F},
            {90.0F, 0.0F},
            {180.0F, 0.0F},
            {-90.0F, 0.0F},
            {0.0F, -90.0F},
            {0.0F, 90.0F}
    };

    private static volatile boolean captureInProgress;
    @Nullable
    private static CaptureDimensions activeDimensions;

    private PanoramaCaptureManager() {
    }

    public static void clientTick(@NotNull Minecraft minecraft) {
        while (KeyMappings.KEY_TAKE_PANORAMA.consumeClick()) {
            requestCapture(minecraft);
        }
    }

    public static void requestCapture(@NotNull Minecraft minecraft) {
        if (captureInProgress) {
            minecraft.showDebugChat(Component.translatable("panoramica.capture.in_progress"));
            return;
        }
        if (minecraft.level == null || minecraft.player == null) {
            minecraft.showDebugChat(Component.translatable("panoramica.capture.unavailable"));
            return;
        }

        captureInProgress = true;
        Options.ResolutionPreset preset = Panoramica.getOptions().getScreenshotResolution();
        Path outputDirectory;
        try {
            outputDirectory = createOutputDirectory(minecraft);
        } catch (IOException ex) {
            captureInProgress = false;
            Panoramica.getLogger().warn("[PANORAMICA] Could not create panorama screenshot folder.", ex);
            minecraft.showDebugChat(Component.translatable("panoramica.capture.failure", ex.getMessage()));
            return;
        }

        minecraft.showDebugChat(Component.translatable("panoramica.capture.started", preset.sideSize + "x" + preset.sideSize));

        try {
            ScreenshotPreviewManager.beginPanoramaCapture();
            capture(minecraft, outputDirectory, preset);
        } catch (Exception ex) {
            captureInProgress = false;
            ScreenshotPreviewManager.finishPanoramaCapture();
            Panoramica.getLogger().error("[PANORAMICA] Could not capture panorama.", ex);
            minecraft.showDebugChat(Component.translatable("panoramica.capture.failure", ex.getMessage()));
        }
    }

    public static int overrideWindowWidth(int original) {
        CaptureDimensions dimensions = activeDimensions;
        return dimensions != null ? dimensions.width : original;
    }

    public static int overrideWindowHeight(int original) {
        CaptureDimensions dimensions = activeDimensions;
        return dimensions != null ? dimensions.height : original;
    }

    public static void overrideWindowRenderState(@NotNull WindowRenderState state) {
        CaptureDimensions dimensions = activeDimensions;
        if (dimensions != null) {
            state.width = dimensions.width;
            state.height = dimensions.height;
        }
    }

    private static void capture(@NotNull Minecraft minecraft, @NotNull Path outputDirectory, @NotNull Options.ResolutionPreset preset) {
        GameRenderer gameRenderer = minecraft.gameRenderer;
        AccessorMixinGameRenderer gameRendererAccessor = (AccessorMixinGameRenderer) gameRenderer;
        RenderTarget originalTarget = gameRendererAccessor.getMainRenderTarget_Panoramica();
        RenderTarget captureTarget = null;
        LocalPlayer player = minecraft.player;
        if (player == null) {
            throw new IllegalStateException("No player is available for panorama capture.");
        }

        float originalXRot = player.getXRot();
        float originalYRot = player.getYRot();
        float originalXRotO = player.xRotO;
        float originalYRotO = player.yRotO;
        boolean originalRenderBlockOutline = gameRendererAccessor.getRenderBlockOutline_Panoramica();
        Camera camera = gameRenderer.mainCamera();
        boolean wasPanoramicMode = camera.isPanoramicMode();
        AtomicInteger pendingFaces = new AtomicInteger(FACE_ROTATIONS.length);
        AtomicBoolean failed = new AtomicBoolean(false);

        try {
            captureTarget = new TextureTarget("Panoramica Capture", preset.sideSize, preset.sideSize, true, GpuFormat.RGBA8_UNORM);
            activeDimensions = new CaptureDimensions(preset.sideSize, preset.sideSize);
            gameRendererAccessor.setMainRenderTarget_Panoramica(captureTarget);
            minecraft.levelRenderer.resize(preset.sideSize, preset.sideSize);
            gameRenderer.setRenderBlockOutline(false);
            camera.enablePanoramicMode();

            for (int i = 0; i < FACE_ROTATIONS.length; i++) {
                renderFace(gameRenderer, player, originalYRot, FACE_ROTATIONS[i]);
                scheduleFaceWrite(minecraft, captureTarget, outputDirectory, i, pendingFaces, failed);
            }
        } finally {
            player.setXRot(originalXRot);
            player.setYRot(originalYRot);
            player.xRotO = originalXRotO;
            player.yRotO = originalYRotO;
            gameRenderer.setRenderBlockOutline(originalRenderBlockOutline);
            gameRendererAccessor.setMainRenderTarget_Panoramica(originalTarget);
            minecraft.levelRenderer.resize(originalTarget.width, originalTarget.height);
            activeDimensions = null;
            if (!wasPanoramicMode) {
                camera.disablePanoramicMode();
            }
            if (captureTarget != null) {
                captureTarget.destroyBuffers();
            }
        }
    }

    private static void renderFace(
            @NotNull GameRenderer gameRenderer,
            @NotNull LocalPlayer player,
            float baseYRot,
            float @NotNull [] rotation
    ) {
        player.setYRot((baseYRot + rotation[0]) % 360.0F);
        player.setXRot(rotation[1]);
        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();
        gameRenderer.update(DeltaTracker.ONE);
        gameRenderer.extract(DeltaTracker.ONE, true);
        gameRenderer.renderLevel(DeltaTracker.ONE);
    }

    private static void scheduleFaceWrite(
            @NotNull Minecraft minecraft,
            @NotNull RenderTarget captureTarget,
            @NotNull Path outputDirectory,
            int face,
            @NotNull AtomicInteger pendingFaces,
            @NotNull AtomicBoolean failed
    ) {
        try {
            Screenshot.takeScreenshot(captureTarget, image -> {
                ScreenshotPreviewManager.collectPanoramaFace(face, image);
                Util.ioPool().execute(() -> writeFace(minecraft, outputDirectory, face, image, pendingFaces, failed));
            });
        } catch (Exception ex) {
            failed.set(true);
            Panoramica.getLogger().warn("[PANORAMICA] Could not copy panorama face {}.", face, ex);
            finishFace(minecraft, outputDirectory, pendingFaces, failed);
        }
    }

    private static void writeFace(
            @NotNull Minecraft minecraft,
            @NotNull Path outputDirectory,
            int face,
            @NotNull NativeImage image,
            @NotNull AtomicInteger pendingFaces,
            @NotNull AtomicBoolean failed
    ) {
        try (NativeImage closableImage = image) {
            closableImage.writeToFile(outputDirectory.resolve("panorama_" + face + ".png"));
        } catch (Exception ex) {
            failed.set(true);
            Panoramica.getLogger().warn("[PANORAMICA] Could not save panorama face {}.", face, ex);
        } finally {
            finishFace(minecraft, outputDirectory, pendingFaces, failed);
        }
    }

    private static void finishFace(
            @NotNull Minecraft minecraft,
            @NotNull Path outputDirectory,
            @NotNull AtomicInteger pendingFaces,
            @NotNull AtomicBoolean failed
    ) {
        if (pendingFaces.decrementAndGet() == 0) {
            captureInProgress = false;
            ScreenshotPreviewManager.finishPanoramaCapture();
            PanoramaMenuManager.invalidate();
            minecraft.execute(() -> minecraft.showDebugChat(failed.get()
                    ? Component.translatable("panoramica.capture.partial_failure", outputDirectory.toString())
                    : Component.translatable("panoramica.capture.success", folderComponent(outputDirectory))));
        }
    }

    @NotNull
    private static Path createOutputDirectory(@NotNull Minecraft minecraft) throws IOException {
        Path root = switch (Panoramica.getOptions().getStorageLocation()) {
            case DEDICATED_FOLDER -> minecraft.gameDirectory.toPath().resolve(DEDICATED_SCREENSHOT_DIR);
            case SCREENSHOTS_FOLDER -> minecraft.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR);
        };
        Files.createDirectories(root);
        String baseName = Util.getFilenameFormattedDateTime();

        for (int i = 1; i < 10_000; i++) {
            Path candidate = root.resolve(baseName + (i == 1 ? "" : "_" + i));
            if (Files.notExists(candidate)) {
                Files.createDirectories(candidate);
                return candidate;
            }
        }

        throw new IOException("Could not find a free timestamped panorama folder name.");
    }

    @NotNull
    private static Component folderComponent(@NotNull Path outputDirectory) {
        return Component.literal(outputDirectory.getFileName().toString())
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new OpenFile(outputDirectory.toFile().getAbsoluteFile())));
    }

    private record CaptureDimensions(int width, int height) {
    }

}
