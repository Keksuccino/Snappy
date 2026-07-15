package de.keksuccino.snappy;

import de.keksuccino.snappy.platform.Services;
import de.keksuccino.snappy.util.file.GameDirectoryUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.File;

public class Snappy {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String VERSION = "1.0.0";
    public static final String LOADER = Services.PLATFORM.getPlatformName().toUpperCase();
    public static final String MOD_ID = "snappy";
    public static final File MOD_DIR = createDirectory(new File(GameDirectoryUtils.getGameDirectory(), "/config/snappy"));
    public static final File SNAPSHOTS_DIR = createDirectory(new File(GameDirectoryUtils.getGameDirectory(), "/snapshots"));

    private static Options options;

    public static void init() {

        if (Services.PLATFORM.isOnClient()) {

            LOGGER.info("[SNAPPY] Starting version " + VERSION + " in CLIENT-SIDE mode on " + Services.PLATFORM.getPlatformDisplayName().toUpperCase() + "..");

        } else {

            LOGGER.info("[SNAPPY] Starting version " + VERSION + " in SERVER-SIDE mode on " + Services.PLATFORM.getPlatformDisplayName().toUpperCase() + "..");

        }

    }

    public static void updateOptions() {
        options = new Options();
    }

    @NotNull
    public static Options getOptions() {
        if (options == null) updateOptions();
        return options;
    }

    private static File createDirectory(@NotNull File file) {
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        return file;
    }

}
