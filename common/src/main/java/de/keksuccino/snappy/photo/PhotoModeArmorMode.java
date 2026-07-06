package de.keksuccino.snappy.photo;

import org.jetbrains.annotations.NotNull;

public enum PhotoModeArmorMode {

    SHOW_ALL("snappy.photo_mode.armor.show_all", false, false),
    HIDE_HAT("snappy.photo_mode.armor.hide_hat", true, false),
    HIDE_ALL("snappy.photo_mode.armor.hide_all", true, true);

    private final String labelKey;
    private final boolean hideHeadSlot;
    private final boolean hideBodySlots;

    PhotoModeArmorMode(@NotNull String labelKey, boolean hideHeadSlot, boolean hideBodySlots) {
        this.labelKey = labelKey;
        this.hideHeadSlot = hideHeadSlot;
        this.hideBodySlots = hideBodySlots;
    }

    @NotNull
    public String labelKey() {
        return this.labelKey;
    }

    public boolean hideHeadSlot() {
        return this.hideHeadSlot;
    }

    public boolean hideBodySlots() {
        return this.hideBodySlots;
    }

    @NotNull
    public PhotoModeArmorMode next() {
        PhotoModeArmorMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

}
