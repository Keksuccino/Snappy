package de.keksuccino.panoramica.screen;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public class PhotoModeSlider extends AbstractSliderButton {

    private static final double DEFAULT_EPSILON = 1.0E-7D;

    private final double minValue;
    private final double maxValue;
    private final double defaultSliderValue;
    private final double snapSliderRadius;
    private final DoubleConsumer valueConsumer;
    private final DoubleFunction<Component> messageFactory;
    private double rawSliderValue;

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
        this(x, y, width, height, minValue, maxValue, currentValue, currentValue, 0.0D, valueConsumer, messageFactory);
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
        super(x, y, width, height, Component.empty(), toSliderValue(minValue, maxValue, currentValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.rawSliderValue = this.value;
        this.defaultSliderValue = toSliderValue(minValue, maxValue, defaultValue);
        this.snapSliderRadius = maxValue <= minValue ? 0.0D : Math.max(0.0D, snapRadius / (maxValue - minValue));
        this.valueConsumer = valueConsumer;
        this.messageFactory = messageFactory;
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
        this.rawSliderValue = Mth.clamp(newValue, 0.0D, 1.0D);
        super.setValue(this.snappedSliderValue(this.rawSliderValue));
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
                this.setValue(this.rawSliderValue + direction / Math.max(1, this.getWidth() - 8));
                return true;
            }
        }
        return false;
    }

    private double snappedSliderValue(double newValue) {
        if (this.snapSliderRadius <= 0.0D || Math.abs(newValue - this.defaultSliderValue) > this.snapSliderRadius + DEFAULT_EPSILON) {
            return newValue;
        }
        return this.defaultSliderValue;
    }

    private static double toSliderValue(double minValue, double maxValue, double currentValue) {
        if (maxValue <= minValue) {
            return 0.0D;
        }
        return Mth.clamp((currentValue - minValue) / (maxValue - minValue), 0.0D, 1.0D);
    }

}
