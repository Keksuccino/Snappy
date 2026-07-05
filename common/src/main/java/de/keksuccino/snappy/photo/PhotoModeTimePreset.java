package de.keksuccino.snappy.photo;

import org.jetbrains.annotations.NotNull;

public enum PhotoModeTimePreset {

    MORNING("morning", "snappy.photo_mode.time.morning", 1_000L),
    NOON("noon", "snappy.photo_mode.time.noon", 6_000L),
    EVENING("evening", "snappy.photo_mode.time.evening", 12_000L),
    NIGHT("night", "snappy.photo_mode.time.night", 18_000L);

    private final String id;
    private final String labelKey;
    private final long clockTicks;

    PhotoModeTimePreset(@NotNull String id, @NotNull String labelKey, long clockTicks) {
        this.id = id;
        this.labelKey = labelKey;
        this.clockTicks = clockTicks;
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public String labelKey() {
        return this.labelKey;
    }

    public long clockTicks() {
        return this.clockTicks;
    }

    @NotNull
    public PhotoModeTimePreset next() {
        PhotoModeTimePreset[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

}
