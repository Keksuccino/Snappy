package de.keksuccino.panoramica.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class PhotoModeColorPicker {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 160;
    public static final String EMPTY_HEX_COLOR = "-----";
    private static final int PADDING = 8;
    private static final int HEADER_HEIGHT = 17;
    private static final int PREVIEW_SIZE = 22;
    private static final int TRACK_HEIGHT = 10;
    private static final int TRACK_GAP = 15;
    private static final int TRACK_LABEL_WIDTH = 11;
    private static final int FOOTER_BUTTON_HEIGHT = 20;
    private static final int FOOTER_BUTTON_GAP = 4;
    private static final int PANEL_BACKGROUND_COLOR = ARGB.color(184, 0, 0, 0);
    private static final int PANEL_BORDER_COLOR = ARGB.color(210, 116, 128, 142);
    private static final int PANEL_ACCENT_COLOR = ARGB.color(255, 255, 209, 102);
    private static final int SECTION_BACKGROUND_COLOR = ARGB.color(82, 24, 28, 34);
    private static final int TRACK_BORDER_COLOR = ARGB.color(255, 20, 20, 20);
    private static final int TRACK_MARKER_DARK = ARGB.color(255, 12, 12, 12);
    private static final int TRACK_MARKER_LIGHT = ARGB.color(255, 245, 245, 245);
    private static final int PREVIEW_BORDER_COLOR = ARGB.color(255, 235, 235, 235);
    private static final float CHANNEL_EPSILON = 1.0E-4F;

    private final Component title;
    private final Supplier<@Nullable Integer> colorSupplier;
    private final IntSupplier editColorSupplier;
    private final Supplier<@Nullable Integer> defaultColorSupplier;
    private final Consumer<@Nullable Integer> colorConsumer;
    private int x;
    private int y;
    private boolean syncedColorInitialized;
    private boolean syncedEmpty;
    private int syncedColor;
    private float hue;
    private float saturation;
    private float brightness;
    @Nullable
    private Channel draggingChannel;

    public PhotoModeColorPicker(
            @NotNull Component title,
            @NotNull Supplier<@Nullable Integer> colorSupplier,
            @NotNull IntSupplier editColorSupplier,
            @NotNull Supplier<@Nullable Integer> defaultColorSupplier,
            @NotNull Consumer<@Nullable Integer> colorConsumer
    ) {
        this.title = title;
        this.colorSupplier = colorSupplier;
        this.editColorSupplier = editColorSupplier;
        this.defaultColorSupplier = defaultColorSupplier;
        this.colorConsumer = colorConsumer;
        this.syncFromSupplier();
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + WIDTH && mouseY >= this.y && mouseY <= this.y + HEIGHT;
    }

    public int doneButtonX() {
        return this.x + PADDING;
    }

    public int doneButtonY() {
        return this.y + HEIGHT - PADDING - FOOTER_BUTTON_HEIGHT;
    }

    public int doneButtonWidth() {
        return WIDTH - PADDING * 2;
    }

    public int doneButtonHeight() {
        return FOOTER_BUTTON_HEIGHT;
    }

    public int resetButtonX() {
        return this.x + PADDING;
    }

    public int resetButtonY() {
        return this.doneButtonY() - FOOTER_BUTTON_GAP - FOOTER_BUTTON_HEIGHT;
    }

    public int resetButtonWidth() {
        return WIDTH - PADDING * 2;
    }

    public int resetButtonHeight() {
        return FOOTER_BUTTON_HEIGHT;
    }

    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || !this.contains(event.x(), event.y())) {
            return false;
        }

        Channel channel = this.channelAt(event.x(), event.y());
        if (channel == null) {
            return false;
        }

        this.draggingChannel = channel;
        this.updateChannel(channel, event.x());
        return true;
    }

    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dx, double dy) {
        if (event.button() != 0 || this.draggingChannel == null) {
            return false;
        }

        this.updateChannel(this.draggingChannel, event.x());
        return true;
    }

    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (event.button() != 0 || this.draggingChannel == null) {
            return false;
        }

        this.draggingChannel = null;
        return true;
    }

    public void resetToDefault() {
        this.colorConsumer.accept(opaqueOrNull(this.defaultColorSupplier.get()));
        this.syncFromSupplier();
    }

    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, @NotNull Font font, int mouseX, int mouseY) {
        this.syncFromSupplier();
        if (this.channelAt(mouseX, mouseY) != null || this.draggingChannel != null) {
            graphics.requestCursor(CursorTypes.RESIZE_EW);
        }

        graphics.fill(this.x, this.y, this.x + WIDTH, this.y + HEIGHT, PANEL_BACKGROUND_COLOR);
        graphics.outline(this.x, this.y, WIDTH, HEIGHT, PANEL_BORDER_COLOR);
        graphics.centeredText(font, this.title, this.x + WIDTH / 2, this.y + 7, PANEL_ACCENT_COLOR);

        int contentX = this.x + PADDING;
        int previewY = this.y + PADDING + HEADER_HEIGHT;
        this.renderSwatch(graphics, contentX, previewY, PREVIEW_SIZE, this.currentValue());

        int hexX = contentX + PREVIEW_SIZE + 8;
        int hexY = previewY + 7;
        graphics.text(font, Component.literal(formatHexColor(this.currentValue())), hexX, hexY, 0xFFFFFFFF);

        int tracksY = previewY + PREVIEW_SIZE + 11;
        this.renderTrack(graphics, font, Channel.HUE, tracksY);
        this.renderTrack(graphics, font, Channel.SATURATION, tracksY + TRACK_GAP);
        this.renderTrack(graphics, font, Channel.BRIGHTNESS, tracksY + TRACK_GAP * 2);
    }

    @NotNull
    public static String formatHexColor(@Nullable Integer color) {
        if (color == null) {
            return EMPTY_HEX_COLOR;
        }
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private void syncFromSupplier() {
        @Nullable Integer suppliedColor = opaqueOrNull(this.colorSupplier.get());
        int color = suppliedColor == null ? ARGB.opaque(this.editColorSupplier.getAsInt()) : suppliedColor;
        boolean empty = suppliedColor == null;
        if (this.syncedColorInitialized && this.syncedEmpty == empty && this.syncedColor == color) {
            return;
        }

        Hsv hsv = Hsv.fromRgb(color);
        if (!this.syncedColorInitialized || empty || hsv.saturation() > CHANNEL_EPSILON && hsv.brightness() > CHANNEL_EPSILON) {
            this.hue = hsv.hue();
        }
        this.saturation = hsv.saturation();
        this.brightness = hsv.brightness();
        this.syncedColorInitialized = true;
        this.syncedEmpty = empty;
        this.syncedColor = color;
    }

    private void updateChannel(@NotNull Channel channel, double mouseX) {
        double normalized = Mth.clamp((mouseX - this.trackX()) / (double) (this.trackWidth() - 1), 0.0D, 1.0D);
        switch (channel) {
            case HUE -> this.hue = (float) normalized;
            case SATURATION -> this.saturation = (float) normalized;
            case BRIGHTNESS -> this.brightness = (float) normalized;
        }
        int color = this.currentColor();
        this.syncedColorInitialized = true;
        this.syncedEmpty = false;
        this.syncedColor = color;
        this.colorConsumer.accept(color);
    }

    @Nullable
    private Channel channelAt(double mouseX, double mouseY) {
        if (this.isInsideTrack(mouseX, mouseY, this.trackY(Channel.HUE))) {
            return Channel.HUE;
        }
        if (this.isInsideTrack(mouseX, mouseY, this.trackY(Channel.SATURATION))) {
            return Channel.SATURATION;
        }
        if (this.isInsideTrack(mouseX, mouseY, this.trackY(Channel.BRIGHTNESS))) {
            return Channel.BRIGHTNESS;
        }
        return null;
    }

    private boolean isInsideTrack(double mouseX, double mouseY, int trackY) {
        int trackX = this.trackX();
        return mouseX >= trackX && mouseX <= trackX + this.trackWidth() && mouseY >= trackY - 2 && mouseY <= trackY + TRACK_HEIGHT + 2;
    }

    private void renderTrack(@NotNull GuiGraphicsExtractor graphics, @NotNull Font font, @NotNull Channel channel, int y) {
        int labelX = this.x + PADDING;
        int trackX = this.trackX();
        int trackWidth = this.trackWidth();
        graphics.text(font, Component.translatable(channel.labelKey()), labelX, y + 1, 0xFFFFFFFF);
        graphics.fill(trackX - 1, y - 1, trackX + trackWidth + 1, y + TRACK_HEIGHT + 1, TRACK_BORDER_COLOR);
        graphics.fill(trackX, y, trackX + trackWidth, y + TRACK_HEIGHT, SECTION_BACKGROUND_COLOR);

        for (int offset = 0; offset < trackWidth; offset++) {
            float value = trackWidth <= 1 ? 0.0F : offset / (float) (trackWidth - 1);
            graphics.fill(trackX + offset, y, trackX + offset + 1, y + TRACK_HEIGHT, this.trackColor(channel, value));
        }

        int markerX = trackX + Math.round(channel.value(this) * (trackWidth - 1));
        graphics.fill(markerX - 2, y - 2, markerX + 3, y + TRACK_HEIGHT + 2, TRACK_MARKER_DARK);
        graphics.fill(markerX - 1, y - 1, markerX + 2, y + TRACK_HEIGHT + 1, TRACK_MARKER_LIGHT);
    }

    private void renderSwatch(@NotNull GuiGraphicsExtractor graphics, int x, int y, int size, @Nullable Integer color) {
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, TRACK_BORDER_COLOR);
        if (color == null) {
            graphics.fill(x, y, x + size, y + size, SECTION_BACKGROUND_COLOR);
            for (int offset = 0; offset < size; offset++) {
                graphics.fill(x + offset, y + size - 1 - offset, x + offset + 1, y + size - offset, PANEL_ACCENT_COLOR);
            }
        } else {
            graphics.fill(x, y, x + size, y + size, ARGB.opaque(color));
        }
        graphics.outline(x, y, size, size, PREVIEW_BORDER_COLOR);
    }

    private int trackColor(@NotNull Channel channel, float value) {
        return switch (channel) {
            case HUE -> Mth.hsvToArgb(this.hueForRender(value), 1.0F, 1.0F, 255);
            case SATURATION -> Mth.hsvToArgb(this.hueForRender(this.hue), value, this.brightness, 255);
            case BRIGHTNESS -> Mth.hsvToArgb(this.hueForRender(this.hue), this.saturation, value, 255);
        };
    }

    private int currentColor() {
        return Mth.hsvToArgb(this.hueForRender(this.hue), this.saturation, this.brightness, 255);
    }

    @Nullable
    private Integer currentValue() {
        return this.syncedEmpty ? null : this.currentColor();
    }

    @Nullable
    private static Integer opaqueOrNull(@Nullable Integer color) {
        return color == null ? null : ARGB.opaque(color);
    }

    private float hueForRender(float value) {
        return value >= 1.0F ? 0.0F : Mth.clamp(value, 0.0F, 1.0F);
    }

    private int trackX() {
        return this.x + PADDING + TRACK_LABEL_WIDTH;
    }

    private int trackWidth() {
        return WIDTH - PADDING * 2 - TRACK_LABEL_WIDTH;
    }

    private int trackY(@NotNull Channel channel) {
        return this.y + PADDING + HEADER_HEIGHT + PREVIEW_SIZE + 11 + channel.ordinal() * TRACK_GAP;
    }

    private enum Channel {
        HUE("panoramica.photo_mode.color_picker.hue") {
            @Override
            float value(@NotNull PhotoModeColorPicker picker) {
                return picker.hue;
            }
        },
        SATURATION("panoramica.photo_mode.color_picker.saturation") {
            @Override
            float value(@NotNull PhotoModeColorPicker picker) {
                return picker.saturation;
            }
        },
        BRIGHTNESS("panoramica.photo_mode.color_picker.brightness") {
            @Override
            float value(@NotNull PhotoModeColorPicker picker) {
                return picker.brightness;
            }
        };

        private final String labelKey;

        Channel(@NotNull String labelKey) {
            this.labelKey = labelKey;
        }

        @NotNull
        private String labelKey() {
            return this.labelKey;
        }

        abstract float value(@NotNull PhotoModeColorPicker picker);
    }

    private record Hsv(float hue, float saturation, float brightness) {

        @NotNull
        private static Hsv fromRgb(int color) {
            float red = ARGB.red(color) / 255.0F;
            float green = ARGB.green(color) / 255.0F;
            float blue = ARGB.blue(color) / 255.0F;
            float max = Math.max(red, Math.max(green, blue));
            float min = Math.min(red, Math.min(green, blue));
            float delta = max - min;
            float hue = 0.0F;
            if (delta > CHANNEL_EPSILON) {
                if (max == red) {
                    hue = ((green - blue) / delta) % 6.0F;
                } else if (max == green) {
                    hue = (blue - red) / delta + 2.0F;
                } else {
                    hue = (red - green) / delta + 4.0F;
                }
                hue /= 6.0F;
                if (hue < 0.0F) {
                    hue += 1.0F;
                }
            }
            float saturation = max <= CHANNEL_EPSILON ? 0.0F : delta / max;
            return new Hsv(Mth.clamp(hue, 0.0F, 1.0F), Mth.clamp(saturation, 0.0F, 1.0F), Mth.clamp(max, 0.0F, 1.0F));
        }

    }

}
