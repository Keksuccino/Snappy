package de.keksuccino.snappy.client.gui;

import de.keksuccino.snappy.Snappy;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable texture and border configuration for a dynamically sized nine-sliced GUI background with optional fixed-size overlays.
 */
public final class GuiBackground {

    private static final int DEFAULT_RENDER_COLOR = -1;
    private static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/backgrounds/default_20x20.png");
    private static final Identifier PHOTO_MODE_TABS_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/backgrounds/photo_mode_tabs_243x61.png");
    private static final Identifier PHOTO_MODE_TABS_LEFT_BODY_TOP_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/backgrounds/photo_mode_tabs_left_body_top_13x54.png");
    private static final Identifier PHOTO_MODE_TABS_LEFT_BODY_BOTTOM_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/backgrounds/photo_mode_tabs_left_body_bottom_13x54.png");
    private static final int PHOTO_MODE_TABS_TOP_BORDER = 45;
    private static final int PHOTO_MODE_TABS_BOTTOM_BORDER = 4;
    private static final int PHOTO_MODE_TABS_LEFT_BODY_OVERLAY_WIDTH = 13;
    private static final int PHOTO_MODE_TABS_LEFT_BODY_OVERLAY_HEIGHT = 54;

    public static final GuiBackground DEFAULT = new GuiBackground(DEFAULT_TEXTURE, 20, 20, 4, 4, 4, 4);
    public static final GuiBackground PHOTO_MODE_TABS = new GuiBackground(PHOTO_MODE_TABS_TEXTURE, 243, 61, 15, PHOTO_MODE_TABS_TOP_BORDER, 4, PHOTO_MODE_TABS_BOTTOM_BORDER, new FixedOverlay(PHOTO_MODE_TABS_LEFT_BODY_TOP_TEXTURE, PHOTO_MODE_TABS_LEFT_BODY_OVERLAY_WIDTH, PHOTO_MODE_TABS_LEFT_BODY_OVERLAY_HEIGHT, 1, PHOTO_MODE_TABS_TOP_BORDER, VerticalAnchor.TOP), new FixedOverlay(PHOTO_MODE_TABS_LEFT_BODY_BOTTOM_TEXTURE, PHOTO_MODE_TABS_LEFT_BODY_OVERLAY_WIDTH, PHOTO_MODE_TABS_LEFT_BODY_OVERLAY_HEIGHT, 1, PHOTO_MODE_TABS_BOTTOM_BORDER, VerticalAnchor.BOTTOM));

    private final Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    private final int leftBorder;
    private final int topBorder;
    private final int rightBorder;
    private final int bottomBorder;
    private final FixedOverlay[] fixedOverlays;
    private final int minimumHeight;

    public GuiBackground(@NotNull Identifier texture, int textureWidth, int textureHeight, int leftBorder, int topBorder, int rightBorder, int bottomBorder) {
        this(texture, textureWidth, textureHeight, leftBorder, topBorder, rightBorder, bottomBorder, new FixedOverlay[0]);
    }

    private GuiBackground(@NotNull Identifier texture, int textureWidth, int textureHeight, int leftBorder, int topBorder, int rightBorder, int bottomBorder, FixedOverlay @NotNull ... fixedOverlays) {
        validateTextureLayout(textureWidth, textureHeight, leftBorder, topBorder, rightBorder, bottomBorder);
        validateFixedOverlays(textureWidth, fixedOverlays);
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.leftBorder = leftBorder;
        this.topBorder = topBorder;
        this.rightBorder = rightBorder;
        this.bottomBorder = bottomBorder;
        this.fixedOverlays = fixedOverlays.clone();
        this.minimumHeight = minimumHeight(this.fixedOverlays);
    }

    public void render(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        this.render(graphics, x, y, width, height, DEFAULT_RENDER_COLOR);
    }

    /**
     * Renders this background multiplied by the supplied ARGB color.
     */
    public void render(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        renderSlices(graphics, this.texture, x, y, width, height, this.textureWidth, this.textureHeight, this.leftBorder, this.topBorder, this.rightBorder, this.bottomBorder, color);
        for (FixedOverlay fixedOverlay : this.fixedOverlays) {
            fixedOverlay.render(graphics, x, y, height, color);
        }
    }

    public int textureWidth() {
        return this.textureWidth;
    }

    public int textureHeight() {
        return this.textureHeight;
    }

    public int minimumHeight() {
        return this.minimumHeight;
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
        render(graphics, texture, x, y, width, height, textureWidth, textureHeight, leftBorder, topBorder, rightBorder, bottomBorder, DEFAULT_RENDER_COLOR);
    }

    /**
     * Renders any texture as a nine-sliced background with independently configurable borders, multiplied by the supplied ARGB color.
     */
    public static void render(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight, int leftBorder, int topBorder, int rightBorder, int bottomBorder, int color) {
        validateTextureLayout(textureWidth, textureHeight, leftBorder, topBorder, rightBorder, bottomBorder);
        renderSlices(graphics, texture, x, y, width, height, textureWidth, textureHeight, leftBorder, topBorder, rightBorder, bottomBorder, color);
    }

    private static void renderSlices(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight, int leftBorder, int topBorder, int rightBorder, int bottomBorder, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (width == textureWidth && height == textureHeight) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, textureWidth, textureHeight, color);
            return;
        }

        // Full-width decorative regions must remain one horizontal slice. This keeps their artwork pixel-exact while the center can still grow vertically.
        if (width == textureWidth) {
            int renderedTopBorder = fitLeadingBorder(topBorder, bottomBorder, height);
            int renderedBottomBorder = Math.min(bottomBorder, height - renderedTopBorder);
            int sourceCenterHeight = textureHeight - topBorder - bottomBorder;
            int renderedCenterHeight = height - renderedTopBorder - renderedBottomBorder;
            int sourceBottomY = textureHeight - bottomBorder;
            int renderedBottomY = y + height - renderedBottomBorder;
            renderSlice(graphics, texture, x, y, width, renderedTopBorder, 0, 0, textureWidth, topBorder, textureWidth, textureHeight, color);
            renderSlice(graphics, texture, x, y + renderedTopBorder, width, renderedCenterHeight, 0, topBorder, textureWidth, sourceCenterHeight, textureWidth, textureHeight, color);
            renderSlice(graphics, texture, x, renderedBottomY, width, renderedBottomBorder, 0, sourceBottomY, textureWidth, bottomBorder, textureWidth, textureHeight, color);
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

        renderSlice(graphics, texture, x, y, renderedLeftBorder, renderedTopBorder, 0, 0, leftBorder, topBorder, textureWidth, textureHeight, color);
        renderSlice(graphics, texture, x + renderedLeftBorder, y, renderedCenterWidth, renderedTopBorder, leftBorder, 0, sourceCenterWidth, topBorder, textureWidth, textureHeight, color);
        renderSlice(graphics, texture, renderedRightX, y, renderedRightBorder, renderedTopBorder, sourceRightX, 0, rightBorder, topBorder, textureWidth, textureHeight, color);
        renderSlice(graphics, texture, x, y + renderedTopBorder, renderedLeftBorder, renderedCenterHeight, 0, topBorder, leftBorder, sourceCenterHeight, textureWidth, textureHeight, color);
        renderSlice(graphics, texture, x + renderedLeftBorder, y + renderedTopBorder, renderedCenterWidth, renderedCenterHeight, leftBorder, topBorder, sourceCenterWidth, sourceCenterHeight, textureWidth, textureHeight, color);
        renderSlice(graphics, texture, renderedRightX, y + renderedTopBorder, renderedRightBorder, renderedCenterHeight, sourceRightX, topBorder, rightBorder, sourceCenterHeight, textureWidth, textureHeight, color);
        renderSlice(graphics, texture, x, renderedBottomY, renderedLeftBorder, renderedBottomBorder, 0, sourceBottomY, leftBorder, bottomBorder, textureWidth, textureHeight, color);
        renderSlice(graphics, texture, x + renderedLeftBorder, renderedBottomY, renderedCenterWidth, renderedBottomBorder, leftBorder, sourceBottomY, sourceCenterWidth, bottomBorder, textureWidth, textureHeight, color);
        renderSlice(graphics, texture, renderedRightX, renderedBottomY, renderedRightBorder, renderedBottomBorder, sourceRightX, sourceBottomY, rightBorder, bottomBorder, textureWidth, textureHeight, color);
    }

    private static int fitLeadingBorder(int leadingBorder, int trailingBorder, int targetSize) {
        int combinedBorderSize = leadingBorder + trailingBorder;
        if (combinedBorderSize <= targetSize) {
            return leadingBorder;
        }
        return (int) (((long) leadingBorder * targetSize + combinedBorderSize / 2L) / combinedBorderSize);
    }

    private static int minimumHeight(FixedOverlay @NotNull [] fixedOverlays) {
        int topExtent = 0;
        int bottomExtent = 0;
        for (FixedOverlay fixedOverlay : fixedOverlays) {
            int extent = fixedOverlay.verticalOffset() + fixedOverlay.height();
            if (fixedOverlay.verticalAnchor() == VerticalAnchor.TOP) {
                topExtent = Math.max(topExtent, extent);
            } else {
                bottomExtent = Math.max(bottomExtent, extent);
            }
        }
        return Math.max(1, topExtent + bottomExtent);
    }

    private static void renderSlice(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier texture, int x, int y, int width, int height, int sourceX, int sourceY, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight, int color) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, sourceX, sourceY, width, height, sourceWidth, sourceHeight, textureWidth, textureHeight, color);
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

    private static void validateFixedOverlays(int textureWidth, FixedOverlay @NotNull [] fixedOverlays) {
        for (FixedOverlay fixedOverlay : fixedOverlays) {
            if (fixedOverlay.width() <= 0 || fixedOverlay.height() <= 0) {
                throw new IllegalArgumentException("Fixed background overlay dimensions must be positive.");
            }
            if (fixedOverlay.xOffset() < 0 || fixedOverlay.verticalOffset() < 0 || (long) fixedOverlay.xOffset() + fixedOverlay.width() > textureWidth) {
                throw new IllegalArgumentException("Fixed background overlays must fit within the background width and use non-negative offsets.");
            }
        }
    }

    private record FixedOverlay(@NotNull Identifier texture, int width, int height, int xOffset, int verticalOffset, @NotNull VerticalAnchor verticalAnchor) {

        private void render(@NotNull GuiGraphicsExtractor graphics, int backgroundX, int backgroundY, int backgroundHeight, int color) {
            int y = this.verticalAnchor == VerticalAnchor.TOP ? backgroundY + this.verticalOffset : backgroundY + backgroundHeight - this.verticalOffset - this.height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, backgroundX + this.xOffset, y, 0.0F, 0.0F, this.width, this.height, this.width, this.height, color);
        }

    }

    private enum VerticalAnchor {
        TOP,
        BOTTOM
    }

}
