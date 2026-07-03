package de.keksuccino.panoramica;

import de.keksuccino.panoramica.util.AbstractOptions;
import de.keksuccino.konkrete.config.Config;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class Options extends AbstractOptions {

    protected final Config config = new Config(Panoramica.MOD_DIR.getAbsolutePath().replace("\\", "/") + "/config.txt");

    public final Option<String> screenshotResolution = new Option<>(config, "screenshot_resolution", ResolutionPreset.DEFAULT_1024.id, "capture");
    public final Option<String> menuPanoramaMode = new Option<>(config, "menu_panorama_mode", MenuPanoramaMode.SHOW_LATEST.id, "menu");
    public final Option<String> cycleInterval = new Option<>(config, "cycle_interval", CycleInterval.SECONDS_30.id, "menu");
    public final Option<String> storageLocation = new Option<>(config, "storage_location", StorageLocation.DEDICATED_FOLDER.id, "capture");
    public final Option<Boolean> hideHudInNormalScreenshots = new Option<>(config, "hide_hud_in_normal_screenshots", false, "capture");
    public final Option<String> screenshotPreviewMode = new Option<>(config, "screenshot_preview_mode", ScreenshotPreviewMode.BOTH.id, "preview");
    public final Option<Boolean> screenshotChatMessages = new Option<>(config, "screenshot_chat_messages", true, "notifications");
    public final Option<String> browserSortMode = new Option<>(config, "browser_sort_mode", BrowserSortMode.NEWEST_FIRST.id, "browser");

    public Options() {
        this.config.syncConfig();
        this.config.clearUnusedValues();
    }

    @NotNull
    public ResolutionPreset getScreenshotResolution() {
        return ResolutionPreset.byId(this.screenshotResolution.getValue(), this.screenshotResolution);
    }

    public void setScreenshotResolution(@NotNull ResolutionPreset preset) {
        this.screenshotResolution.setValue(preset.id);
    }

    @NotNull
    public MenuPanoramaMode getMenuPanoramaMode() {
        return MenuPanoramaMode.byId(this.menuPanoramaMode.getValue(), this.menuPanoramaMode);
    }

    public void setMenuPanoramaMode(@NotNull MenuPanoramaMode mode) {
        this.menuPanoramaMode.setValue(mode.id);
    }

    @NotNull
    public CycleInterval getCycleInterval() {
        return CycleInterval.byId(this.cycleInterval.getValue(), this.cycleInterval);
    }

    public void setCycleInterval(@NotNull CycleInterval interval) {
        this.cycleInterval.setValue(interval.id);
    }

    @NotNull
    public StorageLocation getStorageLocation() {
        return StorageLocation.byId(this.storageLocation.getValue(), this.storageLocation);
    }

    public void setStorageLocation(@NotNull StorageLocation location) {
        this.storageLocation.setValue(location.id);
    }

    public boolean shouldHideHudInNormalScreenshots() {
        return this.hideHudInNormalScreenshots.getValue();
    }

    public void setHideHudInNormalScreenshots(boolean enabled) {
        this.hideHudInNormalScreenshots.setValue(enabled);
    }

    @NotNull
    public ScreenshotPreviewMode getScreenshotPreviewMode() {
        return ScreenshotPreviewMode.byId(this.screenshotPreviewMode.getValue(), this.screenshotPreviewMode);
    }

    public void setScreenshotPreviewMode(@NotNull ScreenshotPreviewMode mode) {
        this.screenshotPreviewMode.setValue(mode.id);
    }

    public boolean areScreenshotChatMessagesEnabled() {
        return this.screenshotChatMessages.getValue();
    }

    public void setScreenshotChatMessagesEnabled(boolean enabled) {
        this.screenshotChatMessages.setValue(enabled);
    }

    @NotNull
    public BrowserSortMode getBrowserSortMode() {
        return BrowserSortMode.byId(this.browserSortMode.getValue(), this.browserSortMode);
    }

    public void setBrowserSortMode(@NotNull BrowserSortMode mode) {
        this.browserSortMode.setValue(mode.id);
    }

    public enum ResolutionPreset {
        LOW_256("low_256", 256),
        BALANCED_512("balanced_512", 512),
        DEFAULT_1024("default_1024", 1024),
        VERY_HIGH_1536("very_high_1536", 1536),
        ULTRA_2048("ultra_2048", 2048);

        public final String id;
        public final int sideSize;

        ResolutionPreset(@NotNull String id, int sideSize) {
            this.id = id;
            this.sideSize = sideSize;
        }

        @NotNull
        public ResolutionPreset next() {
            ResolutionPreset[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        @NotNull
        public String labelKey() {
            return "panoramica.options.resolution." + this.id;
        }

        @NotNull
        public String tooltipKey() {
            return this.labelKey() + ".desc";
        }

        @NotNull
        private static ResolutionPreset byId(@NotNull String id, @NotNull Option<String> option) {
            if ("vanilla_256".equals(normalize(id))) {
                option.setValue(LOW_256.id);
                return LOW_256;
            }
            for (ResolutionPreset preset : values()) {
                if (preset.id.equals(normalize(id))) return preset;
            }
            option.setValue(DEFAULT_1024.id);
            return DEFAULT_1024;
        }
    }

    public enum MenuPanoramaMode {
        CYCLE_ALL("cycle_all"),
        SHOW_LATEST("show_latest"),
        SHOW_SELECTED("show_selected"),
        SHOW_VANILLA("show_vanilla");

        public final String id;

        MenuPanoramaMode(@NotNull String id) {
            this.id = id;
        }

        @NotNull
        public MenuPanoramaMode next() {
            MenuPanoramaMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        @NotNull
        public String labelKey() {
            return "panoramica.options.menu_mode." + this.id;
        }

        public boolean usesCycleInterval() {
            return this == CYCLE_ALL || this == SHOW_SELECTED;
        }

        @NotNull
        private static MenuPanoramaMode byId(@NotNull String id, @NotNull Option<String> option) {
            for (MenuPanoramaMode mode : values()) {
                if (mode.id.equals(normalize(id))) return mode;
            }
            option.setValue(SHOW_LATEST.id);
            return SHOW_LATEST;
        }
    }

    public enum CycleInterval {
        SECONDS_5("5_seconds", 5),
        SECONDS_10("10_seconds", 10),
        SECONDS_15("15_seconds", 15),
        SECONDS_30("30_seconds", 30),
        SECONDS_60("60_seconds", 60),
        MINUTES_5("5_minutes", 5 * 60),
        MINUTES_30("30_minutes", 30 * 60);

        public final String id;
        public final int seconds;

        CycleInterval(@NotNull String id, int seconds) {
            this.id = id;
            this.seconds = seconds;
        }

        @NotNull
        public CycleInterval next() {
            CycleInterval[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        @NotNull
        public String labelKey() {
            return "panoramica.options.cycle_interval." + this.id;
        }

        @NotNull
        private static CycleInterval byId(@NotNull String id, @NotNull Option<String> option) {
            for (CycleInterval interval : values()) {
                if (interval.id.equals(normalize(id))) return interval;
            }
            option.setValue(SECONDS_30.id);
            return SECONDS_30;
        }
    }

    public enum StorageLocation {
        DEDICATED_FOLDER("dedicated_folder"),
        SCREENSHOTS_FOLDER("screenshots_folder");

        public final String id;

        StorageLocation(@NotNull String id) {
            this.id = id;
        }

        @NotNull
        public StorageLocation next() {
            StorageLocation[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        @NotNull
        public String labelKey() {
            return "panoramica.options.storage." + this.id;
        }

        @NotNull
        private static StorageLocation byId(@NotNull String id, @NotNull Option<String> option) {
            for (StorageLocation location : values()) {
                if (location.id.equals(normalize(id))) return location;
            }
            option.setValue(DEDICATED_FOLDER.id);
            return DEDICATED_FOLDER;
        }
    }

    public enum ScreenshotPreviewMode {
        BOTH("both", true, true),
        PANORAMAS_ONLY("panoramas_only", true, false),
        NORMAL_ONLY("normal_only", false, true),
        DISABLED("disabled", false, false);

        public final String id;
        public final boolean showPanoramaScreenshots;
        public final boolean showNormalScreenshots;

        ScreenshotPreviewMode(@NotNull String id, boolean showPanoramaScreenshots, boolean showNormalScreenshots) {
            this.id = id;
            this.showPanoramaScreenshots = showPanoramaScreenshots;
            this.showNormalScreenshots = showNormalScreenshots;
        }

        @NotNull
        public ScreenshotPreviewMode next() {
            ScreenshotPreviewMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        @NotNull
        public String labelKey() {
            return "panoramica.options.preview_mode." + this.id;
        }

        @NotNull
        private static ScreenshotPreviewMode byId(@NotNull String id, @NotNull Option<String> option) {
            for (ScreenshotPreviewMode mode : values()) {
                if (mode.id.equals(normalize(id))) return mode;
            }
            option.setValue(BOTH.id);
            return BOTH;
        }
    }

    public enum BrowserSortMode {
        NEWEST_FIRST("newest_first"),
        OLDEST_FIRST("oldest_first"),
        BY_TYPE("by_type");

        public final String id;

        BrowserSortMode(@NotNull String id) {
            this.id = id;
        }

        @NotNull
        public BrowserSortMode next() {
            BrowserSortMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        @NotNull
        public String labelKey() {
            return "panoramica.browser.sort." + this.id;
        }

        @NotNull
        private static BrowserSortMode byId(@NotNull String id, @NotNull Option<String> option) {
            for (BrowserSortMode mode : values()) {
                if (mode.id.equals(normalize(id))) return mode;
            }
            option.setValue(NEWEST_FIRST.id);
            return NEWEST_FIRST;
        }
    }

    @NotNull
    private static String normalize(@NotNull String id) {
        return id.toLowerCase(Locale.ROOT);
    }

}
