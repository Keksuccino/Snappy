package de.keksuccino.snappy.util.rendering.gui.widget;

import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.client.gui.GuiBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Reusable button base with support for independently configurable background textures in each render state.
 */
public class SnappyButton extends Button {

    public static final int DEFAULT_HEIGHT = 25;
    public static final Identifier DEFAULT_IDLE_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/buttons/advanced/normal_30x25.png");
    public static final Identifier DEFAULT_HOVER_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/buttons/advanced/hover_30x25.png");
    public static final Identifier DEFAULT_DISABLED_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/buttons/advanced/disabled_30x25.png");

    private static final int BACKGROUND_TEXTURE_WIDTH = 30;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 25;
    private static final int BACKGROUND_HORIZONTAL_BORDER_SIZE = 10;
    private static final int BACKGROUND_VERTICAL_BORDER_SIZE = 5;
    private static final int LABEL_HORIZONTAL_MARGIN = 14;

    @Nullable
    private Identifier idleBackgroundTexture = DEFAULT_IDLE_BACKGROUND_TEXTURE;
    @Nullable
    private Identifier hoverBackgroundTexture = DEFAULT_HOVER_BACKGROUND_TEXTURE;
    @Nullable
    private Identifier selectedBackgroundTexture;
    @Nullable
    private Identifier disabledBackgroundTexture = DEFAULT_DISABLED_BACKGROUND_TEXTURE;
    private int backgroundTextureWidth = BACKGROUND_TEXTURE_WIDTH;
    private int backgroundTextureHeight = BACKGROUND_TEXTURE_HEIGHT;
    private int backgroundHorizontalBorderSize = BACKGROUND_HORIZONTAL_BORDER_SIZE;
    private int backgroundVerticalBorderSize = BACKGROUND_VERTICAL_BORDER_SIZE;
    @Nullable
    private CreateNarration narration;
    private boolean useVanillaTextures = false;
    private boolean selected = false;

    public SnappyButton(int x, int y, int width, @NotNull Component message, @NotNull OnPress onPress) {
        this(x, y, width, DEFAULT_HEIGHT, message, onPress);
    }

    public SnappyButton(int x, int y, int width, int height, @NotNull Component message, @NotNull OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    /**
     * Sets 30x25 background textures for each render state. Each custom texture is nine-sliced with 10-pixel borders and a 10-pixel center horizontally, plus 5-pixel borders and a 15-pixel center vertically. A null texture keeps Vanilla's default sprite for that state. The hover texture also applies while keyboard-focused, matching Vanilla behavior.
     */
    @NotNull
    public SnappyButton setBackgroundTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier disabledTexture) {
        return this.setBackgroundTextures(idleTexture, hoverTexture, null, disabledTexture, BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT, BACKGROUND_HORIZONTAL_BORDER_SIZE, BACKGROUND_VERTICAL_BORDER_SIZE);
    }

    /**
     * Sets custom background textures and their symmetric nine-slice layout. A null texture keeps Vanilla's default sprite for that state. The hover texture also applies while keyboard-focused, matching Vanilla behavior.
     */
    @NotNull
    public SnappyButton setBackgroundTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier disabledTexture, int textureWidth, int textureHeight, int horizontalBorderSize, int verticalBorderSize) {
        return this.setBackgroundTextures(idleTexture, hoverTexture, null, disabledTexture, textureWidth, textureHeight, horizontalBorderSize, verticalBorderSize);
    }

    /**
     * Sets 30x25 background textures for each render state, including an optional selected state. The selected texture takes precedence over hover while the button is selected.
     */
    @NotNull
    public SnappyButton setBackgroundTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier selectedTexture, @Nullable Identifier disabledTexture) {
        return this.setBackgroundTextures(idleTexture, hoverTexture, selectedTexture, disabledTexture, BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT, BACKGROUND_HORIZONTAL_BORDER_SIZE, BACKGROUND_VERTICAL_BORDER_SIZE);
    }

    /**
     * Sets custom background textures and their symmetric nine-slice layout, including an optional selected state. Disabled takes highest precedence, followed by selected, hover/focus, and idle. A null selected texture falls back to the ordinary hover or idle state.
     */
    @NotNull
    public SnappyButton setBackgroundTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier selectedTexture, @Nullable Identifier disabledTexture, int textureWidth, int textureHeight, int horizontalBorderSize, int verticalBorderSize) {
        validateBackgroundTextureLayout(textureWidth, textureHeight, horizontalBorderSize, verticalBorderSize);
        this.idleBackgroundTexture = idleTexture;
        this.hoverBackgroundTexture = hoverTexture;
        this.selectedBackgroundTexture = selectedTexture;
        this.disabledBackgroundTexture = disabledTexture;
        this.backgroundTextureWidth = textureWidth;
        this.backgroundTextureHeight = textureHeight;
        this.backgroundHorizontalBorderSize = horizontalBorderSize;
        this.backgroundVerticalBorderSize = verticalBorderSize;
        return this;
    }

    /**
     * Controls whether this button uses its selected background texture.
     */
    @NotNull
    public SnappyButton setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    /**
     * Controls whether Vanilla button sprites are rendered instead of this button's custom background textures.
     */
    @NotNull
    public SnappyButton useVanillaTextures(boolean useVanillaTextures) {
        this.useVanillaTextures = useVanillaTextures;
        return this;
    }

    /**
     * Sets a custom narration factory, or restores the default narration when null.
     */
    @NotNull
    public SnappyButton setNarration(@Nullable CreateNarration narration) {
        this.narration = narration;
        return this;
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        if (this.narration == null) {
            return super.createNarrationMessage();
        }
        return this.narration.createNarrationMessage(() -> super.createNarrationMessage());
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractBackground(graphics);
        this.extractLabel(graphics);
    }

    protected final void extractBackground(@NotNull GuiGraphicsExtractor graphics) {
        if (this.useVanillaTextures) {
            this.extractDefaultSprite(graphics);
            return;
        }
        Identifier backgroundTexture = this.backgroundTexture();
        if (backgroundTexture == null) {
            this.extractDefaultSprite(graphics);
            return;
        }
        GuiBackground.render(graphics, backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.backgroundTextureWidth, this.backgroundTextureHeight, this.backgroundHorizontalBorderSize, this.backgroundVerticalBorderSize, this.backgroundHorizontalBorderSize, this.backgroundVerticalBorderSize, ARGB.white(this.alpha));
    }

    /**
     * Extracts the centered label with 14-pixel horizontal margins while retaining Minecraft's scrolling behavior for oversized labels.
     */
    protected final void extractLabel(@NotNull GuiGraphicsExtractor graphics) {
        int left = Math.min(this.getRight(), this.getX() + LABEL_HORIZONTAL_MARGIN);
        int right = Math.max(left, this.getRight() - LABEL_HORIZONTAL_MARGIN);
        graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE).acceptScrollingWithDefaultCenter(this.getMessage(), left, right, this.getY(), this.getBottom());
    }

    @Nullable
    private Identifier backgroundTexture() {
        if (!this.active) {
            return this.disabledBackgroundTexture;
        }
        if (this.selected && this.selectedBackgroundTexture != null) {
            return this.selectedBackgroundTexture;
        }
        return this.isHoveredOrFocused() ? this.hoverBackgroundTexture : this.idleBackgroundTexture;
    }

    private static void validateBackgroundTextureLayout(int textureWidth, int textureHeight, int horizontalBorderSize, int verticalBorderSize) {
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw new IllegalArgumentException("Background texture dimensions must be positive.");
        }
        if (horizontalBorderSize < 0 || verticalBorderSize < 0) {
            throw new IllegalArgumentException("Background texture borders cannot be negative.");
        }
        if ((long) horizontalBorderSize * 2 >= textureWidth || (long) verticalBorderSize * 2 >= textureHeight) {
            throw new IllegalArgumentException("Background texture borders must leave a non-empty center slice.");
        }
    }

}
