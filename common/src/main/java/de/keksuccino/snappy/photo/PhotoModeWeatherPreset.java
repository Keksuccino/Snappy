package de.keksuccino.snappy.photo;

import org.jetbrains.annotations.NotNull;

public enum PhotoModeWeatherPreset {

    SUNNY("sunny", "snappy.photo_mode.weather.sunny", 0.0F, 0.0F),
    RAINY("rainy", "snappy.photo_mode.weather.rainy", 1.0F, 0.0F),
    THUNDERING("thundering", "snappy.photo_mode.weather.thundering", 1.0F, 1.0F);

    private final String id;
    private final String labelKey;
    private final float rainLevel;
    private final float thunderLevel;

    PhotoModeWeatherPreset(@NotNull String id, @NotNull String labelKey, float rainLevel, float thunderLevel) {
        this.id = id;
        this.labelKey = labelKey;
        this.rainLevel = rainLevel;
        this.thunderLevel = thunderLevel;
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public String labelKey() {
        return this.labelKey;
    }

    public float rainLevel() {
        return this.rainLevel;
    }

    public float thunderLevel() {
        return this.thunderLevel;
    }

    @NotNull
    public PhotoModeWeatherPreset next() {
        PhotoModeWeatherPreset[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

}
