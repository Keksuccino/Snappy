package de.keksuccino.snappy.util.file;

import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ExternalFileOpener {

    private static final Logger LOGGER = LogManager.getLogger();

    private ExternalFileOpener() {
    }

    public static void openPath(@NotNull Path path) {
        Util.getPlatform().openPath(path.toAbsolutePath().normalize());
    }

    public static void revealFile(@NotNull Path path) {
        Path absolutePath = path.toAbsolutePath().normalize();
        if (!revealFileWithNativeCommand(absolutePath)) {
            openParentPath(absolutePath);
        }
    }

    private static boolean revealFileWithNativeCommand(@NotNull Path path) {
        List<String> command = switch (Util.getPlatform()) {
            case OSX -> List.of("/usr/bin/open", "-R", path.toString());
            case WINDOWS -> List.of("explorer.exe", "/select," + path);
            default -> null;
        };

        if (command == null) {
            return false;
        }

        try {
            startProcess(command);
            return true;
        } catch (IOException ex) {
            LOGGER.warn("[SNAPPY] Could not reveal file {}.", path, ex);
            return false;
        }
    }

    private static void openParentPath(@NotNull Path path) {
        Path parent = path.getParent();
        openPath(parent == null ? path : parent);
    }

    private static void startProcess(@NotNull List<String> command) throws IOException {
        Process process = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }
    }

}
