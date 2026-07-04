package de.keksuccino.panoramica.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntSupplier;

public class PhotoModeColorButton extends Button {

    private static final int SWATCH_SIZE = 10;
    private static final int SWATCH_BORDER_COLOR = ARGB.color(255, 20, 20, 20);
    private static final int SWATCH_INNER_BORDER_COLOR = ARGB.color(255, 235, 235, 235);

    private final IntSupplier colorSupplier;

    public PhotoModeColorButton(
            int x,
            int y,
            int width,
            int height,
            @NotNull Component message,
            @NotNull Button.OnPress onPress,
            @NotNull IntSupplier colorSupplier
    ) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.colorSupplier = colorSupplier;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);
        int swatchX = this.getX() + 6;
        int swatchY = this.getY() + (this.getHeight() - SWATCH_SIZE) / 2;
        graphics.fill(swatchX - 1, swatchY - 1, swatchX + SWATCH_SIZE + 1, swatchY + SWATCH_SIZE + 1, SWATCH_BORDER_COLOR);
        graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, ARGB.opaque(this.colorSupplier.getAsInt()));
        graphics.outline(swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, SWATCH_INNER_BORDER_COLOR);
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

}
