package de.keksuccino.snappy.photo;

import org.jetbrains.annotations.NotNull;

public enum PhotoModeSeason {

    NONE("none", "snappy.photo_mode.season.none"),
    SUMMER("summer", "snappy.photo_mode.season.summer"),
    AUTUMN("autumn", "snappy.photo_mode.season.autumn"),
    WINTER("winter", "snappy.photo_mode.season.winter");

    private final String id;
    private final String labelKey;

    PhotoModeSeason(@NotNull String id, @NotNull String labelKey) {
        this.id = id;
        this.labelKey = labelKey;
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public String labelKey() {
        return this.labelKey;
    }

    public boolean removesSnow() {
        return this == SUMMER || this == AUTUMN;
    }

    public boolean addsSnow() {
        return this == WINTER;
    }

    public boolean hasAutumnFoliage() {
        return this == AUTUMN;
    }

    public boolean hasWinterTint() {
        return this == WINTER;
    }

    public boolean hasVisualOverrides() {
        return this != NONE;
    }

    @NotNull
    public PhotoModeSeason next() {
        PhotoModeSeason[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

}
