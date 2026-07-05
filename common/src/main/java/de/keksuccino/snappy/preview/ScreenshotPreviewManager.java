package de.keksuccino.snappy.preview;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import de.keksuccino.snappy.Options;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.screen.ScreenshotBrowserScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.ClickEvent.OpenFile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class ScreenshotPreviewManager {

    private static final String VANILLA_SCREENSHOT_SUCCESS_KEY = "screenshot.success";
    private static final String SNAPPY_SCREENSHOT_SUCCESS_KEY = "snappy.capture.success";
    private static final int PREVIEW_WIDTH = 150;
    private static final int NORMAL_TEXTURE_WIDTH = 400;
    private static final int PANORAMA_TEXTURE_WIDTH = 400;
    private static final int PANORAMA_TEXTURE_HEIGHT = Math.max(1, Math.round(PANORAMA_TEXTURE_WIDTH * 9.0F / 16.0F));
    private static final int PANORAMA_RENDER_HEIGHT = Math.max(1, Math.round(PREVIEW_WIDTH * 9.0F / 16.0F));
    private static final int PANORAMA_FACE_SIZE = 256;
    private static final int BORDER_SIZE = 1;
    private static final int MARGIN = 8;
    private static final long DISPLAY_MILLIS = 10_000L;
    private static final long SLIDE_MILLIS = 350L;
    private static final long HOVER_ANIMATION_MILLIS = 120L;
    private static final long PREVIEW_TEXTURE_RETIRE_MILLIS = 1_000L;
    private static final float HOVER_GROWTH = 0.05F;
    private static final Object PANORAMA_LOCK = new Object();
    private static final String FLAT_PREVIEW_TEXTURE_PATH = "dynamic/screenshot_preview/";

    @Nullable
    private static Preview currentPreview;
    @Nullable
    private static File currentScreenshotTarget;
    @Nullable
    private static NativeImage[] pendingPanoramaFaces;
    private static final List<RetiredPreview> retiredPreviews = new ArrayList<>();
    private static int pendingPanoramaFaceCount;
    private static int flatPreviewTextureSequence;
    private static float panoramaSpin;
    private static float hoverProgress;
    private static long lastHoverUpdateMillis;
    private static boolean playedSlideOutSound;
    private static final PreviewCubeMapRenderer PANORAMA_RENDERER = new PreviewCubeMapRenderer(PANORAMA_TEXTURE_WIDTH, PANORAMA_TEXTURE_HEIGHT);

    private ScreenshotPreviewManager() {
    }

    public static boolean shouldShowNormalScreenshots() {
        return Snappy.getOptions().getScreenshotPreviewMode().showNormalScreenshots;
    }

    public static boolean shouldShowPanoramaScreenshots() {
        return Snappy.getOptions().getScreenshotPreviewMode().showPanoramaScreenshots;
    }

    public static void clientTick() {
        closeExpiredRetiredPreviews_Snappy();
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
            Snappy.getLogger().warn("[SNAPPY] Could not prepare normal screenshot preview.", ex);
            return;
        }

        Minecraft.getInstance().execute(() -> setFlatPreview(previewImage));
    }

    public static void beginPanoramaCapture() {
        synchronized (PANORAMA_LOCK) {
            closePendingPanoramaFaces_Snappy();
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
            Snappy.getLogger().warn("[SNAPPY] Could not prepare panorama preview face {}.", face, ex);
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
            closePendingPanoramaFaces_Snappy();
        }
    }

    public static void extractRenderState(@NotNull GuiGraphicsExtractor graphics, boolean shouldRenderLevel) {
        closeExpiredRetiredPreviews_Snappy();

        Minecraft minecraft = Minecraft.getInstance();
        Preview preview = currentPreview;
        if (preview == null) {
            return;
        }

        long now = Util.getMillis();
        long age = now - preview.startedAtMillis();
        Options.ScreenshotPreviewMode mode = Snappy.getOptions().getScreenshotPreviewMode();
        if (age >= DISPLAY_MILLIS || !preview.isAllowed(mode)) {
            closeCurrentPreview_Snappy();
            return;
        }
        updateSlideOutSound_Snappy(age);
        if (!shouldRenderLevel || minecraft.gui.hud.isHidden()) {
            return;
        }

        PreviewBounds bounds = getPreviewBounds_Snappy(preview, age, 1.0F);
        double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        boolean hovered = minecraft.gui.screen() != null && getPreviewBounds_Snappy(preview, age, getHoverScale_Snappy()).contains(mouseX, mouseY);
        updateHoverProgress_Snappy(now, hovered);
        float hoverScale = getHoverScale_Snappy();

        graphics.nextStratum();
        if (hovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(bounds.x, bounds.y);
        graphics.pose().scale(hoverScale, hoverScale);
        graphics.fill(0, 0, bounds.width, bounds.height, 0xFFFFFFFF);
        try {
            if (!preview.render(graphics, BORDER_SIZE, BORDER_SIZE, now)) {
                closeCurrentPreview_Snappy();
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    public static boolean handlePreviewClick(@NotNull MouseButtonEvent event) {
        if (event.button() != 0) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() == null || minecraft.level == null || minecraft.gui.hud.isHidden()) {
            return false;
        }

        Preview preview = currentPreview;
        if (preview == null) {
            return false;
        }

        long now = Util.getMillis();
        long age = now - preview.startedAtMillis();
        Options.ScreenshotPreviewMode mode = Snappy.getOptions().getScreenshotPreviewMode();
        if (age >= DISPLAY_MILLIS || !preview.isAllowed(mode)) {
            closeCurrentPreview_Snappy();
            return false;
        }

        if (!getPreviewBounds_Snappy(preview, age, getHoverScale_Snappy()).contains(event.x(), event.y())) {
            return false;
        }

        File target = currentScreenshotTarget;
        if (target != null) {
            Screen parent = minecraft.gui.screen();
            if (parent != null && ScreenshotBrowserScreen.openDetailViewer(minecraft, parent, target.toPath())) {
                closeCurrentPreview_Snappy();
            }
        }
        return true;
    }

    public static void acceptDebugChatMessage(@NotNull Component message) {
        Preview preview = currentPreview;
        if (preview == null || !isMatchingScreenshotSuccessMessage_Snappy(message, preview)) {
            return;
        }

        findOpenFileTarget_Snappy(message).ifPresent(target -> {
            if (currentPreview != null) {
                currentScreenshotTarget = target;
            }
        });
    }

    public static void close() {
        synchronized (PANORAMA_LOCK) {
            closePendingPanoramaFaces_Snappy();
        }
        closeCurrentPreviewNow_Snappy();
        closeRetiredPreviewsNow_Snappy();
        PANORAMA_RENDERER.close();
    }

    private static void setFlatPreview(@NotNull NativeImage previewImage) {
        if (!shouldShowNormalScreenshots()) {
            previewImage.close();
            return;
        }

        DynamicTexture texture = null;
        Identifier textureId = nextFlatPreviewTextureId_Snappy();
        boolean registered = false;
        try {
            texture = new LinearDynamicTexture(previewImage);
            int renderHeight = Math.max(1, Math.round(PREVIEW_WIDTH * (previewImage.getHeight() / (float) previewImage.getWidth())));
            closeCurrentPreview_Snappy();
            Minecraft.getInstance().getTextureManager().register(textureId, texture);
            registered = true;
            currentPreview = new FlatPreview(textureId, PREVIEW_WIDTH, renderHeight, Util.getMillis());
            afterCurrentPreviewSet_Snappy();
        } catch (Exception ex) {
            if (registered) {
                Minecraft.getInstance().getTextureManager().release(textureId);
            } else if (texture != null) {
                texture.close();
            } else {
                previewImage.close();
            }
            Snappy.getLogger().warn("[SNAPPY] Could not upload normal screenshot preview.", ex);
        }
    }

    private static void setPanoramaPreview(NativeImage @NotNull [] faces) {
        if (!shouldShowPanoramaScreenshots()) {
            closeImages_Snappy(faces);
            return;
        }

        try {
            PreviewCubeMapTexture texture = new PreviewCubeMapTexture(faces);
            setCurrentPreview_Snappy(new PanoramaPreview(texture, Util.getMillis()));
        } catch (Exception ex) {
            closeImages_Snappy(faces);
            Snappy.getLogger().warn("[SNAPPY] Could not upload panorama screenshot preview.", ex);
        }
    }

    private static void setCurrentPreview_Snappy(@NotNull Preview preview) {
        closeCurrentPreview_Snappy();
        currentPreview = preview;
        afterCurrentPreviewSet_Snappy();
    }

    private static void closeCurrentPreview_Snappy() {
        Preview preview = currentPreview;
        clearCurrentPreviewState_Snappy();
        if (preview != null) {
            retirePreview_Snappy(preview);
        }
    }

    private static void closeCurrentPreviewNow_Snappy() {
        Preview preview = currentPreview;
        clearCurrentPreviewState_Snappy();
        if (preview != null) {
            preview.close();
        }
    }

    private static void clearCurrentPreviewState_Snappy() {
        currentPreview = null;
        currentScreenshotTarget = null;
        hoverProgress = 0.0F;
        lastHoverUpdateMillis = 0L;
        playedSlideOutSound = false;
    }

    private static void afterCurrentPreviewSet_Snappy() {
        currentScreenshotTarget = null;
        hoverProgress = 0.0F;
        lastHoverUpdateMillis = 0L;
        playedSlideOutSound = false;
        playToastSound_Snappy(SoundEvents.UI_TOAST_IN);
    }

    private static void retirePreview_Snappy(@NotNull Preview preview) {
        retiredPreviews.add(new RetiredPreview(preview, Util.getMillis() + PREVIEW_TEXTURE_RETIRE_MILLIS));
    }

    private static void closeExpiredRetiredPreviews_Snappy() {
        if (retiredPreviews.isEmpty()) {
            return;
        }

        long now = Util.getMillis();
        Iterator<RetiredPreview> iterator = retiredPreviews.iterator();
        while (iterator.hasNext()) {
            RetiredPreview retiredPreview = iterator.next();
            if (now >= retiredPreview.closeAtMillis()) {
                retiredPreview.preview().close();
                iterator.remove();
            }
        }
    }

    private static void closeRetiredPreviewsNow_Snappy() {
        if (retiredPreviews.isEmpty()) {
            return;
        }

        for (RetiredPreview retiredPreview : retiredPreviews) {
            retiredPreview.preview().close();
        }
        retiredPreviews.clear();
    }

    @NotNull
    private static Identifier nextFlatPreviewTextureId_Snappy() {
        return Identifier.fromNamespaceAndPath(Snappy.MOD_ID, FLAT_PREVIEW_TEXTURE_PATH + flatPreviewTextureSequence++);
    }

    private static void closePendingPanoramaFaces_Snappy() {
        NativeImage[] faces = pendingPanoramaFaces;
        pendingPanoramaFaces = null;
        pendingPanoramaFaceCount = 0;
        if (faces != null) {
            closeImages_Snappy(faces);
        }
    }

    private static void closeImages_Snappy(NativeImage @NotNull [] images) {
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

    @NotNull
    private static PreviewBounds getPreviewBounds_Snappy(@NotNull Preview preview, long ageMillis, float scale) {
        float slide = slideAmount(ageMillis);
        int outerWidth = preview.width() + (BORDER_SIZE * 2);
        int outerHeight = preview.height() + (BORDER_SIZE * 2);
        int x = Math.round(MARGIN - outerWidth + outerWidth * slide);
        int y = MARGIN;
        return new PreviewBounds(x, y, Math.round(outerWidth * scale), Math.round(outerHeight * scale));
    }

    private static void updateHoverProgress_Snappy(long nowMillis, boolean hovered) {
        if (lastHoverUpdateMillis == 0L) {
            lastHoverUpdateMillis = nowMillis;
        }

        float amount = Mth.clamp((nowMillis - lastHoverUpdateMillis) / (float) HOVER_ANIMATION_MILLIS, 0.0F, 1.0F);
        hoverProgress = Mth.lerp(amount, hoverProgress, hovered ? 1.0F : 0.0F);
        lastHoverUpdateMillis = nowMillis;
    }

    private static float getHoverScale_Snappy() {
        return 1.0F + (HOVER_GROWTH * easeOutCubic(hoverProgress));
    }

    private static void updateSlideOutSound_Snappy(long ageMillis) {
        if (!playedSlideOutSound && ageMillis >= DISPLAY_MILLIS - SLIDE_MILLIS) {
            playedSlideOutSound = true;
            playToastSound_Snappy(SoundEvents.UI_TOAST_OUT);
        }
    }

    private static void playToastSound_Snappy(@NotNull SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
    }

    private static boolean isMatchingScreenshotSuccessMessage_Snappy(@NotNull Component message, @NotNull Preview preview) {
        if (message.getContents() instanceof TranslatableContents contents) {
            String key = contents.getKey();
            return VANILLA_SCREENSHOT_SUCCESS_KEY.equals(key) && preview instanceof FlatPreview
                    || SNAPPY_SCREENSHOT_SUCCESS_KEY.equals(key) && preview instanceof PanoramaPreview;
        }
        return false;
    }

    @NotNull
    private static Optional<File> findOpenFileTarget_Snappy(@NotNull Component message) {
        return message.visit((style, contents) -> {
            if (style.getClickEvent() instanceof OpenFile openFile) {
                return Optional.of(openFile.file());
            }
            return Optional.empty();
        }, Style.EMPTY);
    }

    private record RetiredPreview(@NotNull Preview preview, long closeAtMillis) {
    }

    private record PreviewBounds(int x, int y, int width, int height) {

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
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
            super(() -> "Snappy normal screenshot preview", image);
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
                Snappy.getLogger().warn("[SNAPPY] Could not render panorama screenshot preview.", ex);
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
