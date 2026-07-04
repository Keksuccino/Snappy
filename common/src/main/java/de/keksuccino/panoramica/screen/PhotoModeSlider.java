package de.keksuccino.panoramica.screen;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public class PhotoModeSlider extends AbstractSliderButton {

    private final double minValue;
    private final double maxValue;
    private final DoubleConsumer valueConsumer;
    private final DoubleFunction<Component> messageFactory;

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
        super(x, y, width, height, Component.empty(), toSliderValue(minValue, maxValue, currentValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
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

    private static double toSliderValue(double minValue, double maxValue, double currentValue) {
        if (maxValue <= minValue) {
            return 0.0D;
        }
        return Mth.clamp((currentValue - minValue) / (maxValue - minValue), 0.0D, 1.0D);
    }

}
