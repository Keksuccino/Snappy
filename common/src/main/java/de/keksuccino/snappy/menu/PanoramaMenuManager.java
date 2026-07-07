package de.keksuccino.snappy.menu;

import de.keksuccino.snappy.Options;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.client.input.MainMenuParallaxController;
import de.keksuccino.snappy.storage.PanoramaScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public final class PanoramaMenuManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Identifier VANILLA_PANORAMA_BASE = Identifier.withDefaultNamespace("textures/gui/title/background/panorama");
    private static final Identifier DYNAMIC_PANORAMA_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "dynamic/menu_panorama");
    private static final long SCAN_INTERVAL_MS = 5_000L;

    private static List<Path> cachedPanoramas = List.of();
    private static long lastScanMillis;
    private static boolean scanDirty = true;
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
                LOGGER.warn("[SNAPPY] Could not load menu panorama from {}.", selectedFolder, ex);
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
        MainMenuParallaxController.update();
    }

    public static boolean shouldSpinMenuPanorama() {
        return MainMenuParallaxController.shouldSpin();
    }

    public static float applyMenuParallaxXRotation(float rotXInDegrees) {
        return MainMenuParallaxController.applyXRotation(rotXInDegrees);
    }

    public static float applyMenuParallaxYRotation(float rotYInDegrees) {
        return MainMenuParallaxController.applyYRotation(rotYInDegrees);
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

        Path gameDirectory = minecraft.gameDirectory.toPath();
        cachedPanoramas = PanoramaScanner.scanPanoramaFolders(
                PanoramaScanner.LinkPolicy.FOLLOW_LINKS,
                PanoramaScanner.ModifiedTimePolicy.FOLDER_ONLY,
                gameDirectory.resolve(Screenshot.SCREENSHOT_DIR)
        );
        lastScanMillis = now;
        scanDirty = false;
        return cachedPanoramas;
    }

    private static void releaseRegisteredTexture(@Nullable Minecraft minecraft) {
        if (minecraft != null && registeredFolder != null) {
            minecraft.getTextureManager().release(DYNAMIC_PANORAMA_ID);
            registeredFolder = null;
        }
    }

}
