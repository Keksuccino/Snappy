package de.keksuccino.snappy.screen;

import org.jetbrains.annotations.NotNull;

final class PhotoModeEnvironmentPanel implements PhotoModeTabPanel {

    @Override
    public void addControls(@NotNull PhotoModeScreen screen) {
        screen.addWorldControls();
    }

}
