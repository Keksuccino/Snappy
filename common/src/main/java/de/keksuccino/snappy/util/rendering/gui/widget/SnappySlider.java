package de.keksuccino.snappy.util.rendering.gui.widget;

import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.client.gui.GuiBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Reusable slider base with independently configurable background and handle textures in each render state.
 */
public abstract class SnappySlider extends AbstractSliderButton {

    public static final Identifier DEFAULT_IDLE_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/background/normal_20x20.png");
    public static final Identifier DEFAULT_HOVER_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/background/hover_20x20.png");
    public static final Identifier DEFAULT_DISABLED_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/background/disabled_20x20.png");
    public static final Identifier DEFAULT_IDLE_HANDLE_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/handle/normal_8x20.png");
    public static final Identifier DEFAULT_HOVER_HANDLE_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/handle/hover_8x20.png");
    public static final Identifier DEFAULT_DISABLED_HANDLE_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/sliders/handle/disabled_8x20.png");

    private static final Identifier VANILLA_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("widget/slider");
    private static final Identifier VANILLA_HIGHLIGHTED_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("widget/slider_highlighted");
    private static final Identifier VANILLA_HANDLE_TEXTURE = Identifier.withDefaultNamespace("widget/slider_handle");
    private static final Identifier VANILLA_HIGHLIGHTED_HANDLE_TEXTURE = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");
    private static final int BACKGROUND_TEXTURE_SIZE = 20;
    private static final int BACKGROUND_BORDER_SIZE = 4;
    private static final int HANDLE_TEXTURE_WIDTH = 8;
    private static final int HANDLE_TEXTURE_HEIGHT = 20;
    private static final int HANDLE_VERTICAL_BORDER_SIZE = 4;

    @Nullable
    private Identifier idleBackgroundTexture = DEFAULT_IDLE_BACKGROUND_TEXTURE;
    @Nullable
    private Identifier hoverBackgroundTexture = DEFAULT_HOVER_BACKGROUND_TEXTURE;
    @Nullable
    private Identifier disabledBackgroundTexture = DEFAULT_DISABLED_BACKGROUND_TEXTURE;
    @Nullable
    private Identifier idleHandleTexture = DEFAULT_IDLE_HANDLE_TEXTURE;
    @Nullable
    private Identifier hoverHandleTexture = DEFAULT_HOVER_HANDLE_TEXTURE;
    @Nullable
    private Identifier disabledHandleTexture = DEFAULT_DISABLED_HANDLE_TEXTURE;
    private boolean useVanillaTextures = false;

    protected SnappySlider(int x, int y, int width, int height, @NotNull Component message, double initialValue) {
        super(x, y, width, height, message, initialValue);
    }

    /**
     * Sets 20x20 background textures for each render state. Each custom texture is nine-sliced with a 4-pixel border on every side. A null texture uses Minecraft's native slider background for that state. The hover texture also applies while keyboard-focused.
     */
    @NotNull
    public SnappySlider setBackgroundTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier disabledTexture) {
        this.idleBackgroundTexture = idleTexture;
        this.hoverBackgroundTexture = hoverTexture;
        this.disabledBackgroundTexture = disabledTexture;
        return this;
    }

    /**
     * Sets 8x20 handle textures for each render state. Each custom texture is sliced only vertically, retaining 4-pixel top and bottom borders while always rendering the complete 8-pixel width. A null texture uses Minecraft's native slider handle for that state. The hover texture also applies while keyboard-focused.
     */
    @NotNull
    public SnappySlider setHandleTextures(@Nullable Identifier idleTexture, @Nullable Identifier hoverTexture, @Nullable Identifier disabledTexture) {
        this.idleHandleTexture = idleTexture;
        this.hoverHandleTexture = hoverTexture;
        this.disabledHandleTexture = disabledTexture;
        return this;
    }

    /**
     * Controls whether Minecraft's native slider sprites are rendered instead of this slider's custom background and handle textures.
     */
    @NotNull
    public SnappySlider useVanillaTextures(boolean useVanillaTextures) {
        this.useVanillaTextures = useVanillaTextures;
        return this;
    }

    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractSliderBackground(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        this.extractSliderHandle(graphics, this.getX() + (int) (this.value * (this.getWidth() - HANDLE_WIDTH)), this.getY(), this.getHeight());
        this.extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), this.getMessage(), TEXT_MARGIN);
        this.handleCursor(graphics);
    }

    /**
     * Renders the configured slider background inside custom bounds, allowing specialized sliders to reserve space beside the track.
     */
    protected final void extractSliderBackground(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        Identifier backgroundTexture = this.useVanillaTextures ? null : this.backgroundTexture();
        if (backgroundTexture == null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.vanillaBackgroundTexture(), x, y, width, height, ARGB.white(this.alpha));
            return;
        }
        GuiBackground.render(graphics, backgroundTexture, x, y, width, height, BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, BACKGROUND_BORDER_SIZE, ARGB.white(this.alpha));
    }

    /**
     * Renders the configured slider handle at its fixed 8-pixel width, allowing specialized sliders to align it with an offset track without permitting horizontal stretching.
     */
    protected final void extractSliderHandle(@NotNull GuiGraphicsExtractor graphics, int x, int y, int height) {
        Identifier handleTexture = this.useVanillaTextures ? null : this.handleTexture();
        if (handleTexture == null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.vanillaHandleTexture(), x, y, HANDLE_WIDTH, height, ARGB.white(this.alpha));
            return;
        }
        GuiBackground.render(graphics, handleTexture, x, y, HANDLE_WIDTH, height, HANDLE_TEXTURE_WIDTH, HANDLE_TEXTURE_HEIGHT, 0, HANDLE_VERTICAL_BORDER_SIZE, 0, HANDLE_VERTICAL_BORDER_SIZE, ARGB.white(this.alpha));
    }

    @Nullable
    private Identifier backgroundTexture() {
        if (!this.active) {
            return this.disabledBackgroundTexture;
        }
        return this.isHoveredOrFocused() ? this.hoverBackgroundTexture : this.idleBackgroundTexture;
    }

    @Nullable
    private Identifier handleTexture() {
        if (!this.active) {
            return this.disabledHandleTexture;
        }
        return this.isHoveredOrFocused() ? this.hoverHandleTexture : this.idleHandleTexture;
    }

    @NotNull
    private Identifier vanillaBackgroundTexture() {
        return this.isActive() && this.isFocused() && !this.canChangeValue ? VANILLA_HIGHLIGHTED_BACKGROUND_TEXTURE : VANILLA_BACKGROUND_TEXTURE;
    }

    @NotNull
    private Identifier vanillaHandleTexture() {
        return !this.isActive() || !this.isHovered && !this.canChangeValue ? VANILLA_HANDLE_TEXTURE : VANILLA_HIGHLIGHTED_HANDLE_TEXTURE;
    }

}
