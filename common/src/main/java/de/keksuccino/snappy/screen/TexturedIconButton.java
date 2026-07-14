package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.client.gui.GuiBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class TexturedIconButton extends Button {

    public static final int DEFAULT_BUTTON_SIZE = 20;
    public static final int DEFAULT_ICON_SIZE = 15;
    public static final int DEFAULT_TEXTURE_SIZE = 15;
    private static final int BACKGROUND_TEXTURE_SIZE = 20;
    private static final int BACKGROUND_BORDER_SIZE = 4;

    private Identifier iconTexture;
    @Nullable
    private Identifier idleBackgroundTexture;
    @Nullable
    private Identifier hoverBackgroundTexture;
    @Nullable
    private Identifier disabledBackgroundTexture;
    private final int iconSize;
    private final int textureSize;

    public TexturedIconButton(@NotNull Component message, @NotNull OnPress onPress, @NotNull Identifier iconTexture) {
        this(DEFAULT_BUTTON_SIZE, DEFAULT_ICON_SIZE, DEFAULT_TEXTURE_SIZE, message, onPress, iconTexture);
    }

    public TexturedIconButton(
            int buttonSize,
            int iconSize,
            int textureSize,
            @NotNull Component message,
            @NotNull OnPress onPress,
            @NotNull Identifier iconTexture
    ) {
        super(0, 0, buttonSize, buttonSize, message, onPress, DEFAULT_NARRATION);
        if (buttonSize <= 0 || iconSize <= 0 || textureSize <= 0) {
            throw new IllegalArgumentException("Icon button sizes must be positive.");
        }
        this.iconTexture = iconTexture;
        this.iconSize = iconSize;
        this.textureSize = textureSize;
        this.setTooltip(Tooltip.create(this.getMessage()));
    }

    public void setIconTexture(@NotNull Identifier iconTexture) {
        this.iconTexture = iconTexture;
    }

    /**
     * Sets 20x20 background textures for each render state. Each custom texture is nine-sliced with a 4-pixel border on every side. A null texture keeps Vanilla's default sprite for that state. The hover texture also applies while keyboard-focused, matching Vanilla behavior.
     */
    public void setBackgroundTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier disabledTexture) {
        this.idleBackgroundTexture = idleTexture;
        this.hoverBackgroundTexture = hoverTexture;
        this.disabledBackgroundTexture = disabledTexture;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Identifier backgroundTexture = this.backgroundTexture();
        if (backgroundTexture == null) {
            this.extractDefaultSprite(graphics);
        } else {
            GuiBackground.render(graphics, backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE);
        }

        int iconX = this.getX() + (this.getWidth() - this.iconSize) / 2;
        int iconY = this.getY() + (this.getHeight() - this.iconSize) / 2;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.iconTexture,
                iconX,
                iconY,
                0.0F,
                0.0F,
                this.iconSize,
                this.iconSize,
                this.textureSize,
                this.textureSize,
                this.textureSize,
                this.textureSize,
                ARGB.white(this.alpha)
        );
    }

    @Nullable
    private Identifier backgroundTexture() {
        if (!this.active) {
            return this.disabledBackgroundTexture;
        }
        return this.isHoveredOrFocused() ? this.hoverBackgroundTexture : this.idleBackgroundTexture;
    }

}
