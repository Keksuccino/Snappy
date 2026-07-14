package de.keksuccino.snappy.util.rendering.gui.widget;

import de.keksuccino.snappy.client.gui.GuiBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Reusable button base with support for independently configurable background textures in each render state.
 */
public class AdvancedButton extends Button {

    private static final int BACKGROUND_TEXTURE_SIZE = 20;
    private static final int BACKGROUND_BORDER_SIZE = 4;

    @Nullable
    private Identifier idleBackgroundTexture;
    @Nullable
    private Identifier hoverBackgroundTexture;
    @Nullable
    private Identifier disabledBackgroundTexture;

    public AdvancedButton(int x, int y, int width, int height, @NotNull Component message, @NotNull OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    /**
     * Sets 20x20 background textures for each render state. Each custom texture is nine-sliced with a 4-pixel border on every side. A null texture keeps Vanilla's default sprite for that state. The hover texture also applies while keyboard-focused, matching Vanilla behavior.
     */
    @NotNull
    public AdvancedButton setBackgroundTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier disabledTexture) {
        this.idleBackgroundTexture = idleTexture;
        this.hoverBackgroundTexture = hoverTexture;
        this.disabledBackgroundTexture = disabledTexture;
        return this;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractBackground(graphics);
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

    protected final void extractBackground(@NotNull GuiGraphicsExtractor graphics) {
        Identifier backgroundTexture = this.backgroundTexture();
        if (backgroundTexture == null) {
            this.extractDefaultSprite(graphics);
            return;
        }
        GuiBackground.render(graphics, backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, ARGB.white(this.alpha));
    }

    @Nullable
    private Identifier backgroundTexture() {
        if (!this.active) {
            return this.disabledBackgroundTexture;
        }
        return this.isHoveredOrFocused() ? this.hoverBackgroundTexture : this.idleBackgroundTexture;
    }

}
