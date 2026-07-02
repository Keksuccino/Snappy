package de.keksuccino.panoramica.screen;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.panoramica.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;

public final class ScreenshotImageLoader {

    public static final int THUMBNAIL_NORMAL_MAX_WIDTH = 256;
    public static final int THUMBNAIL_NORMAL_MAX_HEIGHT = 144;
    public static final int THUMBNAIL_PANORAMA_WIDTH = 256;
    public static final int THUMBNAIL_PANORAMA_HEIGHT = 144;
    public static final int VIEWER_NORMAL_MAX_SIZE = 2048;
    public static final int VIEWER_PANORAMA_FACE_MAX_SIZE = 1024;

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

    public record LoadedImage(@NotNull NativeImage image, int sourceWidth, int sourceHeight) {
    }

}
