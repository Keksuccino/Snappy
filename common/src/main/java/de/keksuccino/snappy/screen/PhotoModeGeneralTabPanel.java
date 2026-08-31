package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.photo.PhotoModeManager;
import de.keksuccino.snappy.util.rendering.gui.widget.SnappyButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

final class PhotoModeGeneralTabPanel implements PhotoModeTabPanel {

    private static final int COLOR_BALANCE_NEUTRAL_COLOR = ARGB.color(245, 245, 245);
    private static final int RED_BALANCE_LOW_COLOR = ARGB.color(0, 255, 255);
    private static final int RED_BALANCE_HIGH_COLOR = ARGB.color(255, 64, 64);
    private static final int GREEN_BALANCE_LOW_COLOR = ARGB.color(255, 64, 255);
    private static final int GREEN_BALANCE_HIGH_COLOR = ARGB.color(64, 255, 64);
    private static final int BLUE_BALANCE_LOW_COLOR = ARGB.color(255, 232, 64);
    private static final int BLUE_BALANCE_HIGH_COLOR = ARGB.color(80, 128, 255);

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
                -180.0D,
                180.0D,
                active.roll(),
                0.0D,
                PhotoModeScreen.ROLL_SNAP_RADIUS,
                value -> active.setRoll((float) value),
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.roll", Component.translatable("snappy.photo_mode.degrees", String.format(Locale.ROOT, "%.0f", value)).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR)))
        ));

        this.addColorEffectControls(screen, width, active);

        SnappyButton gridButton = new SnappyButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setGridEnabled(!active.gridEnabled());
            screen.updateButtonMessages();
        });
        gridButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.grid.desc")));
        screen.gridButton = screen.addTabControl(gridButton);
    }

    private void addColorEffectControls(@NotNull PhotoModeScreen screen, int width, @NotNull PhotoModeManager.Session active) {
        PhotoModeSlider vignetteSlider = screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
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

        this.addPercentEffectSlider(
                screen,
                width,
                "snappy.photo_mode.bloom",
                "snappy.photo_mode.bloom.desc",
                PhotoModeManager.BLOOM_MIN,
                PhotoModeManager.BLOOM_MAX,
                active.bloom(),
                PhotoModeManager.BLOOM_DEFAULT,
                PhotoModeScreen.BLOOM_SNAP_RADIUS,
                PhotoModeScreen.BLOOM_STEP,
                value -> active.setBloom((float) value)
        );

        this.addPercentEffectSlider(
                screen,
                width,
                "snappy.photo_mode.chromatic_aberration",
                "snappy.photo_mode.chromatic_aberration.desc",
                PhotoModeManager.CHROMATIC_ABERRATION_MIN,
                PhotoModeManager.CHROMATIC_ABERRATION_MAX,
                active.chromaticAberration(),
                PhotoModeManager.CHROMATIC_ABERRATION_DEFAULT,
                PhotoModeScreen.CHROMATIC_ABERRATION_SNAP_RADIUS,
                PhotoModeScreen.CHROMATIC_ABERRATION_STEP,
                value -> active.setChromaticAberration((float) value)
        );

        this.addPercentEffectSlider(
                screen,
                width,
                "snappy.photo_mode.film_grain",
                "snappy.photo_mode.film_grain.desc",
                PhotoModeManager.FILM_GRAIN_MIN,
                PhotoModeManager.FILM_GRAIN_MAX,
                active.filmGrain(),
                PhotoModeManager.FILM_GRAIN_DEFAULT,
                PhotoModeScreen.FILM_GRAIN_SNAP_RADIUS,
                PhotoModeScreen.FILM_GRAIN_STEP,
                value -> active.setFilmGrain((float) value)
        );

        this.addColorBalanceSlider(screen, width, "snappy.photo_mode.red_balance", "snappy.photo_mode.red_balance.desc", "snappy.photo_mode.color.cyan", "snappy.photo_mode.color.red", RED_BALANCE_LOW_COLOR, RED_BALANCE_HIGH_COLOR, active.redBalance(), active::redBalance, value -> active.setRedBalance((float) value));
        this.addColorBalanceSlider(screen, width, "snappy.photo_mode.green_balance", "snappy.photo_mode.green_balance.desc", "snappy.photo_mode.color.magenta", "snappy.photo_mode.color.green", GREEN_BALANCE_LOW_COLOR, GREEN_BALANCE_HIGH_COLOR, active.greenBalance(), active::greenBalance, value -> active.setGreenBalance((float) value));
        this.addColorBalanceSlider(screen, width, "snappy.photo_mode.blue_balance", "snappy.photo_mode.blue_balance.desc", "snappy.photo_mode.color.yellow", "snappy.photo_mode.color.blue", BLUE_BALANCE_LOW_COLOR, BLUE_BALANCE_HIGH_COLOR, active.blueBalance(), active::blueBalance, value -> active.setBlueBalance((float) value));

        SnappyButton colorizeButton = new SnappyButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setColorizePreset(active.colorizePreset().next());
            screen.updateButtonMessages();
        });
        colorizeButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.colorize.desc")));
        screen.colorizeButton = screen.addTabControl(colorizeButton);

        SnappyButton stylizeButton = new SnappyButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setStylizePreset(active.stylizePreset().next());
            screen.updateButtonMessages();
        });
        stylizeButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.stylize.desc")));
        screen.stylizeButton = screen.addTabControl(stylizeButton);
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

    private void addColorBalanceSlider(@NotNull PhotoModeScreen screen, int width, @NotNull String labelKey, @NotNull String tooltipKey, @NotNull String lowLabelKey, @NotNull String highLabelKey, int lowColor, int highColor, double currentValue, @NotNull DoubleSupplier currentValueSupplier, @NotNull DoubleConsumer valueConsumer) {
        PhotoModeColorBalanceSlider slider = screen.addTabControl(new PhotoModeColorBalanceSlider(0, 0, width, PhotoModeManager.COLOR_BALANCE_MIN, PhotoModeManager.COLOR_BALANCE_MAX, currentValue, PhotoModeManager.COLOR_BALANCE_DEFAULT, PhotoModeScreen.COLOR_ADJUSTMENT_SNAP_RADIUS, PhotoModeScreen.COLOR_ADJUSTMENT_STEP, valueConsumer, value -> PhotoModeScreen.optionMessage(labelKey, this.colorBalanceValue(value, lowLabelKey, highLabelKey)), () -> colorBalancePreview(currentValueSupplier.getAsDouble(), lowColor, highColor)));
        slider.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
    }

    @NotNull
    private Component colorBalanceValue(double value, @NotNull String lowLabelKey, @NotNull String highLabelKey) {
        int percent = (int) Math.round(Math.abs(value) * 100.0D);
        if (percent == 0) {
            return Component.translatable("snappy.photo_mode.color_balance.neutral").withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR));
        }
        Component direction = Component.translatable(value < 0.0D ? lowLabelKey : highLabelKey);
        Component amount = Component.translatable("snappy.photo_mode.percent", percent);
        return Component.translatable("snappy.photo_mode.color_balance.value", direction, amount).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR));
    }

    private static int colorBalancePreview(double value, int lowColor, int highColor) {
        if (value < 0.0D) {
            return ARGB.srgbLerp((float) Math.min(1.0D, -value), COLOR_BALANCE_NEUTRAL_COLOR, lowColor);
        }
        return ARGB.srgbLerp((float) Math.min(1.0D, value), COLOR_BALANCE_NEUTRAL_COLOR, highColor);
    }

    private void addPercentEffectSlider(
            @NotNull PhotoModeScreen screen,
            int width,
            @NotNull String labelKey,
            @NotNull String tooltipKey,
            float minValue,
            float maxValue,
            float currentValue,
            float defaultValue,
            double snapRadius,
            double actualStep,
            @NotNull DoubleConsumer valueConsumer
    ) {
        PhotoModeSlider slider = screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                minValue,
                maxValue,
                currentValue,
                defaultValue,
                snapRadius,
                actualStep,
                valueConsumer,
                value -> PhotoModeScreen.optionMessage(labelKey, Component.translatable("snappy.photo_mode.percent", Math.round(value * 100.0D)).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR)))
        ));
        slider.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
    }

}
