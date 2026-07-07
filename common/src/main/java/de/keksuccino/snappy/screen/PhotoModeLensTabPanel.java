package de.keksuccino.snappy.screen;

import org.jetbrains.annotations.NotNull;

final class PhotoModeLensTabPanel implements PhotoModeTabPanel {

    @Override
    public void addControls(@NotNull PhotoModeScreen screen) {
        screen.addLensControls();
    }

}
