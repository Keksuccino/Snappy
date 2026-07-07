package de.keksuccino.snappy.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PanoramaScanner {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final int PANORAMA_FACE_COUNT = 6;

    private PanoramaScanner() {
    }

    public static boolean isValidPanoramaFolder(@NotNull Path folder) {
        return isValidPanoramaFolder(folder, LinkPolicy.NOFOLLOW_LINKS);
    }

    public static boolean isValidPanoramaFolder(@NotNull Path folder, @NotNull LinkPolicy linkPolicy) {
        if (!Files.isDirectory(folder, linkPolicy.options())) {
            return false;
        }

        for (int face = 0; face < PANORAMA_FACE_COUNT; face++) {
            if (!Files.isRegularFile(facePath(folder, face), linkPolicy.options())) {
                return false;
            }
        }

        return true;
    }

    @NotNull
    public static List<Path> scanPanoramaFolders(Path @NotNull ... roots) {
        return scanPanoramaFolders(LinkPolicy.NOFOLLOW_LINKS, ModifiedTimePolicy.FOLDER_AND_FACES, roots);
    }

    @NotNull
    public static List<Path> scanPanoramaFolders(
            @NotNull LinkPolicy linkPolicy,
            @NotNull ModifiedTimePolicy modifiedTimePolicy,
            Path @NotNull ... roots
    ) {
        List<Path> panoramas = new ArrayList<>();
        for (Path root : roots) {
            addPanoramaFolders(root, panoramas, linkPolicy);
        }
        panoramas.sort(Comparator.comparingLong((Path path) -> panoramaModifiedMillis(path, linkPolicy, modifiedTimePolicy)).thenComparing(Path::toString));
        return List.copyOf(panoramas);
    }

    public static void addPanoramaFolders(@NotNull Path root, @NotNull List<Path> result) {
        addPanoramaFolders(root, result, LinkPolicy.NOFOLLOW_LINKS);
    }

    public static void addPanoramaFolders(@NotNull Path root, @NotNull List<Path> result, @NotNull LinkPolicy linkPolicy) {
        if (!Files.isDirectory(root, linkPolicy.options())) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path path : stream) {
                Path normalized = path.toAbsolutePath().normalize();
                if (isValidPanoramaFolder(normalized, linkPolicy) && !result.contains(normalized)) {
                    result.add(normalized);
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("[SNAPPY] Could not scan panorama folder {}.", root, ex);
        }
    }

    public static long panoramaModifiedMillis(@NotNull Path folder) {
        return panoramaModifiedMillis(folder, LinkPolicy.NOFOLLOW_LINKS, ModifiedTimePolicy.FOLDER_AND_FACES);
    }

    public static long panoramaModifiedMillis(
            @NotNull Path folder,
            @NotNull LinkPolicy linkPolicy,
            @NotNull ModifiedTimePolicy modifiedTimePolicy
    ) {
        long modified = lastModifiedMillis(folder, linkPolicy);
        if (modifiedTimePolicy == ModifiedTimePolicy.FOLDER_ONLY) {
            return modified;
        }
        for (int face = 0; face < PANORAMA_FACE_COUNT; face++) {
            modified = Math.max(modified, lastModifiedMillis(facePath(folder, face), linkPolicy));
        }
        return modified;
    }

    public static long lastModifiedMillis(@NotNull Path path) {
        return lastModifiedMillis(path, LinkPolicy.NOFOLLOW_LINKS);
    }

    public static long lastModifiedMillis(@NotNull Path path, @NotNull LinkPolicy linkPolicy) {
        try {
            return Files.getLastModifiedTime(path, linkPolicy.options()).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    @NotNull
    public static Path facePath(@NotNull Path folder, int face) {
        return folder.resolve("panorama_" + face + ".png");
    }

    public enum LinkPolicy {
        FOLLOW_LINKS,
        NOFOLLOW_LINKS(LinkOption.NOFOLLOW_LINKS);

        private final LinkOption[] options;

        LinkPolicy(LinkOption... options) {
            this.options = options;
        }

        private LinkOption[] options() {
            return this.options;
        }
    }

    public enum ModifiedTimePolicy {
        FOLDER_ONLY,
        FOLDER_AND_FACES
    }

}
