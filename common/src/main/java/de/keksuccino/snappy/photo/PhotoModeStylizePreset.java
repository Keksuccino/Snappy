package de.keksuccino.snappy.photo;

import org.jetbrains.annotations.NotNull;

public enum PhotoModeStylizePreset {

    NONE("none", "snappy.photo_mode.stylize.none", 0),
    COLOR_SKETCH("color_sketch", "snappy.photo_mode.stylize.color_sketch", 1),
    PENCIL_SKETCH("pencil_sketch", "snappy.photo_mode.stylize.pencil_sketch", 2),
    CARTOON("cartoon", "snappy.photo_mode.stylize.cartoon", 3),
    THE_FUUUUUTURE("the_fuuuuuture", "snappy.photo_mode.stylize.the_fuuuuuture", 4);

    private final String id;
    private final String labelKey;
    private final int shaderIndex;

    PhotoModeStylizePreset(@NotNull String id, @NotNull String labelKey, int shaderIndex) {
        this.id = id;
        this.labelKey = labelKey;
        this.shaderIndex = shaderIndex;
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public String labelKey() {
        return this.labelKey;
    }

    public int shaderIndex() {
        return this.shaderIndex;
    }

    public boolean appliesShader() {
        return this.shaderIndex > 0;
    }

    @NotNull
    public PhotoModeStylizePreset next() {
        PhotoModeStylizePreset[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

}
