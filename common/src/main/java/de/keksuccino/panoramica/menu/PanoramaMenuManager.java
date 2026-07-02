package de.keksuccino.panoramica.menu;

import de.keksuccino.panoramica.Options;
import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.capture.PanoramaCaptureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.Identifier;
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
    private static final Identifier DYNAMIC_PANORAMA_ID = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "dynamic/menu_panorama");
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

        Options.MenuPanoramaMode mode = Panoramica.getOptions().getMenuPanoramaMode();
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
                Panoramica.getLogger().warn("[PANORAMICA] Could not load menu panorama from {}.", selectedFolder, ex);
                return null;
            }
        }

        return DYNAMIC_PANORAMA_ID;
    }

    public static void invalidate() {
        scanDirty = true;
        failedFolder = null;
    }

    @Nullable
    private static Path selectFolder(@NotNull Minecraft minecraft, @NotNull Options.MenuPanoramaMode mode) {
        List<Path> panoramas = getPanoramaFolders(minecraft);
        if (panoramas.isEmpty()) {
            return null;
        }

        return switch (mode) {
            case SHOW_LATEST -> panoramas.get(panoramas.size() - 1);
            case CYCLE_ALL -> {
                int interval = Math.max(1, Panoramica.getOptions().getCycleInterval().seconds);
                int index = (int) ((Util.getMillis() / 1000L / interval) % panoramas.size());
                yield panoramas.get(index);
            }
            case SHOW_VANILLA -> null;
        };
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
            Panoramica.getLogger().warn("[PANORAMICA] Could not scan panorama folder {}.", root, ex);
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
