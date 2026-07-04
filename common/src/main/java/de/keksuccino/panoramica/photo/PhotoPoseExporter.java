package de.keksuccino.panoramica.photo;

import de.keksuccino.panoramica.Panoramica;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

    private static final String JSON_EXTENSION = ".json";

    private PhotoPoseExporter() {
    }

    public static void saveWithNativeDialog(@NotNull Minecraft minecraft, @NotNull PhotoPose pose) {
        @Nullable Path selectedPath = chooseSavePath(minecraft, pose);
        if (selectedPath == null) {
            minecraft.showDebugChat(Component.translatable("panoramica.photo_mode.pose_maker.save.cancelled"));
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
            minecraft.showDebugChat(Component.translatable("panoramica.photo_mode.pose_maker.save.success", targetPath.toAbsolutePath().toString()));
        } catch (IOException ex) {
            Panoramica.getLogger().warn("[PANORAMICA] Could not save photo pose '{}'.", targetPath, ex);
            minecraft.showDebugChat(Component.translatable("panoramica.photo_mode.pose_maker.save.failure", ex.getMessage()));
        }
    }

    @Nullable
    private static Path chooseSavePath(@NotNull Minecraft minecraft, @NotNull PhotoPose pose) {
        String defaultPath = minecraft.gameDirectory.toPath()
                .resolve(suggestedFileName(pose.nameKey()))
                .toAbsolutePath()
                .toString();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*" + JSON_EXTENSION));
            filters.flip();

            @Nullable String selected = TinyFileDialogs.tinyfd_saveFileDialog(
                    Component.translatable("panoramica.photo_mode.pose_maker.save_dialog").getString(),
                    defaultPath,
                    filters,
                    Component.translatable("panoramica.photo_mode.pose_maker.json_files").getString()
            );
            return selected == null || selected.isBlank() ? null : Path.of(selected);
        }
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
