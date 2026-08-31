package de.keksuccino.snappy.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntSupplier;

final class PhotoModeColorBalanceSlider extends PhotoModeSlider {

    private static final int SWATCH_X_OFFSET = 6;
    private static final int SWATCH_Y_OFFSET = 2;

    private final IntSupplier colorSupplier;

    PhotoModeColorBalanceSlider(int x, int y, int width, double minValue, double maxValue, double currentValue, double defaultValue, double snapRadius, double actualStep, @NotNull DoubleConsumer valueConsumer, @NotNull DoubleFunction<Component> messageFactory, @NotNull IntSupplier colorSupplier) {
        super(x, y, width, minValue, maxValue, currentValue, defaultValue, snapRadius, actualStep, valueConsumer, messageFactory);
        this.colorSupplier = colorSupplier;
    }

    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        PhotoModeColorSwatch.render(graphics, this.getX() + SWATCH_X_OFFSET, this.getY() + SWATCH_Y_OFFSET, this.colorSupplier.getAsInt());
    }

}
