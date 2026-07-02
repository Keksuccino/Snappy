package de.keksuccino.panoramica.metadata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import de.keksuccino.panoramica.Panoramica;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
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
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ScreenshotMetadataManager {

    public static final String METADATA_FILE_NAME = "screenshot_metadata.json";
    private static final int FORMAT_VERSION = 1;
    private static final long TICKS_PER_DAY = 24_000L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object METADATA_LOCK = new Object();
    private static final Object PENDING_NORMAL_CONTEXT_LOCK = new Object();
    private static final ArrayDeque<CaptureContext> PENDING_NORMAL_CONTEXTS = new ArrayDeque<>();
    private static final Path METADATA_FILE = Panoramica.INSTANCE_DATA_DIR.toPath().resolve(METADATA_FILE_NAME);

    @Nullable
    private static Map<String, ScreenshotMetadata> metadataByPath;

    private ScreenshotMetadataManager() {
    }

    @NotNull
    public static CaptureContext captureNormalScreenshot(@NotNull Minecraft minecraft, boolean hudHidden) {
        return capture(minecraft, hudHidden);
    }

    @NotNull
    public static CaptureContext capturePanoramaScreenshot(@NotNull Minecraft minecraft) {
        return capture(minecraft, true);
    }

    public static void enqueueNormalContext(@NotNull CaptureContext context) {
        synchronized (PENDING_NORMAL_CONTEXT_LOCK) {
            PENDING_NORMAL_CONTEXTS.addLast(context);
        }
    }

    @Nullable
    public static CaptureContext pollNormalContext() {
        synchronized (PENDING_NORMAL_CONTEXT_LOCK) {
            return PENDING_NORMAL_CONTEXTS.pollFirst();
        }
    }

    public static void saveNormalScreenshotMetadata(
            @NotNull Path screenshotPath,
            @NotNull CaptureContext context,
            int width,
            int height
    ) {
        saveMetadata(ScreenshotKind.NORMAL, screenshotPath, context, width, height);
    }

    public static void savePanoramaMetadataAsync(
            @NotNull Path screenshotPath,
            @NotNull CaptureContext context,
            int faceSize
    ) {
        Util.ioPool().execute(() -> saveMetadata(ScreenshotKind.PANORAMA, screenshotPath, context, faceSize, faceSize));
    }

    @NotNull
    public static Optional<ScreenshotMetadata> find(@NotNull Path screenshotPath) {
        synchronized (METADATA_LOCK) {
            loadLocked();
            return Optional.ofNullable(metadataByPath.get(normalizeKey(screenshotPath)));
        }
    }

    public static void remove(@NotNull Path screenshotPath) {
        synchronized (METADATA_LOCK) {
            loadLocked();
            if (metadataByPath.remove(normalizeKey(screenshotPath)) != null) {
                writeLocked();
            }
        }
    }

    public static void removeAll(@NotNull Collection<Path> screenshotPaths) {
        synchronized (METADATA_LOCK) {
            loadLocked();
            boolean changed = false;
            for (Path screenshotPath : screenshotPaths) {
                changed |= metadataByPath.remove(normalizeKey(screenshotPath)) != null;
            }
            if (changed) {
                writeLocked();
            }
        }
    }

    private static void saveMetadata(
            @NotNull ScreenshotKind kind,
            @NotNull Path screenshotPath,
            @NotNull CaptureContext context,
            int width,
            int height
    ) {
        ScreenshotMetadata metadata = ScreenshotMetadata.create(kind, screenshotPath, context, Math.max(1, width), Math.max(1, height));
        synchronized (METADATA_LOCK) {
            loadLocked();
            metadataByPath.put(normalizeKey(screenshotPath), metadata);
            writeLocked();
        }
    }

    @NotNull
    private static CaptureContext capture(@NotNull Minecraft minecraft, boolean hudHidden) {
        Instant now = Instant.now();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        long gameTime = level == null ? 0L : level.getGameTime();
        long clockTime = level == null ? 0L : level.getDefaultClockTime();
        long timeOfDay = Math.floorMod(clockTime, TICKS_PER_DAY);
        long worldDay = Math.floorDiv(clockTime, TICKS_PER_DAY);
        String dimension = level == null ? "" : level.dimension().identifier().toString();
        String biome = "";
        String difficulty = "";

        if (level != null) {
            difficulty = Optional.ofNullable(level.getDifficulty()).map(Difficulty::getSerializedName).orElse("");
        }
        if (level != null && player != null) {
            biome = level.getBiome(player.blockPosition()).getRegisteredName();
        }

        return new CaptureContext(
                now.toEpochMilli(),
                now.toString(),
                captureWorld(minecraft, dimension),
                capturePlayer(minecraft, player, biome),
                new TimeInfo(gameTime, clockTime, timeOfDay, worldDay),
                difficulty,
                captureRenderDistance(minecraft),
                hudHidden
        );
    }

    @NotNull
    private static WorldInfo captureWorld(@NotNull Minecraft minecraft, @NotNull String dimension) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (minecraft.hasSingleplayerServer() && server != null) {
            Path savePath = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            String levelName = server.getWorldData().getLevelName();
            Path fileName = savePath.getFileName();
            String name = levelName.isBlank() ? fileName == null ? savePath.toString() : fileName.toString() : levelName;
            return new WorldInfo(name, "singleplayer", savePath.toString(), savePath.toString(), dimension);
        }

        ServerData serverData = minecraft.getCurrentServer();
        if (serverData != null) {
            String type = serverData.isRealm() ? "realm" : serverData.isLan() ? "lan" : "multiplayer";
            return new WorldInfo(nullToEmpty(serverData.name), type, nullToEmpty(serverData.ip), "", dimension);
        }

        return new WorldInfo("", "unknown", "", "", dimension);
    }

    @NotNull
    private static PlayerInfo capturePlayer(
            @NotNull Minecraft minecraft,
            @Nullable LocalPlayer player,
            @NotNull String biome
    ) {
        if (player == null) {
            return new PlayerInfo(0.0D, 0.0D, 0.0D, 0, 0, 0, 0.0F, 0.0F, "", biome);
        }

        GameType gameType = minecraft.gameMode == null ? player.gameMode() : minecraft.gameMode.getPlayerMode();
        return new PlayerInfo(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.blockPosition().getX(),
                player.blockPosition().getY(),
                player.blockPosition().getZ(),
                player.getYRot(),
                player.getXRot(),
                gameType == null ? "" : gameType.getSerializedName(),
                biome
        );
    }

    private static int captureRenderDistance(@NotNull Minecraft minecraft) {
        try {
            return minecraft.options.renderDistance().get();
        } catch (Exception ex) {
            return 0;
        }
    }

    @NotNull
    private static String normalizeKey(@NotNull Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private static void loadLocked() {
        if (metadataByPath != null) {
            return;
        }

        metadataByPath = new LinkedHashMap<>();
        if (!Files.isRegularFile(METADATA_FILE)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(METADATA_FILE, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return;
            }

            JsonObject root = rootElement.getAsJsonObject();
            JsonObject entries = getObject(root, "screenshots");
            if (entries == null) {
                return;
            }

            for (Map.Entry<String, JsonElement> jsonEntry : entries.entrySet()) {
                if (!jsonEntry.getValue().isJsonObject()) {
                    continue;
                }
                ScreenshotMetadata metadata = ScreenshotMetadata.fromJson(jsonEntry.getValue().getAsJsonObject());
                if (metadata != null) {
                    metadataByPath.put(normalizeStoredKey(jsonEntry.getKey(), metadata.path()), metadata);
                }
            }
        } catch (Exception ex) {
            Panoramica.getLogger().warn("[PANORAMICA] Could not read screenshot metadata from {}.", METADATA_FILE, ex);
        }
    }

    @NotNull
    private static String normalizeStoredKey(@NotNull String key, @NotNull String fallbackPath) {
        String value = key.isBlank() ? fallbackPath : key;
        try {
            return normalizeKey(Path.of(value));
        } catch (InvalidPathException ex) {
            return value;
        }
    }

    private static void writeLocked() {
        try {
            Files.createDirectories(METADATA_FILE.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", FORMAT_VERSION);
            JsonObject entries = new JsonObject();
            metadataByPath.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .forEach(entry -> entries.add(entry.getKey(), entry.getValue().toJson()));
            root.add("screenshots", entries);

            Path temporaryFile = METADATA_FILE.resolveSibling(METADATA_FILE.getFileName() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(temporaryFile, METADATA_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryFile, METADATA_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            Panoramica.getLogger().warn("[PANORAMICA] Could not write screenshot metadata to {}.", METADATA_FILE, ex);
        }
    }

    @Nullable
    private static JsonObject getObject(@NotNull JsonObject object, @NotNull String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    @NotNull
    private static String getString(@NotNull JsonObject object, @NotNull String key, @NotNull String defaultValue) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : defaultValue;
    }

    private static int getInt(@NotNull JsonObject object, @NotNull String key, int defaultValue) {
        JsonElement value = object.get(key);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsInt() : defaultValue;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    private static long getLong(@NotNull JsonObject object, @NotNull String key, long defaultValue) {
        JsonElement value = object.get(key);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsLong() : defaultValue;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    private static double getDouble(@NotNull JsonObject object, @NotNull String key, double defaultValue) {
        JsonElement value = object.get(key);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsDouble() : defaultValue;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    private static float getFloat(@NotNull JsonObject object, @NotNull String key, float defaultValue) {
        JsonElement value = object.get(key);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsFloat() : defaultValue;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    private static boolean getBoolean(@NotNull JsonObject object, @NotNull String key, boolean defaultValue) {
        JsonElement value = object.get(key);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : defaultValue;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    @NotNull
    private static String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static void addString(@NotNull JsonObject object, @NotNull String key, @NotNull String value) {
        object.add(key, new JsonPrimitive(value));
    }

    private static void addNumber(@NotNull JsonObject object, @NotNull String key, Number value) {
        object.add(key, new JsonPrimitive(value));
    }

    public enum ScreenshotKind {
        NORMAL("normal", "panoramica.browser.type.normal"),
        PANORAMA("panorama", "panoramica.browser.type.panorama");

        private final String serializedName;
        private final String labelKey;

        ScreenshotKind(@NotNull String serializedName, @NotNull String labelKey) {
            this.serializedName = serializedName;
            this.labelKey = labelKey;
        }

        @NotNull
        public String serializedName() {
            return this.serializedName;
        }

        @NotNull
        public String labelKey() {
            return this.labelKey;
        }

        @NotNull
        public static ScreenshotKind bySerializedName(@NotNull String serializedName) {
            for (ScreenshotKind kind : values()) {
                if (kind.serializedName.equals(serializedName)) {
                    return kind;
                }
            }
            return NORMAL;
        }
    }

    public record ScreenshotMetadata(
            @NotNull ScreenshotKind kind,
            @NotNull String path,
            @NotNull String fileName,
            long capturedAtEpochMillis,
            @NotNull String capturedAtIso,
            int width,
            int height,
            boolean hudHidden,
            @NotNull WorldInfo world,
            @NotNull PlayerInfo player,
            @NotNull TimeInfo time,
            @NotNull String difficulty,
            int renderDistanceChunks
    ) {

        @NotNull
        private static ScreenshotMetadata create(
                @NotNull ScreenshotKind kind,
                @NotNull Path path,
                @NotNull CaptureContext context,
                int width,
                int height
        ) {
            Path normalizedPath = path.toAbsolutePath().normalize();
            Path fileNamePath = normalizedPath.getFileName();
            return new ScreenshotMetadata(
                    kind,
                    normalizedPath.toString(),
                    fileNamePath == null ? normalizedPath.toString() : fileNamePath.toString(),
                    context.capturedAtEpochMillis(),
                    context.capturedAtIso(),
                    width,
                    height,
                    context.hudHidden(),
                    context.world(),
                    context.player(),
                    context.time(),
                    context.difficulty(),
                    context.renderDistanceChunks()
            );
        }

        @NotNull
        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            addString(object, "kind", this.kind.serializedName());
            addString(object, "path", this.path);
            addString(object, "file_name", this.fileName);
            addNumber(object, "captured_at_epoch_millis", this.capturedAtEpochMillis);
            addString(object, "captured_at_iso", this.capturedAtIso);
            addNumber(object, "width", this.width);
            addNumber(object, "height", this.height);
            object.addProperty("hud_hidden", this.hudHidden);
            object.add("world", this.world.toJson());
            object.add("player", this.player.toJson());
            object.add("time", this.time.toJson());
            addString(object, "difficulty", this.difficulty);
            addNumber(object, "render_distance_chunks", this.renderDistanceChunks);
            return object;
        }

        @Nullable
        private static ScreenshotMetadata fromJson(@NotNull JsonObject object) {
            JsonObject worldObject = getObject(object, "world");
            JsonObject playerObject = getObject(object, "player");
            JsonObject timeObject = getObject(object, "time");
            if (worldObject == null || playerObject == null || timeObject == null) {
                return null;
            }

            return new ScreenshotMetadata(
                    ScreenshotKind.bySerializedName(getString(object, "kind", "normal")),
                    getString(object, "path", ""),
                    getString(object, "file_name", ""),
                    getLong(object, "captured_at_epoch_millis", 0L),
                    getString(object, "captured_at_iso", ""),
                    getInt(object, "width", 0),
                    getInt(object, "height", 0),
                    getBoolean(object, "hud_hidden", false),
                    WorldInfo.fromJson(worldObject),
                    PlayerInfo.fromJson(playerObject),
                    TimeInfo.fromJson(timeObject),
                    getString(object, "difficulty", ""),
                    getInt(object, "render_distance_chunks", 0)
            );
        }
    }

    public record CaptureContext(
            long capturedAtEpochMillis,
            @NotNull String capturedAtIso,
            @NotNull WorldInfo world,
            @NotNull PlayerInfo player,
            @NotNull TimeInfo time,
            @NotNull String difficulty,
            int renderDistanceChunks,
            boolean hudHidden
    ) {
    }

    public record WorldInfo(
            @NotNull String name,
            @NotNull String type,
            @NotNull String identifier,
            @NotNull String savePath,
            @NotNull String dimension
    ) {

        @NotNull
        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            addString(object, "name", this.name);
            addString(object, "type", this.type);
            addString(object, "identifier", this.identifier);
            addString(object, "save_path", this.savePath);
            addString(object, "dimension", this.dimension);
            return object;
        }

        @NotNull
        private static WorldInfo fromJson(@NotNull JsonObject object) {
            return new WorldInfo(
                    getString(object, "name", ""),
                    getString(object, "type", "unknown"),
                    getString(object, "identifier", ""),
                    getString(object, "save_path", ""),
                    getString(object, "dimension", "")
            );
        }
    }

    public record PlayerInfo(
            double x,
            double y,
            double z,
            int blockX,
            int blockY,
            int blockZ,
            float yaw,
            float pitch,
            @NotNull String gameMode,
            @NotNull String biome
    ) {

        @NotNull
        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            addNumber(object, "x", this.x);
            addNumber(object, "y", this.y);
            addNumber(object, "z", this.z);
            addNumber(object, "block_x", this.blockX);
            addNumber(object, "block_y", this.blockY);
            addNumber(object, "block_z", this.blockZ);
            addNumber(object, "yaw", this.yaw);
            addNumber(object, "pitch", this.pitch);
            addString(object, "game_mode", this.gameMode);
            addString(object, "biome", this.biome);
            return object;
        }

        @NotNull
        private static PlayerInfo fromJson(@NotNull JsonObject object) {
            return new PlayerInfo(
                    getDouble(object, "x", 0.0D),
                    getDouble(object, "y", 0.0D),
                    getDouble(object, "z", 0.0D),
                    getInt(object, "block_x", 0),
                    getInt(object, "block_y", 0),
                    getInt(object, "block_z", 0),
                    getFloat(object, "yaw", 0.0F),
                    getFloat(object, "pitch", 0.0F),
                    getString(object, "game_mode", ""),
                    getString(object, "biome", "")
            );
        }
    }

    public record TimeInfo(
            long gameTime,
            long clockTime,
            long timeOfDay,
            long worldDay
    ) {

        @NotNull
        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            addNumber(object, "game_time", this.gameTime);
            addNumber(object, "clock_time", this.clockTime);
            addNumber(object, "time_of_day", this.timeOfDay);
            addNumber(object, "world_day", this.worldDay);
            return object;
        }

        @NotNull
        private static TimeInfo fromJson(@NotNull JsonObject object) {
            return new TimeInfo(
                    getLong(object, "game_time", 0L),
                    getLong(object, "clock_time", 0L),
                    getLong(object, "time_of_day", 0L),
                    getLong(object, "world_day", 0L)
            );
        }
    }

}
