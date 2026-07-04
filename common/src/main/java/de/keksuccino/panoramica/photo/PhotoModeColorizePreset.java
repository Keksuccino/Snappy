package de.keksuccino.panoramica.photo;

import de.keksuccino.panoramica.Panoramica;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PhotoModeColorizePreset {

    NONE("none", "panoramica.photo_mode.colorize.none", null),
    BLACK_WHITE("black_white", "panoramica.photo_mode.colorize.black_white", "photo_colorize_black_white"),
    BLACK_WHITE_BRIGHT("black_white_bright", "panoramica.photo_mode.colorize.black_white_2", "photo_colorize_black_white_bright"),
    VIBRANT("vibrant", "panoramica.photo_mode.colorize.vibrant", "photo_colorize_vibrant"),
    SEPIA("sepia", "panoramica.photo_mode.colorize.sepia", "photo_colorize_sepia"),
    PALE_BLUE("pale_blue", "panoramica.photo_mode.colorize.pale_blue", "photo_colorize_pale_blue"),
    WARM_GLOW("warm_glow", "panoramica.photo_mode.colorize.warm_glow", "photo_colorize_warm_glow"),
    PINK("pink", "panoramica.photo_mode.colorize.pink", "photo_colorize_pink"),
    SUMMER("summer", "panoramica.photo_mode.colorize.summer", "photo_colorize_summer"),
    VINTAGE("vintage", "panoramica.photo_mode.colorize.vintage", "photo_colorize_vintage");

    private final String id;
    private final String labelKey;
    @Nullable
    private final Identifier postEffectId;

    PhotoModeColorizePreset(@NotNull String id, @NotNull String labelKey, @Nullable String postEffectPath) {
        this.id = id;
        this.labelKey = labelKey;
        this.postEffectId = postEffectPath == null ? null : Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, postEffectPath);
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
