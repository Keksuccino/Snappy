package de.keksuccino.snappy.menu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.snappy.storage.PanoramaScanner;
import de.keksuccino.snappy.util.file.GameDirectoryUtils;
import net.minecraft.client.Screenshot;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MenuBackgroundSelectionManager {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String SELECTION_FILE_NAME = "menu_background_selection.json";
    private static final int FORMAT_VERSION = 1;
    private static final long VALIDATION_INTERVAL_MS = 5_000L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object SELECTION_LOCK = new Object();

    @Nullable
    private static LinkedHashSet<String> selectedPanoramas;
    private static List<Path> cachedValidSelectedPanoramas = List.of();
    private static boolean validationDirty = true;
    private static long lastValidationMillis;

    private MenuBackgroundSelectionManager() {
    }

    public static boolean isSelected(@NotNull Path panoramaFolder) {
        synchronized (SELECTION_LOCK) {
            loadLocked();
            return selectedPanoramas.contains(normalizeKey(panoramaFolder));
        }
    }

    public static boolean toggle(@NotNull Path panoramaFolder) {
        boolean selected;
        synchronized (SELECTION_LOCK) {
            loadLocked();
            String key = normalizeKey(panoramaFolder);
            if (selectedPanoramas.remove(key)) {
                selected = false;
            } else {
                selectedPanoramas.add(key);
                selected = true;
            }
            markValidationDirtyLocked();
            writeLocked();
        }
        PanoramaMenuManager.invalidate();
        return selected;
    }

    public static void removeAll(@NotNull Iterable<Path> panoramaFolders) {
        boolean changed = false;
        synchronized (SELECTION_LOCK) {
            loadLocked();
            for (Path panoramaFolder : panoramaFolders) {
                changed |= selectedPanoramas.remove(normalizeKey(panoramaFolder));
            }
            if (changed) {
                markValidationDirtyLocked();
                writeLocked();
            }
        }
        if (changed) {
            PanoramaMenuManager.invalidate();
        }
    }

    @NotNull
    public static List<Path> getSelectedPanoramaFolders() {
        long now = Util.getMillis();
        synchronized (SELECTION_LOCK) {
            loadLocked();
            if (!validationDirty && now - lastValidationMillis < VALIDATION_INTERVAL_MS) {
                return cachedValidSelectedPanoramas;
            }

            boolean changed = false;
            Set<String> validKeys = new LinkedHashSet<>();
            List<Path> validPanoramas = new ArrayList<>();
            for (String selectedPanorama : selectedPanoramas) {
                Path panoramaFolder = pathFromKey(selectedPanorama);
                if (panoramaFolder != null && PanoramaScanner.isValidPanoramaFolder(panoramaFolder)) {
                    String normalizedKey = normalizeKey(panoramaFolder);
                    validKeys.add(normalizedKey);
                    validPanoramas.add(panoramaFolder.toAbsolutePath().normalize());
                    changed |= !normalizedKey.equals(selectedPanorama);
                } else {
                    changed = true;
                }
            }

            if (changed) {
                selectedPanoramas = new LinkedHashSet<>(validKeys);
                writeLocked();
            }

            cachedValidSelectedPanoramas = List.copyOf(validPanoramas);
            validationDirty = false;
            lastValidationMillis = now;
            return cachedValidSelectedPanoramas;
        }
    }

    private static void loadLocked() {
        if (selectedPanoramas != null) {
            return;
        }

        selectedPanoramas = new LinkedHashSet<>();
        Path selectionFile = selectionFile();
        if (!Files.isRegularFile(selectionFile)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(selectionFile, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return;
            }

            JsonArray selected = getArray(rootElement.getAsJsonObject(), "selected_panoramas");
            if (selected == null) {
                return;
            }

            for (JsonElement entry : selected) {
                if (entry.isJsonPrimitive()) {
                    addStoredKeyLocked(entry.getAsString());
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("[SNAPPY] Could not read menu background selection from {}.", selectionFile, ex);
        }
    }

    private static void addStoredKeyLocked(@NotNull String key) {
        if (key.isBlank()) {
            return;
        }

        try {
            selectedPanoramas.add(normalizeKey(Path.of(key)));
        } catch (InvalidPathException ex) {
            LOGGER.warn("[SNAPPY] Ignoring invalid menu background panorama path '{}'.", key);
        }
    }

    private static void writeLocked() {
        Path selectionFile = selectionFile();
        try {
            Files.createDirectories(selectionFile.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", FORMAT_VERSION);
            JsonArray selected = new JsonArray();
            for (String selectedPanorama : selectedPanoramas) {
                selected.add(selectedPanorama);
            }
            root.add("selected_panoramas", selected);

            Path temporaryFile = selectionFile.resolveSibling(selectionFile.getFileName() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(temporaryFile, selectionFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryFile, selectionFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            LOGGER.warn("[SNAPPY] Could not write menu background selection to {}.", selectionFile, ex);
        }
    }

    @NotNull
    private static Path selectionFile() {
        return GameDirectoryUtils.getGameDirectory().toPath()
                .resolve(Screenshot.SCREENSHOT_DIR)
                .resolve(SELECTION_FILE_NAME);
    }

    @Nullable
    private static JsonArray getArray(@NotNull JsonObject object, @NotNull String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    @Nullable
    private static Path pathFromKey(@NotNull String key) {
        try {
            return Path.of(key);
        } catch (InvalidPathException ex) {
            return null;
        }
    }

    @NotNull
    private static String normalizeKey(@NotNull Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private static void markValidationDirtyLocked() {
        validationDirty = true;
        cachedValidSelectedPanoramas = List.of();
        lastValidationMillis = 0L;
    }

}
