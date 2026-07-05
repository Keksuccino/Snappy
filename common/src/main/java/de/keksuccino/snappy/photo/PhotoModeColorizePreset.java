package de.keksuccino.snappy.photo;

import de.keksuccino.snappy.Snappy;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PhotoModeColorizePreset {

    NONE("none", "snappy.photo_mode.colorize.none", null),
    BLACK_WHITE("black_white", "snappy.photo_mode.colorize.black_white", "photo_colorize_black_white"),
    BLACK_WHITE_BRIGHT("black_white_bright", "snappy.photo_mode.colorize.black_white_2", "photo_colorize_black_white_bright"),
    VIBRANT("vibrant", "snappy.photo_mode.colorize.vibrant", "photo_colorize_vibrant"),
    SEPIA("sepia", "snappy.photo_mode.colorize.sepia", "photo_colorize_sepia"),
    PALE_BLUE("pale_blue", "snappy.photo_mode.colorize.pale_blue", "photo_colorize_pale_blue"),
    WARM_GLOW("warm_glow", "snappy.photo_mode.colorize.warm_glow", "photo_colorize_warm_glow"),
    PINK("pink", "snappy.photo_mode.colorize.pink", "photo_colorize_pink"),
    SUMMER("summer", "snappy.photo_mode.colorize.summer", "photo_colorize_summer"),
    BLUE_ORANGE("blue_orange", "snappy.photo_mode.colorize.blue_orange", "photo_colorize_blue_orange"),
    VINTAGE("vintage", "snappy.photo_mode.colorize.vintage", "photo_colorize_vintage");

    private final String id;
    private final String labelKey;
    @Nullable
    private final Identifier postEffectId;

    PhotoModeColorizePreset(@NotNull String id, @NotNull String labelKey, @Nullable String postEffectPath) {
        this.id = id;
        this.labelKey = labelKey;
        this.postEffectId = postEffectPath == null ? null : Identifier.fromNamespaceAndPath(Snappy.MOD_ID, postEffectPath);
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public String labelKey() {
        return this.labelKey;
    }

    @Nullable
    public Identifier postEffectId() {
        return this.postEffectId;
    }

    @NotNull
    public PhotoModeColorizePreset next() {
        PhotoModeColorizePreset[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

}
