package de.keksuccino.snappy.photo;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class PhotoPoseExporter {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String JSON_EXTENSION = ".json";
    private static final String JSON_FILE_FILTER_PATTERN = "*" + JSON_EXTENSION;
    private static final String JSON_UTI_FILTER_PATTERN = "*.public.json";

    private PhotoPoseExporter() {
    }

    public static void saveWithNativeDialog(@NotNull Minecraft minecraft, @NotNull PhotoPose pose) {
        @Nullable Path selectedPath = chooseSavePath(minecraft, pose);
        if (selectedPath == null) {
            minecraft.showDebugChat(Component.translatable("snappy.photo_mode.pose_maker.save.cancelled"));
            return;
        }

        Path targetPath = ensureJsonExtension(selectedPath);
        try {
            Path parent = targetPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
                writer.write(PhotoPoseManager.toJsonString(pose));
                writer.newLine();
            }
            minecraft.showDebugChat(Component.translatable("snappy.photo_mode.pose_maker.save.success", targetPath.toAbsolutePath().toString()));
        } catch (IOException ex) {
            LOGGER.warn("[SNAPPY] Could not save photo pose '{}'.", targetPath, ex);
            minecraft.showDebugChat(Component.translatable("snappy.photo_mode.pose_maker.save.failure", ex.getMessage()));
        }
    }

    @Nullable
    public static PhotoPose loadWithNativeDialog(@NotNull Minecraft minecraft) {
        @Nullable Path selectedPath = chooseOpenPath(minecraft);
        if (selectedPath == null) {
            minecraft.showDebugChat(Component.translatable("snappy.photo_mode.pose_maker.load.cancelled"));
            return null;
        }

        try {
            PhotoPose pose = PhotoPoseManager.fromJsonString(Files.readString(selectedPath, StandardCharsets.UTF_8));
            minecraft.showDebugChat(Component.translatable("snappy.photo_mode.pose_maker.load.success", selectedPath.toAbsolutePath().toString()));
            return pose;
        } catch (Exception ex) {
            LOGGER.warn("[SNAPPY] Could not load photo pose '{}'.", selectedPath, ex);
            minecraft.showDebugChat(Component.translatable("snappy.photo_mode.pose_maker.load.failure", ex.getMessage()));
            return null;
        }
    }

    @Nullable
    private static Path chooseSavePath(@NotNull Minecraft minecraft, @NotNull PhotoPose pose) {
        String defaultPath = minecraft.gameDirectory.toPath()
                .resolve(suggestedFileName(pose.nameKey()))
                .toAbsolutePath()
                .toString();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            @Nullable String selected = TinyFileDialogs.tinyfd_saveFileDialog(
                    Component.translatable("snappy.photo_mode.pose_maker.save_dialog").getString(),
                    defaultPath,
                    jsonFilterPatterns(stack),
                    Component.translatable("snappy.photo_mode.pose_maker.json_files").getString()
            );
            return selected == null || selected.isBlank() ? null : Path.of(selected);
        }
    }

    @Nullable
    private static Path chooseOpenPath(@NotNull Minecraft minecraft) {
        String defaultPath = minecraft.gameDirectory.toPath().toAbsolutePath().toString();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            @Nullable String selected = TinyFileDialogs.tinyfd_openFileDialog(
                    Component.translatable("snappy.photo_mode.pose_maker.load_dialog").getString(),
                    defaultPath,
                    jsonFilterPatterns(stack),
                    Component.translatable("snappy.photo_mode.pose_maker.json_files").getString(),
                    false
            );
            return selected == null || selected.isBlank() ? null : Path.of(selected);
        }
    }

    @NotNull
    private static PointerBuffer jsonFilterPatterns(@NotNull MemoryStack stack) {
        PointerBuffer filters = stack.mallocPointer(2);
        filters.put(stack.UTF8(JSON_FILE_FILTER_PATTERN));
        filters.put(stack.UTF8(JSON_UTI_FILTER_PATTERN));
        filters.flip();
        return filters;
    }

    @NotNull
    private static Path ensureJsonExtension(@NotNull Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(JSON_EXTENSION)) {
            return path;
        }
        return path.resolveSibling(fileName + JSON_EXTENSION);
    }

    @NotNull
    private static String suggestedFileName(@NotNull String nameKey) {
        String trimmed = nameKey.trim();
        int lastDot = trimmed.lastIndexOf('.');
        String baseName = lastDot >= 0 && lastDot + 1 < trimmed.length() ? trimmed.substring(lastDot + 1) : trimmed;
        StringBuilder sanitized = new StringBuilder();
        for (int i = 0; i < baseName.length(); i++) {
            char character = baseName.charAt(i);
            if (character >= 'A' && character <= 'Z') {
                sanitized.append((char) (character + 32));
            } else if ((character >= 'a' && character <= 'z') || (character >= '0' && character <= '9') || character == '_' || character == '-') {
                sanitized.append(character);
            } else {
                sanitized.append('_');
            }
        }
        if (sanitized.isEmpty()) {
            sanitized.append("photo_pose");
        }
        return sanitized + JSON_EXTENSION;
    }

}
