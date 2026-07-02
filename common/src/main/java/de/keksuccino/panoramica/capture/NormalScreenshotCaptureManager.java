package de.keksuccino.panoramica.capture;

import com.mojang.blaze3d.pipeline.RenderTarget;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager.CaptureContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public final class NormalScreenshotCaptureManager {

    private static final Deque<PendingScreenshot> PENDING_SCREENSHOTS = new ArrayDeque<>();
    private static boolean forceHideHudForNextFrame;

    private NormalScreenshotCaptureManager() {
    }

    public static void requestHiddenHudScreenshot(
            @NotNull Minecraft minecraft,
            @NotNull File workDir,
            @NotNull RenderTarget target,
            @NotNull Consumer<Component> callback,
            @NotNull CaptureContext metadataContext
    ) {
        PENDING_SCREENSHOTS.addLast(new PendingScreenshot(workDir, target, callback, metadataContext));
        forceHideHudForNextFrame = true;
        minecraft.getFramerateLimitTracker().onInputReceived();
    }

    public static boolean shouldForceHideHud() {
        return forceHideHudForNextFrame && !PENDING_SCREENSHOTS.isEmpty();
    }

    public static void captureQueuedScreenshots() {
        if (!forceHideHudForNextFrame) {
            return;
        }

        forceHideHudForNextFrame = false;
        if (PENDING_SCREENSHOTS.isEmpty()) {
            return;
        }

        List<PendingScreenshot> screenshots = new ArrayList<>(PENDING_SCREENSHOTS);
        PENDING_SCREENSHOTS.clear();
        for (PendingScreenshot screenshot : screenshots) {
            ScreenshotMetadataManager.enqueueNormalContext(screenshot.metadataContext());
            Screenshot.grab(screenshot.workDir(), screenshot.target(), screenshot.callback());
        }
    }

    public static void close() {
        forceHideHudForNextFrame = false;
        PENDING_SCREENSHOTS.clear();
    }

    private record PendingScreenshot(
            @NotNull File workDir,
            @NotNull RenderTarget target,
            @NotNull Consumer<Component> callback,
            @NotNull CaptureContext metadataContext
    ) {
    }

}
