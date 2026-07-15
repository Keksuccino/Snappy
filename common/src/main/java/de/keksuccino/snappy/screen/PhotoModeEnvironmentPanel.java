package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.photo.PhotoModeManager;
import de.keksuccino.snappy.util.rendering.gui.widget.AdvancedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

final class PhotoModeEnvironmentPanel implements PhotoModeTabPanel {

    @Override
    public void addControls(@NotNull PhotoModeScreen screen) {
        int width = screen.tabControlWidth();
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }

        AdvancedButton pauseButton = new AdvancedButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            PhotoModeManager.togglePaused(Minecraft.getInstance());
            screen.updateButtonMessages();
        });
        pauseButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.pause.desc")));
        screen.pauseButton = screen.addTabControl(pauseButton);

        AdvancedButton timeButton = new AdvancedButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setTimePreset(active.timePreset().next());
            screen.updateButtonMessages();
        });
        timeButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.time.desc")));
        screen.timeButton = screen.addTabControl(timeButton);

        AdvancedButton weatherButton = new AdvancedButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setWeatherPreset(active.weatherPreset().next());
            screen.updateButtonMessages();
        });
        weatherButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.weather.desc")));
        screen.weatherButton = screen.addTabControl(weatherButton);

        AdvancedButton forceBiomePrecipitationButton = new AdvancedButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setForceBiomePrecipitation(!active.forceBiomePrecipitation());
            screen.updateButtonMessages();
        });
        forceBiomePrecipitationButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.force_biome_precipitation.desc")));
        screen.forceBiomePrecipitationButton = screen.addTabControl(forceBiomePrecipitationButton);

        AdvancedButton seasonButton = new AdvancedButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setSeason(active.season().next());
            screen.updateButtonMessages();
        });
        seasonButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.season.desc")));
        screen.seasonButton = screen.addTabControl(seasonButton);

        AdvancedButton beaconBeamsButton = new AdvancedButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setBeaconBeamsEnabled(!active.beaconBeamsEnabled());
            screen.updateButtonMessages();
        });
        beaconBeamsButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.beacon_beams.desc")));
        screen.beaconBeamsButton = screen.addTabControl(beaconBeamsButton);

        screen.skyColorButton = screen.addPhotoColorButton(
                width,
                PhotoModeScreen.ColorPickerTarget.SKY,
                active::skyColorOverride,
                active::skyColor,
                PhotoModeScreen::emptyColor,
                active::setSkyColor
        );

        screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                PhotoModeScreen.CONTROL_HEIGHT,
                0.0D,
                1.0D,
                active.fogIntensity(),
                0.0D,
                PhotoModeScreen.FOG_INTENSITY_SNAP_RADIUS,
                value -> active.setFogIntensity((float) value),
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.fog_intensity", Component.translatable("snappy.photo_mode.percent", Math.round(value * 100.0D)).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR)))
        ));

        screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                PhotoModeScreen.CONTROL_HEIGHT,
                PhotoModeManager.PHOTO_FOG_MIN_DISTANCE,
                PhotoModeManager.PHOTO_FOG_MAX_DISTANCE,
                active.fogDistance(),
                PhotoModeManager.PHOTO_FOG_DEFAULT_DISTANCE,
                PhotoModeScreen.FOG_DISTANCE_SNAP_RADIUS,
                PhotoModeScreen.FOG_DISTANCE_STEP,
                value -> active.setFogDistance((float) value),
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.fog_distance", screen.blockValue(value))
        ));

        screen.fogColorButton = screen.addPhotoColorButton(
                width,
                PhotoModeScreen.ColorPickerTarget.FOG,
                active::fogColorOverride,
                active::fogColor,
                PhotoModeScreen::emptyColor,
                active::setFogColor
        );
    }

}
