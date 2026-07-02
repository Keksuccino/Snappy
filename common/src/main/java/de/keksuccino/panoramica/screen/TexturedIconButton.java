package de.keksuccino.panoramica.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

public class TexturedIconButton extends Button {

    public static final int DEFAULT_BUTTON_SIZE = 20;
    public static final int DEFAULT_ICON_SIZE = 15;
    public static final int DEFAULT_TEXTURE_SIZE = 32;

    private final Identifier iconTexture;
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

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);

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

}
