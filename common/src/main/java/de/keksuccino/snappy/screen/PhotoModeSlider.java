package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.util.rendering.gui.widget.SnappySlider;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public class PhotoModeSlider extends SnappySlider {

    private static final double DEFAULT_EPSILON = 1.0E-7D;

    private final double minValue;
    private final double maxValue;
    private final double defaultSliderValue;
    private final double snapSliderRadius;
    private final double actualStep;
    private final DoubleConsumer valueConsumer;
    private final DoubleFunction<Component> messageFactory;
    private double rawSliderValue;
    private boolean skipDefaultSnapUntilOutsideZone;

    public PhotoModeSlider(
            int x,
            int y,
            int width,
            int height,
            double minValue,
            double maxValue,
            double currentValue,
            @NotNull DoubleConsumer valueConsumer,
            @NotNull DoubleFunction<Component> messageFactory
    ) {
        this(x, y, width, height, minValue, maxValue, currentValue, currentValue, 0.0D, 0.0D, valueConsumer, messageFactory);
    }

    public PhotoModeSlider(
            int x,
            int y,
            int width,
            int height,
            double minValue,
            double maxValue,
            double currentValue,
            double defaultValue,
            double snapRadius,
            @NotNull DoubleConsumer valueConsumer,
            @NotNull DoubleFunction<Component> messageFactory
    ) {
        this(x, y, width, height, minValue, maxValue, currentValue, defaultValue, snapRadius, 0.0D, valueConsumer, messageFactory);
    }

    public PhotoModeSlider(
            int x,
            int y,
            int width,
            int height,
            double minValue,
            double maxValue,
            double currentValue,
            double defaultValue,
            double snapRadius,
            double actualStep,
            @NotNull DoubleConsumer valueConsumer,
            @NotNull DoubleFunction<Component> messageFactory
    ) {
        super(x, y, width, height, Component.empty(), toSliderValue(minValue, maxValue, currentValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.rawSliderValue = this.value;
        this.defaultSliderValue = toSliderValue(minValue, maxValue, defaultValue);
        this.snapSliderRadius = maxValue <= minValue ? 0.0D : Math.max(0.0D, snapRadius / (maxValue - minValue));
        this.actualStep = maxValue <= minValue ? 0.0D : Math.max(0.0D, actualStep);
        this.valueConsumer = valueConsumer;
        this.messageFactory = messageFactory;
        super.setValue(this.adjustedSliderValue(this.rawSliderValue, true));
        this.updateMessage();
    }

    public void setActualValue(double value) {
        this.setValue(toSliderValue(this.minValue, this.maxValue, value));
    }

    public double actualValue() {
        return Mth.lerp(this.value, this.minValue, this.maxValue);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(this.messageFactory.apply(this.actualValue()));
    }

    @Override
    protected void applyValue() {
        this.valueConsumer.accept(this.actualValue());
    }

    @Override
    protected void setValue(double newValue) {
        this.setValue(newValue, true);
    }

    private void setValue(double newValue, boolean allowDefaultSnapping) {
        this.rawSliderValue = Mth.clamp(newValue, 0.0D, 1.0D);
        super.setValue(this.adjustedSliderValue(this.rawSliderValue, allowDefaultSnapping));
    }

    @Override
    public void onClick(@NotNull MouseButtonEvent event, boolean doubleClick) {
        this.prepareDefaultSnapFromCurrentValue();
        super.onClick(event, doubleClick);
    }

    @Override
    public void onRelease(@NotNull MouseButtonEvent event) {
        this.skipDefaultSnapUntilOutsideZone = false;
        super.onRelease(event);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (event.isSelection()) {
            this.canChangeValue = !this.canChangeValue;
            return true;
        }
        if (this.canChangeValue) {
            boolean left = event.isLeft();
            boolean right = event.isRight();
            if (left || right) {
                double direction = left ? -1.0D : 1.0D;
                this.setValue(this.value + direction * this.keyboardSliderStep(), false);
                return true;
            }
        }
        return false;
    }

    private double adjustedSliderValue(double newValue, boolean allowDefaultSnapping) {
        return this.steppedSliderValue(this.snappedSliderValue(newValue, allowDefaultSnapping));
    }

    private double snappedSliderValue(double newValue, boolean allowDefaultSnapping) {
        if (!this.shouldSnapToDefault(newValue, allowDefaultSnapping)) {
            return newValue;
        }
        return this.defaultSliderValue;
    }

    private boolean shouldSnapToDefault(double newValue, boolean allowDefaultSnapping) {
        if (!allowDefaultSnapping) {
            return false;
        }

        boolean inDefaultSnapZone = this.isInDefaultSnapZone(newValue);
        if (!this.skipDefaultSnapUntilOutsideZone) {
            return inDefaultSnapZone;
        }
        if (inDefaultSnapZone) {
            return false;
        }
        this.skipDefaultSnapUntilOutsideZone = false;
        return false;
    }

    private boolean isInDefaultSnapZone(double newValue) {
        return this.snapSliderRadius > 0.0D && Math.abs(newValue - this.defaultSliderValue) <= this.snapSliderRadius + DEFAULT_EPSILON;
    }

    protected void prepareDefaultSnapFromCurrentValue() {
        this.skipDefaultSnapUntilOutsideZone = this.isInDefaultSnapZone(this.rawSliderValue);
    }

    private double steppedSliderValue(double newValue) {
        if (this.actualStep <= 0.0D || this.maxValue <= this.minValue) {
            return newValue;
        }

        double actualValue = Mth.lerp(newValue, this.minValue, this.maxValue);
        double steppedActualValue = this.minValue + Math.round((actualValue - this.minValue) / this.actualStep) * this.actualStep;
        return toSliderValue(this.minValue, this.maxValue, steppedActualValue);
    }

    private double keyboardSliderStep() {
        if (this.actualStep > 0.0D && this.maxValue > this.minValue) {
            return this.actualStep / (this.maxValue - this.minValue);
        }
        return 1.0D / Math.max(1, this.getWidth() - 8);
    }

    private static double toSliderValue(double minValue, double maxValue, double currentValue) {
        if (maxValue <= minValue) {
            return 0.0D;
        }
        return Mth.clamp((currentValue - minValue) / (maxValue - minValue), 0.0D, 1.0D);
    }

}
