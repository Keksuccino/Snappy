package de.keksuccino.snappy.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class PhotoModeColorButton extends Button {

    private static final int SWATCH_SIZE = 10;
    private static final int SWATCH_BORDER_COLOR = ARGB.color(255, 20, 20, 20);
    private static final int SWATCH_INNER_BORDER_COLOR = ARGB.color(255, 235, 235, 235);
    private static final int SWATCH_EMPTY_BACKGROUND_COLOR = ARGB.color(82, 24, 28, 34);
    private static final int SWATCH_EMPTY_LINE_COLOR = ARGB.color(255, 255, 209, 102);

    private final Supplier<@Nullable Integer> colorSupplier;

    public PhotoModeColorButton(
            int x,
            int y,
            int width,
            int height,
            @NotNull Component message,
            @NotNull Button.OnPress onPress,
            @NotNull Supplier<@Nullable Integer> colorSupplier
    ) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.colorSupplier = colorSupplier;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);
        int swatchX = this.getX() + 6;
        int swatchY = this.getY() + (this.getHeight() - SWATCH_SIZE) / 2;
        @Nullable Integer color = this.colorSupplier.get();
        graphics.fill(swatchX - 1, swatchY - 1, swatchX + SWATCH_SIZE + 1, swatchY + SWATCH_SIZE + 1, SWATCH_BORDER_COLOR);
        if (color == null) {
            graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, SWATCH_EMPTY_BACKGROUND_COLOR);
            for (int offset = 0; offset < SWATCH_SIZE; offset++) {
                graphics.fill(swatchX + offset, swatchY + SWATCH_SIZE - 1 - offset, swatchX + offset + 1, swatchY + SWATCH_SIZE - offset, SWATCH_EMPTY_LINE_COLOR);
            }
        } else {
            graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, ARGB.opaque(color));
        }
        graphics.outline(swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, SWATCH_INNER_BORDER_COLOR);
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

}
