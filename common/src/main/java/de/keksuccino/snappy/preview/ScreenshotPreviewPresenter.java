package de.keksuccino.snappy.preview;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

final class ScreenshotPreviewPresenter {

    static final long DISPLAY_MILLIS = 10_000L;

    private static final int MARGIN = 8;
    private static final long SLIDE_MILLIS = 350L;
    private static final long HOVER_ANIMATION_MILLIS = 120L;
    private static final float HOVER_GROWTH = 0.05F;

    private float hoverProgress;
    private long lastHoverUpdateMillis;
    private boolean playedSlideOutSound;

    void reset() {
        this.hoverProgress = 0.0F;
        this.lastHoverUpdateMillis = 0L;
        this.playedSlideOutSound = false;
    }

    @NotNull
    PreviewBounds bounds(int previewWidth, int previewHeight, int borderSize, long ageMillis, float scale) {
        float slide = slideAmount(ageMillis);
        int outerWidth = previewWidth + (borderSize * 2);
        int outerHeight = previewHeight + (borderSize * 2);
        int x = Math.round(MARGIN - outerWidth + outerWidth * slide);
        int y = MARGIN;
        return new PreviewBounds(x, y, Math.round(outerWidth * scale), Math.round(outerHeight * scale));
    }

    void updateHoverProgress(long nowMillis, boolean hovered) {
        if (this.lastHoverUpdateMillis == 0L) {
            this.lastHoverUpdateMillis = nowMillis;
        }

        float amount = Mth.clamp((nowMillis - this.lastHoverUpdateMillis) / (float) HOVER_ANIMATION_MILLIS, 0.0F, 1.0F);
        this.hoverProgress = Mth.lerp(amount, this.hoverProgress, hovered ? 1.0F : 0.0F);
        this.lastHoverUpdateMillis = nowMillis;
    }

    float hoverScale() {
        return 1.0F + (HOVER_GROWTH * easeOutCubic(this.hoverProgress));
    }

    boolean consumeSlideOutSound(long ageMillis) {
        if (!this.playedSlideOutSound && ageMillis >= DISPLAY_MILLIS - SLIDE_MILLIS) {
            this.playedSlideOutSound = true;
            return true;
        }
        return false;
    }

    private static float slideAmount(long ageMillis) {
        if (ageMillis < SLIDE_MILLIS) {
            return easeOutCubic(ageMillis / (float) SLIDE_MILLIS);
        }

        long remainingMillis = DISPLAY_MILLIS - ageMillis;
        if (remainingMillis < SLIDE_MILLIS) {
            return easeOutCubic(remainingMillis / (float) SLIDE_MILLIS);
        }

        return 1.0F;
    }

    private static float easeOutCubic(float amount) {
        float clamped = Mth.clamp(amount, 0.0F, 1.0F);
        float inverse = 1.0F - clamped;
        return 1.0F - (inverse * inverse * inverse);
    }

    record PreviewBounds(int x, int y, int width, int height) {

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

}
