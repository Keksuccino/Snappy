package de.keksuccino.snappy.storage;

import de.keksuccino.snappy.Snappy;
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

    public static final int PANORAMA_FACE_COUNT = 6;

    private PanoramaScanner() {
    }

    public static boolean isValidPanoramaFolder(@NotNull Path folder) {
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        for (int face = 0; face < PANORAMA_FACE_COUNT; face++) {
            if (!Files.isRegularFile(facePath(folder, face), LinkOption.NOFOLLOW_LINKS)) {
                return false;
            }
        }

        return true;
    }

    @NotNull
    public static List<Path> scanPanoramaFolders(Path @NotNull ... roots) {
        List<Path> panoramas = new ArrayList<>();
        for (Path root : roots) {
            addPanoramaFolders(root, panoramas);
        }
        panoramas.sort(Comparator.comparingLong(PanoramaScanner::panoramaModifiedMillis).thenComparing(Path::toString));
        return List.copyOf(panoramas);
    }

    public static void addPanoramaFolders(@NotNull Path root, @NotNull List<Path> result) {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path path : stream) {
                Path normalized = path.toAbsolutePath().normalize();
                if (isValidPanoramaFolder(normalized) && !result.contains(normalized)) {
                    result.add(normalized);
                }
            }
        } catch (IOException ex) {
            Snappy.getLogger().warn("[SNAPPY] Could not scan panorama folder {}.", root, ex);
        }
    }

    public static long panoramaModifiedMillis(@NotNull Path folder) {
        long modified = lastModifiedMillis(folder);
        for (int face = 0; face < PANORAMA_FACE_COUNT; face++) {
            modified = Math.max(modified, lastModifiedMillis(facePath(folder, face)));
        }
        return modified;
    }

    public static long lastModifiedMillis(@NotNull Path path) {
        try {
            return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    @NotNull
    public static Path facePath(@NotNull Path folder, int face) {
        return folder.resolve("panorama_" + face + ".png");
    }

}
