package de.keksuccino.snappy.screen;

import org.jetbrains.annotations.NotNull;

final class PhotoModeGeneralTabPanel implements PhotoModeTabPanel {

    @Override
    public void addControls(@NotNull PhotoModeScreen screen) {
        screen.addGeneralControls();
    }

}
