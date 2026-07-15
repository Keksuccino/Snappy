package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.util.rendering.gui.widget.AdvancedButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class PhotoModeColorButton extends AdvancedButton {

    private final Supplier<@Nullable Integer> colorSupplier;

    public PhotoModeColorButton(int x, int y, int width, int height, @NotNull Component message, @NotNull OnPress onPress, @NotNull Supplier<@Nullable Integer> colorSupplier) {
        super(x, y, width, height, message, onPress);
        this.colorSupplier = colorSupplier;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractBackground(graphics);
        int swatchX = this.getX() + 6;
        int swatchY = this.getY() + (this.getHeight() - PhotoModeColorSwatch.SIZE) / 2;
        PhotoModeColorSwatch.render(graphics, swatchX, swatchY, this.colorSupplier.get());
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

}
