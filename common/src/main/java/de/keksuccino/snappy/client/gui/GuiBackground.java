package de.keksuccino.snappy.client.gui;

import de.keksuccino.snappy.Snappy;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable texture and border configuration for a dynamically sized nine-sliced GUI background.
 */
public final class GuiBackground {

    private static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/backgrounds/default_20x20.png");
    private static final Identifier PHOTO_MODE_TABS_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/backgrounds/photo_mode_tabs_31x61.png");

    public static final GuiBackground DEFAULT = new GuiBackground(DEFAULT_TEXTURE, 20, 20, 4, 4, 4, 4);
    public static final GuiBackground PHOTO_MODE_TABS = new GuiBackground(PHOTO_MODE_TABS_TEXTURE, 31, 61, 15, 45, 4, 4);

    private final Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    private final int leftBorder;
    private final int topBorder;
    private final int rightBorder;
    private final int bottomBorder;

    public GuiBackground(@NotNull Identifier texture, int textureWidth, int textureHeight, int leftBorder, int topBorder, int rightBorder, int bottomBorder) {
        validateTextureLayout(textureWidth, textureHeight, leftBorder, topBorder, rightBorder, bottomBorder);
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.leftBorder = leftBorder;
        this.topBorder = topBorder;
        this.rightBorder = rightBorder;
        this.bottomBorder = bottomBorder;
    }

    public void render(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        renderSlices(graphics, this.texture, x, y, width, height, this.textureWidth, this.textureHeight, this.leftBorder, this.topBorder, this.rightBorder, this.bottomBorder);
    }

    public int leftBorder() {
        return this.leftBorder;
    }

    public int topBorder() {
        return this.topBorder;
    }

    public int rightBorder() {
        return this.rightBorder;
    }

    public int bottomBorder() {
        return this.bottomBorder;
    }

    /**
     * Renders any texture as a nine-sliced background with independently configurable borders.
     */
    public static void render(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight, int leftBorder, int topBorder, int rightBorder, int bottomBorder) {
        validateTextureLayout(textureWidth, textureHeight, leftBorder, topBorder, rightBorder, bottomBorder);
        renderSlices(graphics, texture, x, y, width, height, textureWidth, textureHeight, leftBorder, topBorder, rightBorder, bottomBorder);
    }

    private static void renderSlices(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight, int leftBorder, int topBorder, int rightBorder, int bottomBorder) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (width == textureWidth && height == textureHeight) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, textureWidth, textureHeight);
            return;
        }

        // Very small destinations cannot retain every border at its source size. Scale opposing borders proportionally so they meet without overlap while preserving the complete source decorations.
        int renderedLeftBorder = fitLeadingBorder(leftBorder, rightBorder, width);
        int renderedRightBorder = Math.min(rightBorder, width - renderedLeftBorder);
        int renderedTopBorder = fitLeadingBorder(topBorder, bottomBorder, height);
        int renderedBottomBorder = Math.min(bottomBorder, height - renderedTopBorder);
        int sourceCenterWidth = textureWidth - leftBorder - rightBorder;
        int sourceCenterHeight = textureHeight - topBorder - bottomBorder;
        int renderedCenterWidth = width - renderedLeftBorder - renderedRightBorder;
        int renderedCenterHeight = height - renderedTopBorder - renderedBottomBorder;
        int sourceRightX = textureWidth - rightBorder;
        int sourceBottomY = textureHeight - bottomBorder;
        int renderedRightX = x + width - renderedRightBorder;
        int renderedBottomY = y + height - renderedBottomBorder;

        renderSlice(graphics, texture, x, y, renderedLeftBorder, renderedTopBorder, 0, 0, leftBorder, topBorder, textureWidth, textureHeight);
        renderSlice(graphics, texture, x + renderedLeftBorder, y, renderedCenterWidth, renderedTopBorder, leftBorder, 0, sourceCenterWidth, topBorder, textureWidth, textureHeight);
        renderSlice(graphics, texture, renderedRightX, y, renderedRightBorder, renderedTopBorder, sourceRightX, 0, rightBorder, topBorder, textureWidth, textureHeight);
        renderSlice(graphics, texture, x, y + renderedTopBorder, renderedLeftBorder, renderedCenterHeight, 0, topBorder, leftBorder, sourceCenterHeight, textureWidth, textureHeight);
        renderSlice(graphics, texture, x + renderedLeftBorder, y + renderedTopBorder, renderedCenterWidth, renderedCenterHeight, leftBorder, topBorder, sourceCenterWidth, sourceCenterHeight, textureWidth, textureHeight);
        renderSlice(graphics, texture, renderedRightX, y + renderedTopBorder, renderedRightBorder, renderedCenterHeight, sourceRightX, topBorder, rightBorder, sourceCenterHeight, textureWidth, textureHeight);
        renderSlice(graphics, texture, x, renderedBottomY, renderedLeftBorder, renderedBottomBorder, 0, sourceBottomY, leftBorder, bottomBorder, textureWidth, textureHeight);
        renderSlice(graphics, texture, x + renderedLeftBorder, renderedBottomY, renderedCenterWidth, renderedBottomBorder, leftBorder, sourceBottomY, sourceCenterWidth, bottomBorder, textureWidth, textureHeight);
        renderSlice(graphics, texture, renderedRightX, renderedBottomY, renderedRightBorder, renderedBottomBorder, sourceRightX, sourceBottomY, rightBorder, bottomBorder, textureWidth, textureHeight);
    }

    private static int fitLeadingBorder(int leadingBorder, int trailingBorder, int targetSize) {
        int combinedBorderSize = leadingBorder + trailingBorder;
        if (combinedBorderSize <= targetSize) {
            return leadingBorder;
        }
        return (int) (((long) leadingBorder * targetSize + combinedBorderSize / 2L) / combinedBorderSize);
    }

    private static void renderSlice(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier texture, int x, int y, int width, int height, int sourceX, int sourceY, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, sourceX, sourceY, width, height, sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    private static void validateTextureLayout(int textureWidth, int textureHeight, int leftBorder, int topBorder, int rightBorder, int bottomBorder) {
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw new IllegalArgumentException("Nine-sliced texture dimensions must be positive.");
        }
        if (leftBorder < 0 || topBorder < 0 || rightBorder < 0 || bottomBorder < 0) {
            throw new IllegalArgumentException("Nine-sliced texture borders cannot be negative.");
        }
        if ((long) leftBorder + rightBorder >= textureWidth || (long) topBorder + bottomBorder >= textureHeight) {
            throw new IllegalArgumentException("Nine-sliced texture borders must leave a non-empty center slice.");
        }
    }

}
