package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.DoubleConsumer;

final class PhotoModeGeneralTabPanel implements PhotoModeTabPanel {

    @Override
    public void addControls(@NotNull PhotoModeScreen screen) {
        int width = screen.tabControlWidth();
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }

        screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                PhotoModeScreen.CONTROL_HEIGHT,
                30.0D,
                110.0D,
                active.fieldOfView(),
                Minecraft.getInstance().options.fov().get().doubleValue(),
                PhotoModeScreen.FOV_SNAP_RADIUS,
                value -> active.setFieldOfView((float) value),
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.fov", Component.literal(String.format(Locale.ROOT, "%.0f", value)).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR)))
        ));

        screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                PhotoModeScreen.CONTROL_HEIGHT,
                -180.0D,
                180.0D,
                active.roll(),
                0.0D,
                PhotoModeScreen.ROLL_SNAP_RADIUS,
                value -> active.setRoll((float) value),
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.roll", Component.translatable("snappy.photo_mode.degrees", String.format(Locale.ROOT, "%.0f", value)).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR)))
        ));

        this.addColorEffectControls(screen, width, active);

        screen.gridButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setGridEnabled(!active.gridEnabled());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.grid.desc"))).build());
    }

    private void addColorEffectControls(@NotNull PhotoModeScreen screen, int width, @NotNull PhotoModeManager.Session active) {
        PhotoModeSlider vignetteSlider = screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                PhotoModeScreen.CONTROL_HEIGHT,
                0.0D,
                1.0D,
                active.vignette(),
                0.0D,
                PhotoModeScreen.VIGNETTE_SNAP_RADIUS,
                value -> active.setVignette((float) value),
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.vignette", Component.translatable("snappy.photo_mode.percent", Math.round(value * 100.0D)).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR)))
        ));
        vignetteSlider.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.vignette.desc")));

        this.addColorAdjustmentSlider(
                screen,
                width,
                "snappy.photo_mode.gamma",
                "snappy.photo_mode.gamma.desc",
                PhotoModeManager.GAMMA_MIN,
                PhotoModeManager.GAMMA_MAX,
                active.gamma(),
                PhotoModeManager.GAMMA_DEFAULT,
                value -> active.setGamma((float) value)
        );

        this.addColorAdjustmentSlider(
                screen,
                width,
                "snappy.photo_mode.saturation",
                "snappy.photo_mode.saturation.desc",
                PhotoModeManager.SATURATION_MIN,
                PhotoModeManager.SATURATION_MAX,
                active.saturation(),
                PhotoModeManager.SATURATION_DEFAULT,
                value -> active.setSaturation((float) value)
        );

        this.addColorAdjustmentSlider(
                screen,
                width,
                "snappy.photo_mode.contrast",
                "snappy.photo_mode.contrast.desc",
                PhotoModeManager.CONTRAST_MIN,
                PhotoModeManager.CONTRAST_MAX,
                active.contrast(),
                PhotoModeManager.CONTRAST_DEFAULT,
                value -> active.setContrast((float) value)
        );

        this.addColorAdjustmentSlider(
                screen,
                width,
                "snappy.photo_mode.overexposure",
                "snappy.photo_mode.overexposure.desc",
                PhotoModeManager.OVEREXPOSURE_MIN,
                PhotoModeManager.OVEREXPOSURE_MAX,
                active.overexposure(),
                PhotoModeManager.OVEREXPOSURE_DEFAULT,
                value -> active.setOverexposure((float) value)
        );

        PhotoModeSlider bloomSlider = screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                PhotoModeScreen.CONTROL_HEIGHT,
                PhotoModeManager.BLOOM_MIN,
                PhotoModeManager.BLOOM_MAX,
                active.bloom(),
                PhotoModeManager.BLOOM_DEFAULT,
                PhotoModeScreen.BLOOM_SNAP_RADIUS,
                PhotoModeScreen.BLOOM_STEP,
                value -> active.setBloom((float) value),
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.bloom", Component.translatable("snappy.photo_mode.percent", Math.round(value * 100.0D)).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR)))
        ));
        bloomSlider.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.bloom.desc")));

        screen.colorizeButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setColorizePreset(active.colorizePreset().next());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.colorize.desc"))).build());

        screen.stylizeButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setStylizePreset(active.stylizePreset().next());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.stylize.desc"))).build());
    }

    private void addColorAdjustmentSlider(
            @NotNull PhotoModeScreen screen,
            int width,
            @NotNull String labelKey,
            @NotNull String tooltipKey,
            float minValue,
            float maxValue,
            float currentValue,
            float defaultValue,
            @NotNull DoubleConsumer valueConsumer
    ) {
        PhotoModeSlider slider = screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                PhotoModeScreen.CONTROL_HEIGHT,
                minValue,
                maxValue,
                currentValue,
                defaultValue,
                PhotoModeScreen.COLOR_ADJUSTMENT_SNAP_RADIUS,
                PhotoModeScreen.COLOR_ADJUSTMENT_STEP,
                valueConsumer,
                value -> PhotoModeScreen.optionMessage(labelKey, PhotoModeScreen.signedPercentValue(value))
        ));
        slider.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
    }

}
