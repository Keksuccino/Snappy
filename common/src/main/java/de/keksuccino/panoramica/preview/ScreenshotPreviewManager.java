package de.keksuccino.panoramica.preview;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import de.keksuccino.panoramica.Options;
import de.keksuccino.panoramica.Panoramica;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ScreenshotPreviewManager {

    private static final int PREVIEW_WIDTH = 100;
    private static final int NORMAL_TEXTURE_WIDTH = 400;
    private static final int PANORAMA_TEXTURE_WIDTH = 400;
    private static final int PANORAMA_TEXTURE_HEIGHT = Math.max(1, Math.round(PANORAMA_TEXTURE_WIDTH * 9.0F / 16.0F));
    private static final int PANORAMA_RENDER_HEIGHT = Math.max(1, Math.round(PREVIEW_WIDTH * 9.0F / 16.0F));
    private static final int PANORAMA_FACE_SIZE = 256;
    private static final int BORDER_SIZE = 1;
    private static final int MARGIN = 8;
    private static final long DISPLAY_MILLIS = 10_000L;
    private static final long SLIDE_MILLIS = 350L;
    private static final Object PANORAMA_LOCK = new Object();
    private static final Identifier FLAT_PREVIEW_TEXTURE_ID = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "dynamic/screenshot_preview");

    @Nullable
    private static Preview currentPreview;
    @Nullable
    private static NativeImage[] pendingPanoramaFaces;
    private static int pendingPanoramaFaceCount;
    private static float panoramaSpin;
    private static final PreviewCubeMapRenderer PANORAMA_RENDERER = new PreviewCubeMapRenderer(PANORAMA_TEXTURE_WIDTH, PANORAMA_TEXTURE_HEIGHT);

    private ScreenshotPreviewManager() {
    }

    public static boolean shouldShowNormalScreenshots() {
        return Panoramica.getOptions().getScreenshotPreviewMode().showNormalScreenshots;
    }

    public static boolean shouldShowPanoramaScreenshots() {
        return Panoramica.getOptions().getScreenshotPreviewMode().showPanoramaScreenshots;
    }

    public static void showNormalScreenshot(@NotNull NativeImage sourceImage) {
        if (!shouldShowNormalScreenshots()) {
            return;
        }

        NativeImage previewImage;
        try {
            int previewHeight = Math.max(1, Math.round(NORMAL_TEXTURE_WIDTH * (sourceImage.getHeight() / (float) sourceImage.getWidth())));
            previewImage = resize(sourceImage, NORMAL_TEXTURE_WIDTH, previewHeight);
        } catch (Exception ex) {
            Panoramica.getLogger().warn("[PANORAMICA] Could not prepare normal screenshot preview.", ex);
            return;
        }

        Minecraft.getInstance().execute(() -> setFlatPreview(previewImage));
    }

    public static void beginPanoramaCapture() {
        synchronized (PANORAMA_LOCK) {
            closePendingPanoramaFaces_Panoramica();
            if (shouldShowPanoramaScreenshots()) {
                pendingPanoramaFaces = new NativeImage[6];
                pendingPanoramaFaceCount = 0;
            }
        }
    }

    public static void collectPanoramaFace(int face, @NotNull NativeImage sourceImage) {
        if (!shouldShowPanoramaScreenshots()) {
            return;
        }
        if (face < 0 || face >= 6) {
            return;
        }

        NativeImage previewFace;
        try {
            previewFace = resize(sourceImage, PANORAMA_FACE_SIZE, PANORAMA_FACE_SIZE);
        } catch (Exception ex) {
            Panoramica.getLogger().warn("[PANORAMICA] Could not prepare panorama preview face {}.", face, ex);
            return;
        }

        NativeImage[] completedFaces = null;
        synchronized (PANORAMA_LOCK) {
            if (pendingPanoramaFaces == null) {
                pendingPanoramaFaces = new NativeImage[6];
                pendingPanoramaFaceCount = 0;
            }

            if (pendingPanoramaFaces[face] == null) {
                pendingPanoramaFaceCount++;
            } else {
                pendingPanoramaFaces[face].close();
            }
            pendingPanoramaFaces[face] = previewFace;

            if (pendingPanoramaFaceCount == pendingPanoramaFaces.length) {
                completedFaces = pendingPanoramaFaces;
                pendingPanoramaFaces = null;
                pendingPanoramaFaceCount = 0;
            }
        }

        if (completedFaces != null) {
            NativeImage[] faces = completedFaces;
            Minecraft.getInstance().execute(() -> setPanoramaPreview(faces));
        }
    }

    public static void finishPanoramaCapture() {
        synchronized (PANORAMA_LOCK) {
            closePendingPanoramaFaces_Panoramica();
        }
    }

    public static void extractRenderState(@NotNull GuiGraphicsExtractor graphics, boolean shouldRenderLevel) {
        Minecraft minecraft = Minecraft.getInstance();
        Preview preview = currentPreview;
        if (preview == null) {
            return;
        }

        long now = Util.getMillis();
        long age = now - preview.startedAtMillis();
        Options.ScreenshotPreviewMode mode = Panoramica.getOptions().getScreenshotPreviewMode();
        if (age >= DISPLAY_MILLIS || !preview.isAllowed(mode)) {
            closeCurrentPreview_Panoramica();
            return;
        }
        if (!shouldRenderLevel || minecraft.gui.hud.isHidden()) {
            return;
        }

        float slide = slideAmount(age);
        int outerWidth = preview.width() + (BORDER_SIZE * 2);
        int outerHeight = preview.height() + (BORDER_SIZE * 2);
        int x = Math.round(MARGIN - outerWidth + (outerWidth + MARGIN) * slide) + BORDER_SIZE;
        int y = MARGIN + BORDER_SIZE;

        graphics.nextStratum();
        graphics.fill(x - BORDER_SIZE, y - BORDER_SIZE, x + preview.width() + BORDER_SIZE, y + preview.height() + BORDER_SIZE, 0xFFFFFFFF);
        if (!preview.render(graphics, x, y, now)) {
            closeCurrentPreview_Panoramica();
        }
    }

    public static void close() {
        synchronized (PANORAMA_LOCK) {
            closePendingPanoramaFaces_Panoramica();
        }
        closeCurrentPreview_Panoramica();
        PANORAMA_RENDERER.close();
    }

    private static void setFlatPreview(@NotNull NativeImage previewImage) {
        if (!shouldShowNormalScreenshots()) {
            previewImage.close();
            return;
        }

        DynamicTexture texture = null;
        try {
            texture = new LinearDynamicTexture(previewImage);
            int renderHeight = Math.max(1, Math.round(PREVIEW_WIDTH * (previewImage.getHeight() / (float) previewImage.getWidth())));
            closeCurrentPreview_Panoramica();
            Minecraft.getInstance().getTextureManager().register(FLAT_PREVIEW_TEXTURE_ID, texture);
            currentPreview = new FlatPreview(FLAT_PREVIEW_TEXTURE_ID, PREVIEW_WIDTH, renderHeight, Util.getMillis());
        } catch (Exception ex) {
            if (texture != null) {
                texture.close();
            } else {
                previewImage.close();
            }
            Panoramica.getLogger().warn("[PANORAMICA] Could not upload normal screenshot preview.", ex);
        }
    }

    private static void setPanoramaPreview(NativeImage @NotNull [] faces) {
        if (!shouldShowPanoramaScreenshots()) {
            closeImages_Panoramica(faces);
            return;
        }

        try {
            PreviewCubeMapTexture texture = new PreviewCubeMapTexture(faces);
            setCurrentPreview_Panoramica(new PanoramaPreview(texture, Util.getMillis()));
        } catch (Exception ex) {
            closeImages_Panoramica(faces);
            Panoramica.getLogger().warn("[PANORAMICA] Could not upload panorama screenshot preview.", ex);
        }
    }

    private static void setCurrentPreview_Panoramica(@NotNull Preview preview) {
        closeCurrentPreview_Panoramica();
        currentPreview = preview;
    }

    private static void closeCurrentPreview_Panoramica() {
        Preview preview = currentPreview;
        currentPreview = null;
        if (preview != null) {
            preview.close();
        }
    }

    private static void closePendingPanoramaFaces_Panoramica() {
        NativeImage[] faces = pendingPanoramaFaces;
        pendingPanoramaFaces = null;
        pendingPanoramaFaceCount = 0;
        if (faces != null) {
            closeImages_Panoramica(faces);
        }
    }

    private static void closeImages_Panoramica(NativeImage @NotNull [] images) {
        for (NativeImage image : images) {
            if (image != null) {
                image.close();
            }
        }
    }

    @NotNull
    private static NativeImage resize(@NotNull NativeImage sourceImage, int targetWidth, int targetHeight) {
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

    private sealed interface Preview extends AutoCloseable permits FlatPreview, PanoramaPreview {

        int width();

        int height();

        long startedAtMillis();

        boolean isAllowed(@NotNull Options.ScreenshotPreviewMode mode);

        boolean render(@NotNull GuiGraphicsExtractor graphics, int x, int y, long nowMillis);

        @Override
        void close();
    }

    private record FlatPreview(@NotNull Identifier textureId, int width, int height, long startedAtMillis) implements Preview {

        @Override
        public boolean isAllowed(@NotNull Options.ScreenshotPreviewMode mode) {
            return mode.showNormalScreenshots;
        }

        @Override
        public boolean render(@NotNull GuiGraphicsExtractor graphics, int x, int y, long nowMillis) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    this.textureId,
                    x,
                    y,
                    0.0F,
                    0.0F,
                    this.width,
                    this.height,
                    this.width,
                    this.height
            );
            return true;
        }

        @Override
        public void close() {
            Minecraft.getInstance().getTextureManager().release(this.textureId);
        }
    }

    private static final class LinearDynamicTexture extends DynamicTexture {

        private LinearDynamicTexture(@NotNull NativeImage image) {
            super(() -> "Panoramica normal screenshot preview", image);
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
    }

    private record PanoramaPreview(@NotNull PreviewCubeMapTexture texture, long startedAtMillis) implements Preview {

        @Override
        public int width() {
            return PREVIEW_WIDTH;
        }

        @Override
        public int height() {
            return PANORAMA_RENDER_HEIGHT;
        }

        @Override
        public boolean isAllowed(@NotNull Options.ScreenshotPreviewMode mode) {
            return mode.showPanoramaScreenshots;
        }

        @Override
        public boolean render(@NotNull GuiGraphicsExtractor graphics, int x, int y, long nowMillis) {
            try {
                updatePanoramaSpin();
                PANORAMA_RENDERER.render(this.texture, -panoramaSpin);
                graphics.blit(
                        PANORAMA_RENDERER.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR),
                        x,
                        y,
                        x + PREVIEW_WIDTH,
                        y + PANORAMA_RENDER_HEIGHT,
                        0.0F,
                        1.0F,
                        1.0F,
                        0.0F
                );
                return true;
            } catch (Exception ex) {
                Panoramica.getLogger().warn("[PANORAMICA] Could not render panorama screenshot preview.", ex);
                return false;
            }
        }

        @Override
        public void close() {
            this.texture.close();
        }
    }

    private static void updatePanoramaSpin() {
        Minecraft minecraft = Minecraft.getInstance();
        float deltaTicks = minecraft.getDeltaTracker().getRealtimeDeltaTicks();
        float speed = (float) minecraft.gameRenderer.gameRenderState().optionsRenderState.panoramaSpeed;
        panoramaSpin = Mth.wrapDegrees(panoramaSpin + (deltaTicks * speed * 0.1F));
    }

}
