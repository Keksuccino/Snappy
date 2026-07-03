package de.keksuccino.panoramica.screen;

import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.capture.PanoramaCaptureManager;
import de.keksuccino.panoramica.menu.MenuBackgroundSelectionManager;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ScreenshotBrowserCatalog {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ScreenshotBrowserCatalog() {
    }

    @NotNull
    public static List<ScreenshotEntry> scan(@NotNull Minecraft minecraft) {
        Path gameDirectory = minecraft.gameDirectory.toPath();
        Path screenshotsDirectory = gameDirectory.resolve(Screenshot.SCREENSHOT_DIR);
        Path panoramaDirectory = gameDirectory.resolve(PanoramaCaptureManager.DEDICATED_SCREENSHOT_DIR);
        Set<Path> seenPaths = new HashSet<>();
        List<ScreenshotEntry> entries = new ArrayList<>();

        addNormalScreenshots(screenshotsDirectory, seenPaths, entries);
        addPanoramaFolders(panoramaDirectory, seenPaths, entries);
        addPanoramaFolders(screenshotsDirectory, seenPaths, entries);

        entries.sort(Comparator.comparingLong(ScreenshotEntry::modifiedMillis).reversed().thenComparing(entry -> entry.path().toString()));
        return List.copyOf(entries);
    }

    @NotNull
    public static DeletionResult deleteAll(@NotNull List<ScreenshotEntry> entries) {
        int deleted = 0;
        int failed = 0;
        List<Path> deletedPaths = new ArrayList<>();

        for (ScreenshotEntry entry : entries) {
            try {
                entry.delete();
                deletedPaths.add(entry.path());
                deleted++;
            } catch (IOException ex) {
                failed++;
                Panoramica.getLogger().warn("[PANORAMICA] Could not delete screenshot {}.", entry.path(), ex);
            }
        }

        if (!deletedPaths.isEmpty()) {
            ScreenshotMetadataManager.removeAll(deletedPaths);
            MenuBackgroundSelectionManager.removeAll(deletedPaths);
        }

        return new DeletionResult(deleted, failed);
    }

    private static void addNormalScreenshots(@NotNull Path root, @NotNull Set<Path> seenPaths, @NotNull List<ScreenshotEntry> entries) {
        if (!Files.isDirectory(root)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.png")) {
            for (Path path : stream) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }

                Path normalized = normalize(path);
                if (seenPaths.add(normalized)) {
                    long modified = lastModifiedMillis(path);
                    entries.add(new ScreenshotEntry(ScreenshotEntry.Kind.NORMAL, normalized, path.getFileName().toString(), modified, formatDate(modified)));
                }
            }
        } catch (IOException ex) {
            Panoramica.getLogger().warn("[PANORAMICA] Could not scan normal screenshot folder {}.", root, ex);
        }
    }

    private static void addPanoramaFolders(@NotNull Path root, @NotNull Set<Path> seenPaths, @NotNull List<ScreenshotEntry> entries) {
        if (!Files.isDirectory(root)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path path : stream) {
                if (!isValidPanoramaFolder(path)) {
                    continue;
                }

                Path normalized = normalize(path);
                if (seenPaths.add(normalized)) {
                    long modified = panoramaModifiedMillis(path);
                    entries.add(new ScreenshotEntry(ScreenshotEntry.Kind.PANORAMA, normalized, path.getFileName().toString(), modified, formatDate(modified)));
                }
            }
        } catch (IOException ex) {
            Panoramica.getLogger().warn("[PANORAMICA] Could not scan panorama screenshot folder {}.", root, ex);
        }
    }

    private static boolean isValidPanoramaFolder(@NotNull Path folder) {
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        for (int i = 0; i < 6; i++) {
            if (!Files.isRegularFile(folder.resolve("panorama_" + i + ".png"), LinkOption.NOFOLLOW_LINKS)) {
                return false;
            }
        }

        return true;
    }

    private static long panoramaModifiedMillis(@NotNull Path folder) {
        long modified = lastModifiedMillis(folder);
        for (int i = 0; i < 6; i++) {
            modified = Math.max(modified, lastModifiedMillis(folder.resolve("panorama_" + i + ".png")));
        }
        return modified;
    }

    private static long lastModifiedMillis(@NotNull Path path) {
        try {
            return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    @NotNull
    private static Path normalize(@NotNull Path path) {
        return path.toAbsolutePath().normalize();
    }

    @NotNull
    private static String formatDate(long modifiedMillis) {
        if (modifiedMillis <= 0L) {
            return "";
        }
        return DATE_FORMAT.format(Instant.ofEpochMilli(modifiedMillis).atZone(ZoneId.systemDefault()));
    }

    public record DeletionResult(int deleted, int failed) {
    }

    public record ScreenshotEntry(
            @NotNull Kind kind,
            @NotNull Path path,
            @NotNull String displayName,
            long modifiedMillis,
            @NotNull String formattedDate
    ) {

        public boolean isPanorama() {
            return this.kind == Kind.PANORAMA;
        }

        @NotNull
        public Path thumbnailPath() {
            return this.isPanorama() ? this.path.resolve("panorama_0.png") : this.path;
        }

        @NotNull
        public Path outsidePath() {
            return this.path;
        }

        public void delete() throws IOException {
            if (this.isPanorama()) {
                deleteDirectory(this.path);
            } else {
                Files.deleteIfExists(this.path);
            }
        }

        @NotNull
        public String typeLabelKey() {
            return this.isPanorama() ? "panoramica.browser.type.panorama" : "panoramica.browser.type.normal";
        }

        @NotNull
        public String lowerCaseDisplayName() {
            return this.displayName.toLowerCase(Locale.ROOT);
        }

        private static void deleteDirectory(@NotNull Path root) throws IOException {
            if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }

            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        public enum Kind {
            NORMAL,
            PANORAMA
        }
    }

}
