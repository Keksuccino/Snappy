package de.keksuccino.snappy.capture;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.snappy.KeyMappings;
import de.keksuccino.snappy.Options;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.menu.PanoramaMenuManager;
import de.keksuccino.snappy.metadata.ScreenshotMetadataManager;
import de.keksuccino.snappy.metadata.ScreenshotMetadataManager.CaptureContext;
import de.keksuccino.snappy.mixin.mixins.common.client.AccessorMixinGameRenderer;
import de.keksuccino.snappy.mixin.mixins.common.client.AccessorMixinLevelRenderer;
import de.keksuccino.snappy.preview.ScreenshotPreviewManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
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
    private static volatile CaptureSession activeSession;
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
            showScreenshotMessage(minecraft, Component.translatable("snappy.capture.in_progress"));
            return;
        }
        if (minecraft.level == null || minecraft.player == null) {
            showScreenshotMessage(minecraft, Component.translatable("snappy.capture.unavailable"));
            return;
        }

        captureInProgress = true;
        Options.ResolutionPreset preset = Snappy.getOptions().getScreenshotResolution();
        Path outputDirectory;
        try {
            outputDirectory = createOutputDirectory(minecraft);
        } catch (IOException ex) {
            captureInProgress = false;
            Snappy.getLogger().warn("[SNAPPY] Could not create panorama screenshot folder.", ex);
            showScreenshotMessage(minecraft, Component.translatable("snappy.capture.failure", ex.getMessage()));
            return;
        }

        showScreenshotMessage(minecraft, Component.translatable("snappy.capture.started", preset.sideSize + "x" + preset.sideSize));
        CaptureContext metadataContext = ScreenshotMetadataManager.capturePanoramaScreenshot(minecraft);

        try {
            ScreenshotPreviewManager.beginPanoramaCapture();
            startCapture(minecraft, outputDirectory, preset, metadataContext);
        } catch (Exception ex) {
            captureInProgress = false;
            activeSession = null;
            activeDimensions = null;
            ScreenshotPreviewManager.finishPanoramaCapture();
            Snappy.getLogger().error("[SNAPPY] Could not capture panorama.", ex);
            showScreenshotMessage(minecraft, Component.translatable("snappy.capture.failure", ex.getMessage()));
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

    public static void beforeRenderFrame(@NotNull Minecraft minecraft, @NotNull DeltaTracker deltaTracker) {
        CaptureSession session = activeSession;
        if (session != null) {
            session.prepareFrame(minecraft, deltaTracker);
        }
    }

    public static void afterGameRendererUpdate(@NotNull GameRenderer gameRenderer) {
        CaptureSession session = activeSession;
        if (session != null) {
            session.refreshPanoramicDirection(gameRenderer);
        }
    }

    public static void beforeRender(@NotNull GameRenderer gameRenderer) {
        CaptureSession session = activeSession;
        if (session != null) {
            session.installRenderTarget(gameRenderer);
        }
    }

    public static void afterRenderLevel() {
        CaptureSession session = activeSession;
        if (session != null) {
            session.captureCurrentFace();
        }
    }

    public static void afterRender(@NotNull GameRenderer gameRenderer) {
        CaptureSession session = activeSession;
        if (session == null) {
            return;
        }

        session.uninstallRenderTarget(gameRenderer);
        if (session.isFinishedScheduling()) {
            activeSession = null;
            session.restoreRenderState();
            activeDimensions = null;
            session.finishScheduling();
        } else {
            session.restorePlayerRotation();
        }
    }

    public static boolean isRenderCaptureActive() {
        return activeSession != null;
    }

    public static boolean shouldFreezeGameForCapture() {
        return activeSession != null;
    }

    @NotNull
    public static DeltaTracker freezeRenderDelta(@NotNull DeltaTracker original) {
        CaptureSession session = activeSession;
        return session != null ? session.getRenderDelta(original) : original;
    }

    private static void startCapture(
            @NotNull Minecraft minecraft,
            @NotNull Path outputDirectory,
            @NotNull Options.ResolutionPreset preset,
            @NotNull CaptureContext metadataContext
    ) {
        GameRenderer gameRenderer = minecraft.gameRenderer;
        AccessorMixinGameRenderer gameRendererAccessor = (AccessorMixinGameRenderer) gameRenderer;
        RenderTarget originalTarget = gameRendererAccessor.getMainRenderTarget_Snappy();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            throw new IllegalStateException("No player is available for panorama capture.");
        }

        float originalXRot = player.getXRot();
        float originalYRot = player.getYRot();
        float originalXRotO = player.xRotO;
        float originalYRotO = player.yRotO;
        boolean originalRenderBlockOutline = gameRendererAccessor.getRenderBlockOutline_Snappy();
        Camera camera = gameRenderer.mainCamera();
        boolean wasPanoramicMode = camera.isPanoramicMode();
        RenderTarget captureTarget = new TextureTarget("Snappy Capture", preset.sideSize, preset.sideSize, true, GpuFormat.RGBA8_UNORM);
        CaptureSession session = new CaptureSession(
                minecraft,
                outputDirectory,
                originalTarget,
                captureTarget,
                metadataContext,
                preset.sideSize,
                player,
                originalXRot,
                originalYRot,
                originalXRotO,
                originalYRotO,
                originalRenderBlockOutline,
                camera,
                wasPanoramicMode
        );

        boolean started = false;
        try {
            activeDimensions = new CaptureDimensions(preset.sideSize, preset.sideSize);
            minecraft.levelRenderer.resize(preset.sideSize, preset.sideSize);
            gameRenderer.setRenderBlockOutline(false);
            session.installCaptureSkyRenderer();
            activeSession = session;
            started = true;
        } finally {
            if (!started) {
                session.restoreRenderState();
                activeDimensions = null;
                session.destroyCaptureTarget();
            }
        }
    }

    private static void replaceSkyRenderer(@NotNull LevelRenderer levelRenderer, @NotNull Minecraft minecraft, @NotNull RenderTarget target) {
        AccessorMixinLevelRenderer levelRendererAccessor = (AccessorMixinLevelRenderer) levelRenderer;
        SkyRenderer skyRenderer = levelRendererAccessor.getSkyRenderer_Snappy();
        if (skyRenderer != null) {
            skyRenderer.close();
        }

        levelRendererAccessor.setSkyRenderer_Snappy(new SkyRenderer(minecraft.getTextureManager(), minecraft.getAtlasManager(), target));
    }

    private static void scheduleFaceWrite(
            @NotNull CaptureSession session,
            int face
    ) {
        session.retainFace();
        try {
            Screenshot.takeScreenshot(session.captureTarget, image -> {
                try {
                    ScreenshotPreviewManager.collectPanoramaFace(face, image);
                    Util.ioPool().execute(() -> writeFace(session, face, image));
                } catch (Exception ex) {
                    session.fail();
                    image.close();
                    Snappy.getLogger().warn("[SNAPPY] Could not process panorama face {}.", face, ex);
                    session.finishFace();
                }
            });
        } catch (Exception ex) {
            session.fail();
            Snappy.getLogger().warn("[SNAPPY] Could not copy panorama face {}.", face, ex);
            session.finishFace();
        }
    }

    private static void writeFace(
            @NotNull CaptureSession session,
            int face,
            @NotNull NativeImage image
    ) {
        try (NativeImage closableImage = image) {
            closableImage.writeToFile(session.outputDirectory.resolve("panorama_" + face + ".png"));
        } catch (Exception ex) {
            session.fail();
            Snappy.getLogger().warn("[SNAPPY] Could not save panorama face {}.", face, ex);
        } finally {
            session.finishFace();
        }
    }

    private static void showScreenshotMessage(@NotNull Minecraft minecraft, @NotNull Component message) {
        if (Snappy.getOptions().areScreenshotChatMessagesEnabled()) {
            minecraft.showDebugChat(message);
        } else {
            ScreenshotPreviewManager.acceptDebugChatMessage(message);
        }
    }

    @NotNull
    private static Path createOutputDirectory(@NotNull Minecraft minecraft) throws IOException {
        Path root = switch (Snappy.getOptions().getStorageLocation()) {
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

    private static final class CaptureSession {

        private final Minecraft minecraft;
        private final Path outputDirectory;
        private final RenderTarget originalTarget;
        private final RenderTarget captureTarget;
        private final CaptureContext metadataContext;
        private final int faceSize;
        private final LocalPlayer player;
        private final float originalXRot;
        private final float originalYRot;
        private final float originalXRotO;
        private final float originalYRotO;
        private final boolean originalRenderBlockOutline;
        private final Camera camera;
        private final boolean wasPanoramicMode;
        private final AtomicInteger pendingFaces = new AtomicInteger();
        private final AtomicBoolean failed = new AtomicBoolean(false);
        private final AtomicBoolean schedulingFinished = new AtomicBoolean(false);
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final AtomicBoolean captureTargetDestroyed = new AtomicBoolean(false);
        private int nextFace;
        private boolean finishedScheduling;
        private boolean renderTargetInstalled;
        private boolean renderStateRestored;
        @Nullable
        private DeltaTracker frozenRenderDelta;

        private CaptureSession(
                @NotNull Minecraft minecraft,
                @NotNull Path outputDirectory,
                @NotNull RenderTarget originalTarget,
                @NotNull RenderTarget captureTarget,
                @NotNull CaptureContext metadataContext,
                int faceSize,
                @NotNull LocalPlayer player,
                float originalXRot,
                float originalYRot,
                float originalXRotO,
                float originalYRotO,
                boolean originalRenderBlockOutline,
                @NotNull Camera camera,
                boolean wasPanoramicMode
        ) {
            this.minecraft = minecraft;
            this.outputDirectory = outputDirectory;
            this.originalTarget = originalTarget;
            this.captureTarget = captureTarget;
            this.metadataContext = metadataContext;
            this.faceSize = faceSize;
            this.player = player;
            this.originalXRot = originalXRot;
            this.originalYRot = originalYRot;
            this.originalXRotO = originalXRotO;
            this.originalYRotO = originalYRotO;
            this.originalRenderBlockOutline = originalRenderBlockOutline;
            this.camera = camera;
            this.wasPanoramicMode = wasPanoramicMode;
        }

        private void prepareFrame(@NotNull Minecraft minecraft, @NotNull DeltaTracker deltaTracker) {
            if (minecraft != this.minecraft || this.finishedScheduling) {
                return;
            }
            if (minecraft.level == null || minecraft.player != this.player) {
                this.fail();
                this.finishedScheduling = true;
                return;
            }

            this.freezeRenderDelta(deltaTracker);
            this.applyCurrentFaceRotation();
            this.camera.enablePanoramicMode();
        }

        private void refreshPanoramicDirection(@NotNull GameRenderer gameRenderer) {
            if (!this.finishedScheduling && gameRenderer.mainCamera() == this.camera) {
                this.camera.enablePanoramicMode();
            }
        }

        private void installRenderTarget(@NotNull GameRenderer gameRenderer) {
            if (this.finishedScheduling || this.renderTargetInstalled) {
                return;
            }

            ((AccessorMixinGameRenderer) gameRenderer).setMainRenderTarget_Snappy(this.captureTarget);
            this.renderTargetInstalled = true;
        }

        private void uninstallRenderTarget(@NotNull GameRenderer gameRenderer) {
            if (this.renderTargetInstalled) {
                ((AccessorMixinGameRenderer) gameRenderer).setMainRenderTarget_Snappy(this.originalTarget);
                this.renderTargetInstalled = false;
            }
        }

        private void installCaptureSkyRenderer() {
            replaceSkyRenderer(this.minecraft.levelRenderer, this.minecraft, this.captureTarget);
        }

        private void captureCurrentFace() {
            if (this.finishedScheduling || !this.renderTargetInstalled) {
                return;
            }

            int face = this.nextFace;
            if (face >= FACE_ROTATIONS.length) {
                this.finishedScheduling = true;
                return;
            }

            scheduleFaceWrite(this, face);
            this.nextFace++;
            if (this.nextFace >= FACE_ROTATIONS.length) {
                this.finishedScheduling = true;
            }
            this.restorePlayerRotation();
        }

        private void applyCurrentFaceRotation() {
            float[] rotation = FACE_ROTATIONS[this.nextFace];
            this.player.setYRot((this.originalYRot + rotation[0]) % 360.0F);
            this.player.setXRot(rotation[1]);
            this.player.yRotO = this.player.getYRot();
            this.player.xRotO = this.player.getXRot();
        }

        @NotNull
        private DeltaTracker getRenderDelta(@NotNull DeltaTracker original) {
            return this.freezeRenderDelta(original);
        }

        @NotNull
        private DeltaTracker freezeRenderDelta(@NotNull DeltaTracker original) {
            if (this.frozenRenderDelta == null) {
                this.frozenRenderDelta = new FrozenRenderDelta(
                        original.getGameTimeDeltaTicks(),
                        original.getGameTimeDeltaPartialTick(false),
                        original.getRealtimeDeltaTicks()
                );
            }

            return this.frozenRenderDelta;
        }

        private boolean isFinishedScheduling() {
            return this.finishedScheduling;
        }

        private void restoreRenderState() {
            if (this.renderStateRestored) {
                return;
            }

            this.renderStateRestored = true;
            this.restorePlayerRotation();
            this.minecraft.gameRenderer.setRenderBlockOutline(this.originalRenderBlockOutline);
            ((AccessorMixinGameRenderer) this.minecraft.gameRenderer).setMainRenderTarget_Snappy(this.originalTarget);
            this.minecraft.levelRenderer.resize(this.originalTarget.width, this.originalTarget.height);
            if (!this.wasPanoramicMode) {
                this.camera.disablePanoramicMode();
            }
            replaceSkyRenderer(this.minecraft.levelRenderer, this.minecraft, this.originalTarget);
        }

        private void restorePlayerRotation() {
            this.player.setXRot(this.originalXRot);
            this.player.setYRot(this.originalYRot);
            this.player.xRotO = this.originalXRotO;
            this.player.yRotO = this.originalYRotO;
        }

        private void retainFace() {
            this.pendingFaces.incrementAndGet();
        }

        private void fail() {
            this.failed.set(true);
        }

        private void finishFace() {
            this.pendingFaces.decrementAndGet();
            this.completeIfReady();
        }

        private void finishScheduling() {
            this.schedulingFinished.set(true);
            this.completeIfReady();
        }

        private void completeIfReady() {
            if (!this.schedulingFinished.get() || this.pendingFaces.get() > 0 || !this.completed.compareAndSet(false, true)) {
                return;
            }

            this.minecraft.execute(() -> {
                this.destroyCaptureTarget();
                captureInProgress = false;
                ScreenshotPreviewManager.finishPanoramaCapture();
                PanoramaMenuManager.invalidate();
                if (!this.failed.get()) {
                    ScreenshotMetadataManager.savePanoramaMetadataAsync(this.outputDirectory, this.metadataContext, this.faceSize);
                }
                showScreenshotMessage(this.minecraft, this.failed.get()
                        ? Component.translatable("snappy.capture.partial_failure", this.outputDirectory.toString())
                        : Component.translatable("snappy.capture.success", folderComponent(this.outputDirectory)));
            });
        }

        private void destroyCaptureTarget() {
            if (this.captureTargetDestroyed.compareAndSet(false, true)) {
                this.captureTarget.destroyBuffers();
            }
        }
    }

    private record CaptureDimensions(int width, int height) {
    }

    private record FrozenRenderDelta(float gameTimeDeltaTicks, float gameTimeDeltaPartialTick, float realtimeDeltaTicks) implements DeltaTracker {

        @Override
        public float getGameTimeDeltaTicks() {
            return this.gameTimeDeltaTicks;
        }

        @Override
        public float getGameTimeDeltaPartialTick(boolean ignoreFrozenGame) {
            return this.gameTimeDeltaPartialTick;
        }

        @Override
        public float getRealtimeDeltaTicks() {
            return this.realtimeDeltaTicks;
        }
    }

}
