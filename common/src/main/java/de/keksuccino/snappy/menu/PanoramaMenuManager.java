package de.keksuccino.snappy.menu;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.snappy.Options;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.capture.PanoramaCaptureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PanoramaMenuManager {

    private static final Identifier VANILLA_PANORAMA_BASE = Identifier.withDefaultNamespace("textures/gui/title/background/panorama");
    private static final Identifier DYNAMIC_PANORAMA_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "dynamic/menu_panorama");
    private static final long SCAN_INTERVAL_MS = 5_000L;
    private static final float PARALLAX_X_ROTATION_RANGE = 3.0F;
    private static final float PARALLAX_Y_ROTATION_RANGE = 5.0F;
    private static final float PARALLAX_SMOOTHING = 0.12F;
    private static final long PARALLAX_SPIN_RESUME_DELAY_MS = 1_000L;
    private static final double PARALLAX_MOUSE_MOVEMENT_EPSILON = 0.001D;

    private static List<Path> cachedPanoramas = List.of();
    private static long lastScanMillis;
    private static boolean scanDirty = true;
    private static float parallaxXRotationOffset;
    private static float parallaxYRotationOffset;
    private static double lastParallaxMouseX = Double.NaN;
    private static double lastParallaxMouseY = Double.NaN;
    private static long lastParallaxMouseMoveMillis;
    @Nullable
    private static Path registeredFolder;
    @Nullable
    private static Path failedFolder;

    private PanoramaMenuManager() {
    }

    @Nullable
    public static Identifier prepareTextureForRender(@NotNull Identifier requestedLocation) {
        if (!VANILLA_PANORAMA_BASE.equals(requestedLocation)) {
            return null;
        }

        Options.MenuPanoramaMode mode = Snappy.getOptions().getMenuPanoramaMode();
        Minecraft minecraft = Minecraft.getInstance();
        if (mode == Options.MenuPanoramaMode.SHOW_VANILLA || minecraft == null) {
            releaseRegisteredTexture(minecraft);
            return null;
        }

        Path selectedFolder = selectFolder(minecraft, mode);
        if (selectedFolder == null) {
            releaseRegisteredTexture(minecraft);
            return null;
        }
        if (selectedFolder.equals(failedFolder)) {
            return null;
        }

        if (!selectedFolder.equals(registeredFolder)) {
            try {
                FileCubeMapTexture texture = new FileCubeMapTexture(selectedFolder);
                texture.load();
                minecraft.getTextureManager().register(DYNAMIC_PANORAMA_ID, texture);
                registeredFolder = selectedFolder;
                failedFolder = null;
            } catch (Exception ex) {
                failedFolder = selectedFolder;
                releaseRegisteredTexture(minecraft);
                Snappy.getLogger().warn("[SNAPPY] Could not load menu panorama from {}.", selectedFolder, ex);
                return null;
            }
        }

        return DYNAMIC_PANORAMA_ID;
    }

    public static void invalidate() {
        scanDirty = true;
        failedFolder = null;
    }

    public static void updateMenuParallax() {
        if (!Snappy.getOptions().isMenuPanoramaParallaxEnabled()) {
            resetMenuParallax();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        int width = Math.max(1, window.getScreenWidth());
        int height = Math.max(1, window.getScreenHeight());

        double rawMouseX = minecraft.mouseHandler.xpos();
        double rawMouseY = minecraft.mouseHandler.ypos();
        if (!isMouseInsideWindow(window, rawMouseX, rawMouseY)) {
            settleMenuParallax();
            resetParallaxMouseTracking();
            return;
        }

        updateParallaxMouseMovement(rawMouseX, rawMouseY);

        float mouseX = normalizeMousePosition(rawMouseX, width);
        float mouseY = normalizeMousePosition(rawMouseY, height);
        parallaxXRotationOffset = Mth.lerp(PARALLAX_SMOOTHING, parallaxXRotationOffset, mouseY * PARALLAX_X_ROTATION_RANGE);
        parallaxYRotationOffset = Mth.lerp(PARALLAX_SMOOTHING, parallaxYRotationOffset, mouseX * PARALLAX_Y_ROTATION_RANGE);
    }

    public static boolean shouldSpinMenuPanorama() {
        if (!Snappy.getOptions().isMenuPanoramaParallaxEnabled()) {
            resetMenuParallax();
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        double rawMouseX = minecraft.mouseHandler.xpos();
        double rawMouseY = minecraft.mouseHandler.ypos();
        if (!isMouseInsideWindow(window, rawMouseX, rawMouseY)) {
            resetParallaxMouseTracking();
            return true;
        }

        updateParallaxMouseMovement(rawMouseX, rawMouseY);
        return Util.getMillis() - lastParallaxMouseMoveMillis >= PARALLAX_SPIN_RESUME_DELAY_MS;
    }

    public static float applyMenuParallaxXRotation(float rotXInDegrees) {
        return rotXInDegrees + parallaxXRotationOffset;
    }

    public static float applyMenuParallaxYRotation(float rotYInDegrees) {
        return rotYInDegrees + parallaxYRotationOffset;
    }

    @Nullable
    private static Path selectFolder(@NotNull Minecraft minecraft, @NotNull Options.MenuPanoramaMode mode) {
        if (mode == Options.MenuPanoramaMode.SHOW_SELECTED) {
            return selectSelectedFolder();
        }
        if (mode == Options.MenuPanoramaMode.SHOW_VANILLA) {
            return null;
        }

        List<Path> panoramas = getPanoramaFolders(minecraft);
        if (panoramas.isEmpty()) {
            return null;
        }

        return switch (mode) {
            case SHOW_LATEST -> panoramas.get(panoramas.size() - 1);
            case CYCLE_ALL -> {
                int interval = Math.max(1, Snappy.getOptions().getCycleInterval().seconds);
                int index = (int) ((Util.getMillis() / 1000L / interval) % panoramas.size());
                yield panoramas.get(index);
            }
            case SHOW_VANILLA, SHOW_SELECTED -> null;
        };
    }

    @Nullable
    private static Path selectSelectedFolder() {
        List<Path> selectedPanoramas = MenuBackgroundSelectionManager.getSelectedPanoramaFolders();
        if (selectedPanoramas.isEmpty()) {
            return null;
        }
        if (selectedPanoramas.size() == 1) {
            return selectedPanoramas.get(0);
        }

        int interval = Math.max(1, Snappy.getOptions().getCycleInterval().seconds);
        int index = (int) ((Util.getMillis() / 1000L / interval) % selectedPanoramas.size());
        return selectedPanoramas.get(index);
    }

    @NotNull
    private static List<Path> getPanoramaFolders(@NotNull Minecraft minecraft) {
        long now = Util.getMillis();
        if (!scanDirty && now - lastScanMillis < SCAN_INTERVAL_MS) {
            return cachedPanoramas;
        }

        List<Path> panoramas = new ArrayList<>();
        Path gameDirectory = minecraft.gameDirectory.toPath();
        addPanoramaFolders(gameDirectory.resolve(PanoramaCaptureManager.DEDICATED_SCREENSHOT_DIR), panoramas);
        addPanoramaFolders(gameDirectory.resolve(Screenshot.SCREENSHOT_DIR), panoramas);
        panoramas.sort(Comparator.comparingLong(PanoramaMenuManager::lastModifiedMillis).thenComparing(Path::toString));
        cachedPanoramas = List.copyOf(panoramas);
        lastScanMillis = now;
        scanDirty = false;
        return cachedPanoramas;
    }

    private static void addPanoramaFolders(@NotNull Path root, @NotNull List<Path> result) {
        if (!Files.isDirectory(root)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path path : stream) {
                if (isValidPanoramaFolder(path) && !result.contains(path)) {
                    result.add(path);
                }
            }
        } catch (IOException ex) {
            Snappy.getLogger().warn("[SNAPPY] Could not scan panorama folder {}.", root, ex);
        }
    }

    private static boolean isValidPanoramaFolder(@NotNull Path folder) {
        if (!Files.isDirectory(folder)) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (!Files.isRegularFile(folder.resolve("panorama_" + i + ".png"))) {
                return false;
            }
        }
        return true;
    }

    private static float normalizeMousePosition(double position, int size) {
        return Mth.clamp((float) ((position / size) * 2.0D - 1.0D), -1.0F, 1.0F);
    }

    private static boolean isMouseInsideWindow(@NotNull Window window, double mouseX, double mouseY) {
        return mouseX >= 0.0D && mouseY >= 0.0D && mouseX < window.getScreenWidth() && mouseY < window.getScreenHeight();
    }

    private static void updateParallaxMouseMovement(double mouseX, double mouseY) {
        if (!Double.isNaN(lastParallaxMouseX) && !Double.isNaN(lastParallaxMouseY)
                && (Math.abs(mouseX - lastParallaxMouseX) > PARALLAX_MOUSE_MOVEMENT_EPSILON
                || Math.abs(mouseY - lastParallaxMouseY) > PARALLAX_MOUSE_MOVEMENT_EPSILON)) {
            lastParallaxMouseMoveMillis = Util.getMillis();
        }

        lastParallaxMouseX = mouseX;
        lastParallaxMouseY = mouseY;
    }

    private static void settleMenuParallax() {
        parallaxXRotationOffset = Mth.lerp(PARALLAX_SMOOTHING, parallaxXRotationOffset, 0.0F);
        parallaxYRotationOffset = Mth.lerp(PARALLAX_SMOOTHING, parallaxYRotationOffset, 0.0F);
    }

    private static void resetParallaxMouseTracking() {
        lastParallaxMouseX = Double.NaN;
        lastParallaxMouseY = Double.NaN;
        lastParallaxMouseMoveMillis = 0L;
    }

    private static void resetMenuParallax() {
        parallaxXRotationOffset = 0.0F;
        parallaxYRotationOffset = 0.0F;
        resetParallaxMouseTracking();
    }

    private static long lastModifiedMillis(@NotNull Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    private static void releaseRegisteredTexture(@Nullable Minecraft minecraft) {
        if (minecraft != null && registeredFolder != null) {
            minecraft.getTextureManager().release(DYNAMIC_PANORAMA_ID);
            registeredFolder = null;
        }
    }

}
