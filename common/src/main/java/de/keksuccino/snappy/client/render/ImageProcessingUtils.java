package de.keksuccino.snappy.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class ImageProcessingUtils {

    private ImageProcessingUtils() {
    }

    @NotNull
    public static NativeImage resizeBilinear(@NotNull NativeImage sourceImage, int targetWidth, int targetHeight) {
        NativeImage resized = new NativeImage(targetWidth, targetHeight, false);
        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();

        for (int y = 0; y < targetHeight; y++) {
            float sourceY = ((y + 0.5F) * sourceHeight / targetHeight) - 0.5F;
            for (int x = 0; x < targetWidth; x++) {
                float sourceX = ((x + 0.5F) * sourceWidth / targetWidth) - 0.5F;
                resized.setPixel(x, y, sampleBilinear(sourceImage, sourceX, sourceY));
            }
        }

        return resized;
    }

    private static int sampleBilinear(@NotNull NativeImage image, float x, float y) {
        int x0 = Mth.clamp((int) Math.floor(x), 0, image.getWidth() - 1);
        int y0 = Mth.clamp((int) Math.floor(y), 0, image.getHeight() - 1);
        int x1 = Mth.clamp(x0 + 1, 0, image.getWidth() - 1);
        int y1 = Mth.clamp(y0 + 1, 0, image.getHeight() - 1);
        float xBlend = Mth.clamp(x - x0, 0.0F, 1.0F);
        float yBlend = Mth.clamp(y - y0, 0.0F, 1.0F);

        int top = lerpColor(xBlend, image.getPixel(x0, y0), image.getPixel(x1, y0));
        int bottom = lerpColor(xBlend, image.getPixel(x0, y1), image.getPixel(x1, y1));
        return lerpColor(yBlend, top, bottom);
    }

    private static int lerpColor(float amount, int from, int to) {
        int alpha = Mth.lerpInt(amount, ARGB.alpha(from), ARGB.alpha(to));
        int red = Mth.lerpInt(amount, ARGB.red(from), ARGB.red(to));
        int green = Mth.lerpInt(amount, ARGB.green(from), ARGB.green(to));
        int blue = Mth.lerpInt(amount, ARGB.blue(from), ARGB.blue(to));
        return ARGB.color(alpha, red, green, blue);
    }

}
