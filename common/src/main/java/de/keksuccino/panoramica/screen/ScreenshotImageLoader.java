package de.keksuccino.panoramica.screen;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.panoramica.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;

public final class ScreenshotImageLoader {

    public static final int THUMBNAIL_NORMAL_MAX_WIDTH = 256;
    public static final int THUMBNAIL_NORMAL_MAX_HEIGHT = 144;
    public static final int THUMBNAIL_PANORAMA_WIDTH = 256;
    public static final int THUMBNAIL_PANORAMA_HEIGHT = 144;
    public static final int VIEWER_NORMAL_MAX_SIZE = 2048;
    public static final int VIEWER_PANORAMA_FACE_MAX_SIZE = 1024;
    public static final int VIEWER_BACKGROUND_FALLBACK_COLOR = 0xE0000000;
    private static final int VIEWER_BACKGROUND_BUCKET_BITS = 4;
    private static final int VIEWER_BACKGROUND_BUCKETS_PER_CHANNEL = 1 << VIEWER_BACKGROUND_BUCKET_BITS;
    private static final int VIEWER_BACKGROUND_BUCKET_COUNT = VIEWER_BACKGROUND_BUCKETS_PER_CHANNEL
            * VIEWER_BACKGROUND_BUCKETS_PER_CHANNEL
            * VIEWER_BACKGROUND_BUCKETS_PER_CHANNEL;
    private static final int VIEWER_BACKGROUND_BUCKET_SHIFT = 8 - VIEWER_BACKGROUND_BUCKET_BITS;
    private static final float VIEWER_BACKGROUND_DARKEN_FACTOR = 0.8F;

    private ScreenshotImageLoader() {
    }

    public static byte @NotNull [] readThumbnailBytes(@NotNull ScreenshotEntry entry) throws IOException {
        return Files.readAllBytes(entry.thumbnailPath());
    }

    @NotNull
    public static LoadedImage decodeThumbnail(@NotNull ScreenshotEntry entry, byte @NotNull [] bytes) throws IOException {
        try (NativeImage source = NativeImage.read(bytes)) {
            NativeImage thumbnail = entry.isPanorama()
                    ? resizeCover(source, THUMBNAIL_PANORAMA_WIDTH, THUMBNAIL_PANORAMA_HEIGHT)
                    : resizeToFit(source, THUMBNAIL_NORMAL_MAX_WIDTH, THUMBNAIL_NORMAL_MAX_HEIGHT);
            return new LoadedImage(thumbnail, source.getWidth(), source.getHeight());
        }
    }

    public static byte @NotNull [] readNormalViewerBytes(@NotNull ScreenshotEntry entry) throws IOException {
        return Files.readAllBytes(entry.path());
    }

    @NotNull
    public static LoadedImage decodeNormalViewerImage(byte @NotNull [] bytes) throws IOException {
        try (NativeImage source = NativeImage.read(bytes)) {
            NativeImage image = resizeToFit(source, VIEWER_NORMAL_MAX_SIZE, VIEWER_NORMAL_MAX_SIZE);
            return new LoadedImage(image, source.getWidth(), source.getHeight());
        }
    }

    public static int createViewerBackgroundColor(@NotNull NativeImage image) {
        ColorRangeAccumulator[] buckets = new ColorRangeAccumulator[VIEWER_BACKGROUND_BUCKET_COUNT];
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getPixel(x, y);
                int alpha = ARGB.alpha(color);
                if (alpha <= 0) {
                    continue;
                }

                int bucketIndex = backgroundBucketIndex(color);
                ColorRangeAccumulator bucket = buckets[bucketIndex];
                if (bucket == null) {
                    bucket = new ColorRangeAccumulator();
                    buckets[bucketIndex] = bucket;
                }
                bucket.add(color, alpha);
            }
        }

        ColorRangeAccumulator mostCommon = null;
        for (ColorRangeAccumulator bucket : buckets) {
            if (bucket != null && bucket.isMoreCommonThan(mostCommon)) {
                mostCommon = bucket;
            }
        }

        return mostCommon != null ? mostCommon.toBackgroundColor() : VIEWER_BACKGROUND_FALLBACK_COLOR;
    }

    public static byte @NotNull [][] readPanoramaViewerFaceBytes(@NotNull ScreenshotEntry entry) throws IOException {
        byte[][] faces = new byte[6][];
        for (int i = 0; i < faces.length; i++) {
            faces[i] = Files.readAllBytes(entry.path().resolve("panorama_" + i + ".png"));
        }
        return faces;
    }

    public static NativeImage @NotNull [] decodePanoramaViewerFaces(byte @NotNull [][] bytes) throws IOException {
        NativeImage[] faces = new NativeImage[6];
        int expectedSize = -1;
        try {
            for (int i = 0; i < faces.length; i++) {
                try (NativeImage source = NativeImage.read(bytes[i])) {
                    if (source.getWidth() != source.getHeight()) {
                        throw new IOException("Panorama face " + i + " is not square: " + source.getWidth() + "x" + source.getHeight());
                    }
                    if (expectedSize < 0) {
                        expectedSize = source.getWidth();
                    } else if (source.getWidth() != expectedSize) {
                        throw new IOException("Panorama face " + i + " is " + source.getWidth() + "x" + source.getHeight() + ", expected " + expectedSize + "x" + expectedSize);
                    }
                    int targetSize = Math.min(source.getWidth(), VIEWER_PANORAMA_FACE_MAX_SIZE);
                    faces[i] = resizeExact(source, targetSize, targetSize);
                }
            }
            return faces;
        } catch (IOException | RuntimeException ex) {
            closeImages(faces);
            throw ex;
        }
    }

    @NotNull
    private static NativeImage resizeToFit(@NotNull NativeImage source, int maxWidth, int maxHeight) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        float scale = Math.min(maxWidth / (float) sourceWidth, maxHeight / (float) sourceHeight);
        scale = Math.min(1.0F, scale);
        int targetWidth = Math.max(1, Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, Math.round(sourceHeight * scale));
        return resizeExact(source, targetWidth, targetHeight);
    }

    @NotNull
    private static NativeImage resizeCover(@NotNull NativeImage source, int targetWidth, int targetHeight) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        float sourceAspect = sourceWidth / (float) sourceHeight;
        float targetAspect = targetWidth / (float) targetHeight;
        int cropWidth = sourceWidth;
        int cropHeight = sourceHeight;
        int cropX = 0;
        int cropY = 0;

        if (sourceAspect > targetAspect) {
            cropWidth = Math.max(1, Math.round(sourceHeight * targetAspect));
            cropX = (sourceWidth - cropWidth) / 2;
        } else if (sourceAspect < targetAspect) {
            cropHeight = Math.max(1, Math.round(sourceWidth / targetAspect));
            cropY = (sourceHeight - cropHeight) / 2;
        }

        NativeImage resized = new NativeImage(targetWidth, targetHeight, false);
        source.resizeSubRectTo(cropX, cropY, cropWidth, cropHeight, resized);
        return resized;
    }

    @NotNull
    private static NativeImage resizeExact(@NotNull NativeImage source, int targetWidth, int targetHeight) {
        NativeImage resized = new NativeImage(targetWidth, targetHeight, false);
        source.resizeSubRectTo(0, 0, source.getWidth(), source.getHeight(), resized);
        return resized;
    }

    private static void closeImages(NativeImage @NotNull [] images) {
        for (NativeImage image : images) {
            if (image != null) {
                image.close();
            }
        }
    }

    private static int backgroundBucketIndex(int color) {
        int red = ARGB.red(color) >> VIEWER_BACKGROUND_BUCKET_SHIFT;
        int green = ARGB.green(color) >> VIEWER_BACKGROUND_BUCKET_SHIFT;
        int blue = ARGB.blue(color) >> VIEWER_BACKGROUND_BUCKET_SHIFT;
        return (red << (VIEWER_BACKGROUND_BUCKET_BITS * 2)) | (green << VIEWER_BACKGROUND_BUCKET_BITS) | blue;
    }

    private static final class ColorRangeAccumulator {

        private long red;
        private long green;
        private long blue;
        private long weight;
        private long count;

        private void add(int color, int alpha) {
            this.red += (long) ARGB.red(color) * alpha;
            this.green += (long) ARGB.green(color) * alpha;
            this.blue += (long) ARGB.blue(color) * alpha;
            this.weight += alpha;
            this.count++;
        }

        private boolean isMoreCommonThan(@Nullable ColorRangeAccumulator other) {
            return other == null || this.count > other.count || this.count == other.count && this.weight > other.weight;
        }

        private int toBackgroundColor() {
            int averageRed = Math.round(this.red / (float) this.weight);
            int averageGreen = Math.round(this.green / (float) this.weight);
            int averageBlue = Math.round(this.blue / (float) this.weight);
            return ARGB.color(
                    0xFF,
                    darkenChannel(averageRed),
                    darkenChannel(averageGreen),
                    darkenChannel(averageBlue)
            );
        }

        private static int darkenChannel(int channel) {
            return Math.round(channel * VIEWER_BACKGROUND_DARKEN_FACTOR);
        }
    }

    public record LoadedImage(@NotNull NativeImage image, int sourceWidth, int sourceHeight) {
    }

}
