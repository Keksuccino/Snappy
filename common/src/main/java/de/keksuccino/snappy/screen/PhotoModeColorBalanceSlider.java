package de.keksuccino.snappy.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntSupplier;

final class PhotoModeColorBalanceSlider extends PhotoModeSlider {

    // These mirror vanilla AbstractSliderButton sprites so the swatch slider can reserve left-side room without re-skinning the control.
    private static final Identifier SLIDER_SPRITE = Identifier.withDefaultNamespace("widget/slider");
    private static final Identifier HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/slider_highlighted");
    private static final Identifier SLIDER_HANDLE_SPRITE = Identifier.withDefaultNamespace("widget/slider_handle");
    private static final Identifier SLIDER_HANDLE_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");
    private static final int HANDLE_WIDTH = 8;
    private static final int HANDLE_HALF_WIDTH = 4;
    private static final int SWATCH_X_OFFSET = 6;
    private static final int TRACK_X_OFFSET = SWATCH_X_OFFSET + PhotoModeColorSwatch.SIZE + 8;
    private static final int TEXT_MARGIN = TRACK_X_OFFSET;

    private final IntSupplier colorSupplier;
    private boolean dragging;

    PhotoModeColorBalanceSlider(int x, int y, int width, int height, double minValue, double maxValue, double currentValue, double defaultValue, double snapRadius, double actualStep, @NotNull DoubleConsumer valueConsumer, @NotNull DoubleFunction<Component> messageFactory, @NotNull IntSupplier colorSupplier) {
        super(x, y, width, height, minValue, maxValue, currentValue, defaultValue, snapRadius, actualStep, valueConsumer, messageFactory);
        this.colorSupplier = colorSupplier;
    }

    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sliderSprite(), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.handleSprite(), this.handleX(), this.getY(), HANDLE_WIDTH, this.getHeight(), ARGB.white(this.alpha));
        PhotoModeColorSwatch.render(graphics, this.getX() + SWATCH_X_OFFSET, this.getY() + (this.getHeight() - PhotoModeColorSwatch.SIZE) / 2, this.colorSupplier.getAsInt());
        this.extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), this.getMessage(), TEXT_MARGIN);
        this.handleCursor(graphics);
    }

    @Override
    public void onClick(@NotNull MouseButtonEvent event, boolean doubleClick) {
        this.dragging = this.active;
        this.prepareDefaultSnapFromCurrentValue();
        this.setValueFromTrackMouse(event);
    }

    @Override
    protected void onDrag(@NotNull MouseButtonEvent event, double dx, double dy) {
        this.setValueFromTrackMouse(event);
    }

    @Override
    public void onRelease(@NotNull MouseButtonEvent event) {
        this.dragging = false;
        super.onRelease(event);
    }

    @Override
    protected void handleCursor(@NotNull GuiGraphicsExtractor graphics) {
        if (this.isHovered()) {
            graphics.requestCursor(this.isActive() ? (this.dragging ? CursorTypes.RESIZE_EW : CursorTypes.POINTING_HAND) : CursorTypes.NOT_ALLOWED);
        }
    }

    @NotNull
    private Identifier sliderSprite() {
        return this.isActive() && this.isFocused() && !this.canChangeValue ? HIGHLIGHTED_SPRITE : SLIDER_SPRITE;
    }

    @NotNull
    private Identifier handleSprite() {
        return !this.isActive() || !this.isHovered && !this.canChangeValue ? SLIDER_HANDLE_SPRITE : SLIDER_HANDLE_HIGHLIGHTED_SPRITE;
    }

    private void setValueFromTrackMouse(@NotNull MouseButtonEvent event) {
        this.setValue((event.x() - (this.trackX() + HANDLE_HALF_WIDTH)) / Math.max(1.0D, this.trackWidth() - HANDLE_WIDTH));
    }

    private int handleX() {
        return this.trackX() + (int) (this.value * (this.trackWidth() - HANDLE_WIDTH));
    }

    private int trackX() {
        return this.getX() + TRACK_X_OFFSET;
    }

    private int trackWidth() {
        return Math.max(HANDLE_WIDTH, this.getWidth() - TRACK_X_OFFSET);
    }

}
