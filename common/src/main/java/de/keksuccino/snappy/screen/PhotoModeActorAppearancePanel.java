package de.keksuccino.snappy.screen;

import org.jetbrains.annotations.NotNull;

final class PhotoModeActorAppearancePanel implements PhotoModeTabPanel {

    @Override
    public void addControls(@NotNull PhotoModeScreen screen) {
        screen.addPlayerControls();
    }

}
