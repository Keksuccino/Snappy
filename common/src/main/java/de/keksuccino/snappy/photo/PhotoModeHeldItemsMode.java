package de.keksuccino.snappy.photo;

import org.jetbrains.annotations.NotNull;

public enum PhotoModeHeldItemsMode {

    SHOW_BOTH_HANDS("snappy.photo_mode.held_items.show_both_hands", true, true),
    SHOW_ONLY_MAIN_HAND("snappy.photo_mode.held_items.show_only_main_hand", true, false),
    SHOW_ONLY_OFF_HAND("snappy.photo_mode.held_items.show_only_off_hand", false, true);

    private final String labelKey;
    private final boolean showMainHand;
    private final boolean showOffHand;

    PhotoModeHeldItemsMode(@NotNull String labelKey, boolean showMainHand, boolean showOffHand) {
        this.labelKey = labelKey;
        this.showMainHand = showMainHand;
        this.showOffHand = showOffHand;
    }

    @NotNull
    public String labelKey() {
        return this.labelKey;
    }

    public boolean showMainHand() {
        return this.showMainHand;
    }

    public boolean showOffHand() {
        return this.showOffHand;
    }

    @NotNull
    public PhotoModeHeldItemsMode next() {
        PhotoModeHeldItemsMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

}
