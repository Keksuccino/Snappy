package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
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

        screen.pauseButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            PhotoModeManager.togglePaused(Minecraft.getInstance());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.pause.desc"))).build());

        screen.timeButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setTimePreset(active.timePreset().next());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.time.desc"))).build());

        screen.weatherButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setWeatherPreset(active.weatherPreset().next());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.weather.desc"))).build());

        screen.forceBiomePrecipitationButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setForceBiomePrecipitation(!active.forceBiomePrecipitation());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.force_biome_precipitation.desc"))).build());

        screen.seasonButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setSeason(active.season().next());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.season.desc"))).build());

        screen.beaconBeamsButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setBeaconBeamsEnabled(!active.beaconBeamsEnabled());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.beacon_beams.desc"))).build());

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
