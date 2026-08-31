package de.keksuccino.snappy.util.rendering.gui.widget;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.keksuccino.snappy.Snappy;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Reusable slider base with independently configurable bar and handle textures in each render state.
 */
public abstract class SnappySlider extends AbstractSliderButton {

    public static final int DEFAULT_HEIGHT = 25;
    public static final Identifier DEFAULT_IDLE_BAR_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/bar/normal_70x8.png");
    public static final Identifier DEFAULT_HOVER_BAR_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/bar/hover_70x8.png");
    public static final Identifier DEFAULT_DISABLED_BAR_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/bar/disabled_70x8.png");
    public static final Identifier DEFAULT_IDLE_HANDLE_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/handle/normal_8x8.png");
    public static final Identifier DEFAULT_HOVER_HANDLE_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/handle/hover_8x8.png");
    public static final Identifier DEFAULT_DISABLED_HANDLE_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/handle/disabled_8x8.png");

    private static final Identifier VANILLA_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("widget/slider");
    private static final Identifier VANILLA_HIGHLIGHTED_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("widget/slider_highlighted");
    private static final Identifier VANILLA_HANDLE_TEXTURE = Identifier.withDefaultNamespace("widget/slider_handle");
    private static final Identifier VANILLA_HIGHLIGHTED_HANDLE_TEXTURE = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");
    private static final int BAR_TEXTURE_WIDTH = 70;
    private static final int BAR_TEXTURE_HEIGHT = 8;
    private static final int BAR_BORDER_WIDTH = 20;
    private static final int BAR_CENTER_WIDTH = BAR_TEXTURE_WIDTH - BAR_BORDER_WIDTH * 2;
    private static final int HANDLE_TEXTURE_WIDTH = 8;
    private static final int HANDLE_TEXTURE_HEIGHT = 8;
    private static final int SLIDER_CONTROL_HEIGHT = Math.max(BAR_TEXTURE_HEIGHT, HANDLE_TEXTURE_HEIGHT);
    private static final int LABEL_HORIZONTAL_MARGIN = 10;
    // Minecraft adds one pixel when vertically centering its nine-pixel font line. A 13-pixel area therefore places the label exactly three pixels below the widget's top edge.
    private static final int LABEL_RENDER_AREA_HEIGHT = 13;

    @Nullable
    private Identifier idleBarTexture = DEFAULT_IDLE_BAR_TEXTURE;
    @Nullable
    private Identifier hoverBarTexture = DEFAULT_HOVER_BAR_TEXTURE;
    @Nullable
    private Identifier disabledBarTexture = DEFAULT_DISABLED_BAR_TEXTURE;
    @Nullable
    private Identifier idleHandleTexture = DEFAULT_IDLE_HANDLE_TEXTURE;
    @Nullable
    private Identifier hoverHandleTexture = DEFAULT_HOVER_HANDLE_TEXTURE;
    @Nullable
    private Identifier disabledHandleTexture = DEFAULT_DISABLED_HANDLE_TEXTURE;
    private boolean useVanillaTextures = false;
    private boolean sliderControlHovered;
    // AbstractSliderButton keeps its drag flag private, so this mirrors it only for the resize cursor over the narrowed interaction area.
    private boolean sliderControlDragging;

    protected SnappySlider(int x, int y, int width, @NotNull Component message, double initialValue) {
        this(x, y, width, DEFAULT_HEIGHT, message, initialValue);
    }

    protected SnappySlider(int x, int y, int width, int height, @NotNull Component message, double initialValue) {
        super(x, y, width, height, message, initialValue);
    }

    /**
     * Sets 70x8 bar textures for each render state. The first and last 20 pixels remain fixed while the 30-pixel center repeats horizontally. A null texture uses Minecraft's native slider background for that state.
     */
    @NotNull
    public SnappySlider setBarTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier disabledTexture) {
        this.idleBarTexture = idleTexture;
        this.hoverBarTexture = hoverTexture;
        this.disabledBarTexture = disabledTexture;
        return this;
    }

    /**
     * Sets fixed 8x8 handle textures for each render state. A null texture uses Minecraft's native slider handle for that state.
     */
    @NotNull
    public SnappySlider setHandleTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier disabledTexture) {
        this.idleHandleTexture = idleTexture;
        this.hoverHandleTexture = hoverTexture;
        this.disabledHandleTexture = disabledTexture;
        return this;
    }

    /**
     * Controls whether Minecraft's native slider sprites are rendered instead of this slider's custom bar and handle textures.
     */
    @NotNull
    public SnappySlider useVanillaTextures(boolean useVanillaTextures) {
        this.useVanillaTextures = useVanillaTextures;
        return this;
    }

    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.sliderControlHovered = this.isHovered() && this.isPointOverSliderControl(mouseX, mouseY);
        this.extractSliderBar(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        this.extractSliderHandle(graphics, this.getX() + (int) (this.value * (this.getWidth() - HANDLE_WIDTH)), this.getY(), this.getHeight());
        this.extractSliderLabel(graphics, LABEL_HORIZONTAL_MARGIN, LABEL_HORIZONTAL_MARGIN);
        this.handleCursor(graphics);
    }

    /**
     * Restricts mouse interaction to the bottom bar and handle strip. AbstractWidget calculates tooltip hover directly from the complete widget rectangle instead of this method, so tooltips intentionally remain available over the label area.
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.isActive() && this.isPointOverSliderControl(mouseX, mouseY);
    }

    @Override
    public void onClick(@NotNull MouseButtonEvent event, boolean doubleClick) {
        this.sliderControlDragging = this.active;
        super.onClick(event, doubleClick);
    }

    @Override
    public void onRelease(@NotNull MouseButtonEvent event) {
        this.sliderControlDragging = false;
        super.onRelease(event);
    }

    @Override
    protected void handleCursor(@NotNull GuiGraphicsExtractor graphics) {
        if (this.sliderControlHovered) {
            graphics.requestCursor(this.isActive() ? (this.sliderControlDragging ? CursorTypes.RESIZE_EW : CursorTypes.POINTING_HAND) : CursorTypes.NOT_ALLOWED);
        }
    }

    /**
     * Checks the shared bar/handle interaction strip. Subclasses with a deliberately offset track can override this to match their custom rendering bounds.
     */
    protected boolean isPointOverSliderControl(double mouseX, double mouseY) {
        int controlTop = Math.max(this.getY(), this.getBottom() - SLIDER_CONTROL_HEIGHT);
        return mouseX >= this.getX() && mouseX < this.getRight() && mouseY >= controlTop && mouseY < this.getBottom();
    }

    /**
     * Renders the configured slider bar along the bottom of custom bounds, allowing specialized sliders to reserve space beside the track.
     */
    protected final void extractSliderBar(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        Identifier barTexture = this.useVanillaTextures ? null : this.barTexture();
        if (barTexture == null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.vanillaBackgroundTexture(), x, y, width, height, ARGB.white(this.alpha));
            return;
        }
        renderBar(graphics, barTexture, x, y + height - BAR_TEXTURE_HEIGHT, width, ARGB.white(this.alpha));
    }

    /**
     * Renders the configured slider handle at its fixed 8x8 size, allowing specialized sliders to align it with an offset track.
     */
    protected final void extractSliderHandle(@NotNull GuiGraphicsExtractor graphics, int x, int y, int height) {
        Identifier handleTexture = this.useVanillaTextures ? null : this.handleTexture();
        if (handleTexture == null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.vanillaHandleTexture(), x, y, HANDLE_WIDTH, height, ARGB.white(this.alpha));
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, handleTexture, x, y + height - HANDLE_TEXTURE_HEIGHT, 0.0F, 0.0F, HANDLE_TEXTURE_WIDTH, HANDLE_TEXTURE_HEIGHT, HANDLE_TEXTURE_WIDTH, HANDLE_TEXTURE_HEIGHT, ARGB.white(this.alpha));
    }

    /**
     * Extracts the slider label with custom horizontal margins while retaining Minecraft's scrolling behavior for oversized labels.
     */
    protected final void extractSliderLabel(@NotNull GuiGraphicsExtractor graphics, int leftMargin, int rightMargin) {
        int left = Math.min(this.getRight(), this.getX() + Math.max(0, leftMargin));
        int right = Math.max(left, this.getRight() - Math.max(0, rightMargin));
        ActiveTextCollector textRenderer = graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE);
        textRenderer.acceptScrollingWithDefaultCenter(this.getMessage(), left, right, this.getY(), this.getY() + LABEL_RENDER_AREA_HEIGHT);
    }

    @Nullable
    private Identifier barTexture() {
        if (!this.active) {
            return this.disabledBarTexture;
        }
        return this.sliderControlHovered ? this.hoverBarTexture : this.idleBarTexture;
    }

    @Nullable
    private Identifier handleTexture() {
        if (!this.active) {
            return this.disabledHandleTexture;
        }
        return this.sliderControlHovered ? this.hoverHandleTexture : this.idleHandleTexture;
    }

    @NotNull
    private Identifier vanillaBackgroundTexture() {
        return this.isActive() && this.sliderControlHovered ? VANILLA_HIGHLIGHTED_BACKGROUND_TEXTURE : VANILLA_BACKGROUND_TEXTURE;
    }

    @NotNull
    private Identifier vanillaHandleTexture() {
        return this.isActive() && this.sliderControlHovered ? VANILLA_HIGHLIGHTED_HANDLE_TEXTURE : VANILLA_HANDLE_TEXTURE;
    }

    private static void renderBar(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier texture, int x, int y, int width, int color) {
        if (width <= 0) {
            return;
        }

        int leftWidth = Math.min(BAR_BORDER_WIDTH, (width + 1) / 2);
        int rightWidth = Math.min(BAR_BORDER_WIDTH, width - leftWidth);
        int centerWidth = width - leftWidth - rightWidth;
        renderBarRegion(graphics, texture, x, y, 0, leftWidth, color);

        int centerX = x + leftWidth;
        for (int renderedWidth = 0; renderedWidth < centerWidth; renderedWidth += BAR_CENTER_WIDTH) {
            int repeatWidth = Math.min(BAR_CENTER_WIDTH, centerWidth - renderedWidth);
            renderBarRegion(graphics, texture, centerX + renderedWidth, y, BAR_BORDER_WIDTH, repeatWidth, color);
        }

        renderBarRegion(graphics, texture, x + width - rightWidth, y, BAR_TEXTURE_WIDTH - rightWidth, rightWidth, color);
    }

    private static void renderBarRegion(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier texture, int x, int y, int sourceX, int width, int color) {
        if (width <= 0) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, sourceX, 0.0F, width, BAR_TEXTURE_HEIGHT, width, BAR_TEXTURE_HEIGHT, BAR_TEXTURE_WIDTH, BAR_TEXTURE_HEIGHT, color);
    }

}
