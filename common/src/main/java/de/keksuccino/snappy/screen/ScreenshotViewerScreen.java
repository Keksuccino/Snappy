package de.keksuccino.snappy.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.menu.MenuBackgroundSelectionManager;
import de.keksuccino.snappy.menu.PanoramaMenuManager;
import de.keksuccino.snappy.preview.PreviewCubeMapRenderer;
import de.keksuccino.snappy.preview.PreviewCubeMapTexture;
import de.keksuccino.snappy.screen.ScreenshotBrowserCatalog.DeletionResult;
import de.keksuccino.snappy.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import de.keksuccino.snappy.screen.ScreenshotImageLoader.LoadedImage;
import de.keksuccino.snappy.util.file.ExternalFileOpener;
import de.keksuccino.snappy.util.rendering.RenderingUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ScreenshotViewerScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String TEXTURE_PATH = "dynamic/screenshot_browser/viewer/";
    private static final int BUTTON_GAP = 6;
    private static final int NAVIGATION_BUTTON_GAP = 20;
    private static final int PANORAMA_RENDER_WIDTH = 960;
    private static final int PANORAMA_RENDER_HEIGHT = 540;
    private static final int PANORAMA_PROGRESS_MAX_WIDTH = 360;
    private static final int PANORAMA_PROGRESS_MIN_WIDTH = 120;
    private static final int PANORAMA_PROGRESS_HORIZONTAL_MARGIN = 80;
    private static final int PANORAMA_PROGRESS_BOTTOM_MARGIN = 28;
    private static final int PANORAMA_PROGRESS_TRACK_THICKNESS = 4;
    private static final int PANORAMA_PROGRESS_DOT_SIZE = 8;
    private static final int PANORAMA_PROGRESS_HIT_PADDING = 8;
    private static final int IMAGE_AREA_BORDER_SIZE = 1;
    private static final int IMAGE_AREA_BORDER_COLOR = ARGB.color(255, 112, 112, 112);
    private static final int PANORAMA_VERTICAL_PROGRESS_MAX_HEIGHT = 300;
    private static final int PANORAMA_VERTICAL_PROGRESS_MIN_HEIGHT = 100;
    private static final int PANORAMA_VERTICAL_PROGRESS_VERTICAL_MARGIN = 120;
    private static final int PANORAMA_VERTICAL_PROGRESS_RIGHT_MARGIN = 28;
    private static final float PANORAMA_ROTATION_DEGREES_PER_MILLI = 0.01F;
    private static final float PANORAMA_ROTATION_FULL_TURN_DEGREES = 360.0F;
    private static final float PANORAMA_MOUSE_LOOK_SENSITIVITY = 0.16F;
    private static final float PANORAMA_VERTICAL_ANGLE_MIN_DEGREES = -90.0F;
    private static final float PANORAMA_VERTICAL_ANGLE_MAX_DEGREES = 90.0F;
    private static final int NO_HOVER_MOUSE_POSITION = -1;
    private static final Identifier BACK_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/shared/back_icon_15x15.png");
    private static final Identifier METADATA_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/viewer/open_metadata_icon_15x15.png");
    private static final Identifier SHOW_OUTSIDE_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/viewer/show_outside_minecraft_icon_15x15.png");
    private static final Identifier DELETE_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/shared/delete_icon_15x15.png");
    private static final Identifier PREVIOUS_IMAGE_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/viewer/previous_screenshot_icon_15x15.png");
    private static final Identifier NEXT_IMAGE_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/viewer/next_screenshot_icon_15x15.png");
    private static final Identifier MENU_BACKGROUND_DISABLED_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/viewer/menu_background/unselected_icon_15x15.png");
    private static final Identifier MENU_BACKGROUND_ENABLED_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/viewer/menu_background/selected_icon_15x15.png");
    private static int textureSequence;

    private final Screen parent;
    private final List<ScreenshotEntry> entries;
    private final PreviewCubeMapRenderer panoramaRenderer = new PreviewCubeMapRenderer(PANORAMA_RENDER_WIDTH, PANORAMA_RENDER_HEIGHT);
    private int index;
    private int loadGeneration;
    private LoadStatus loadStatus = LoadStatus.LOADING;
    @Nullable
    private Identifier normalTextureId;
    @Nullable
    private PreviewCubeMapTexture panoramaTexture;
    private int imageWidth = 16;
    private int imageHeight = 9;
    private int normalBackgroundColor = ScreenshotImageLoader.VIEWER_BACKGROUND_FALLBACK_COLOR;
    private float panoramaRotationDegrees;
    private float panoramaVerticalAngleDegrees;
    private long panoramaRotationLastMillis;
    private boolean panoramaProgressDragging;
    private boolean panoramaVerticalProgressDragging;
    private boolean panoramaMouseLookActive;
    private boolean panoramaMouseLookCursorGrabbed;
    private Component statusMessage = Component.translatable("snappy.browser.loading");
    @Nullable
    private Button previousButton;
    @Nullable
    private Button nextButton;
    @Nullable
    private Button metadataButton;
    @Nullable
    private Button outsideButton;
    @Nullable
    private Button deleteButton;
    @Nullable
    private TexturedIconButton menuBackgroundSelectionButton;

    public ScreenshotViewerScreen(@NotNull Screen parent, @NotNull List<ScreenshotEntry> entries, int index) {
        super(Component.translatable("snappy.viewer.title"));
        this.parent = parent;
        this.entries = new ArrayList<>(entries);
        this.index = Mth.clamp(index, 0, Math.max(0, entries.size() - 1));
    }

    @Override
    public void added() {
        this.loadCurrentEntry();
    }

    @Override
    protected void init() {
        int bottomY = this.footerButtonY();
        int centerX = this.width / 2;

        int iconButtonWidth = TexturedIconButton.DEFAULT_BUTTON_SIZE;
        int totalWidth = iconButtonWidth * 6 + BUTTON_GAP * 3 + NAVIGATION_BUTTON_GAP * 2;
        int x = centerX - totalWidth / 2;

        this.previousButton = this.addRenderableWidget(new TexturedIconButton(Component.translatable("snappy.viewer.previous"), button -> this.previous(), PREVIOUS_IMAGE_ICON));
        this.previousButton.setPosition(x, bottomY);
        x += iconButtonWidth + NAVIGATION_BUTTON_GAP;
        Button backButton = this.addRenderableWidget(new TexturedIconButton(CommonComponents.GUI_BACK, button -> this.onClose(), BACK_ICON));
        backButton.setPosition(x, bottomY);
        x += iconButtonWidth + BUTTON_GAP;
        this.metadataButton = this.addRenderableWidget(new TexturedIconButton(Component.translatable("snappy.viewer.metadata"), button -> this.openMetadata(), METADATA_ICON));
        this.metadataButton.setPosition(x, bottomY);
        x += iconButtonWidth + BUTTON_GAP;
        this.outsideButton = this.addRenderableWidget(new TexturedIconButton(Component.translatable("snappy.viewer.show_outside"), button -> this.showOutsideMinecraft(), SHOW_OUTSIDE_ICON));
        this.outsideButton.setPosition(x, bottomY);
        x += iconButtonWidth + BUTTON_GAP;
        this.deleteButton = this.addRenderableWidget(new TexturedIconButton(this.deleteMessage(), button -> this.confirmDeleteCurrent(), DELETE_ICON));
        this.deleteButton.setPosition(x, bottomY);
        x += iconButtonWidth + NAVIGATION_BUTTON_GAP;
        this.nextButton = this.addRenderableWidget(new TexturedIconButton(Component.translatable("snappy.viewer.next"), button -> this.next(), NEXT_IMAGE_ICON));
        this.nextButton.setPosition(x, bottomY);

        this.menuBackgroundSelectionButton = this.addRenderableWidget(new TexturedIconButton(
                Component.translatable("snappy.viewer.menu_background.tooltip"),
                button -> this.toggleMenuBackgroundSelection(),
                MENU_BACKGROUND_DISABLED_ICON
        ));
        this.menuBackgroundSelectionButton.setPosition(0, bottomY);
        this.menuBackgroundSelectionButton.visible = false;
        this.updateButtons();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int hoverMouseX = this.hoverMouseX(mouseX);
        int hoverMouseY = this.hoverMouseY(mouseY);
        this.renderHeader(graphics);
        this.renderImage(graphics, hoverMouseX, hoverMouseY);
        super.extractRenderState(graphics, hoverMouseX, hoverMouseY, a);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (this.isPanoramaProgressGrabberHit(event.x(), event.y())) {
                this.panoramaProgressDragging = true;
                this.setDragging(true);
                this.setPanoramaRotationFromMouse(event.x());
                return true;
            }
            if (this.isPanoramaVerticalProgressGrabberHit(event.x(), event.y())) {
                this.panoramaVerticalProgressDragging = true;
                this.setDragging(true);
                this.setPanoramaVerticalAngleFromMouse(event.y());
                return true;
            }
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0 && this.isPanoramaMouseLookHit(event.x(), event.y())) {
            this.startPanoramaMouseLook();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dx, double dy) {
        if (this.panoramaMouseLookActive && event.button() == 0) {
            this.rotatePanoramaFromMouseDrag(dx, dy);
            return true;
        }
        if (this.panoramaProgressDragging && event.button() == 0) {
            this.setPanoramaRotationFromMouse(event.x());
            return true;
        }
        if (this.panoramaVerticalProgressDragging && event.button() == 0) {
            this.setPanoramaVerticalAngleFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (this.panoramaMouseLookActive && event.button() == 0) {
            this.stopPanoramaMouseLook();
            return true;
        }
        if (this.panoramaProgressDragging && event.button() == 0) {
            this.setPanoramaRotationFromMouse(event.x());
            this.panoramaProgressDragging = false;
            this.panoramaRotationLastMillis = Util.getMillis();
            this.setDragging(false);
            return true;
        }
        if (this.panoramaVerticalProgressDragging && event.button() == 0) {
            this.setPanoramaVerticalAngleFromMouse(event.y());
            this.panoramaVerticalProgressDragging = false;
            this.setDragging(false);
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (event.isLeft()) {
            this.previous();
            return true;
        }
        if (event.isRight()) {
            this.next();
            return true;
        }
        if (event.key() == 259 || event.key() == 261) {
            this.confirmDeleteCurrent();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        this.loadGeneration++;
        this.stopPanoramaMouseLook();
        this.releaseCurrentImage();
        this.panoramaRenderer.close();
    }

    @Override
    public void onClose() {
        if (this.parent instanceof ScreenshotBrowserScreen browserScreen) {
            browserScreen.refreshEntries();
        }
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    @Nullable
    private ScreenshotEntry currentEntry() {
        return this.index >= 0 && this.index < this.entries.size() ? this.entries.get(this.index) : null;
    }

    private void previous() {
        if (this.index > 0) {
            this.index--;
            this.loadCurrentEntry();
            this.updateButtons();
        }
    }

    private void next() {
        if (this.index + 1 < this.entries.size()) {
            this.index++;
            this.loadCurrentEntry();
            this.updateButtons();
        }
    }

    private void showOutsideMinecraft() {
        ScreenshotEntry entry = this.currentEntry();
        if (entry != null) {
            if (entry.isPanorama()) {
                ExternalFileOpener.openPath(entry.outsidePath());
            } else {
                ExternalFileOpener.revealFile(entry.outsidePath());
            }
        }
    }

    private void openMetadata() {
        ScreenshotEntry entry = this.currentEntry();
        if (entry != null) {
            this.minecraft.gui.setScreen(new ScreenshotMetadataScreen(this, entry));
        }
    }

    private void toggleMenuBackgroundSelection() {
        ScreenshotEntry entry = this.currentEntry();
        if (entry != null && entry.isPanorama()) {
            MenuBackgroundSelectionManager.toggle(entry.path());
            this.updateButtons();
        }
    }

    private void confirmDeleteCurrent() {
        ScreenshotEntry entry = this.currentEntry();
        if (entry == null) {
            return;
        }

        this.minecraft.gui.setScreen(new ConfirmScreen(result -> {
            if (result) {
                this.deleteCurrent(entry);
            } else {
                this.minecraft.gui.setScreen(this);
            }
        }, Component.translatable("snappy.viewer.delete_confirm.title"), Component.translatable("snappy.viewer.delete_confirm.message", entry.displayName()),
                Component.translatable("snappy.browser.delete"), CommonComponents.GUI_CANCEL));
    }

    private void deleteCurrent(@NotNull ScreenshotEntry entry) {
        DeletionResult result = ScreenshotBrowserCatalog.deleteAll(List.of(entry));
        PanoramaMenuManager.invalidate();
        if (this.parent instanceof ScreenshotBrowserScreen browserScreen) {
            browserScreen.refreshEntries();
        }

        if (result.failed() > 0) {
            this.statusMessage = Component.translatable("snappy.viewer.delete_failed").withStyle(ChatFormatting.YELLOW);
            this.minecraft.gui.setScreen(this);
            return;
        }

        this.entries.remove(entry);
        if (this.entries.isEmpty()) {
            this.minecraft.gui.setScreen(this.parent);
            return;
        }

        this.index = Mth.clamp(this.index, 0, this.entries.size() - 1);
        this.minecraft.gui.setScreen(this);
    }

    private void loadCurrentEntry() {
        ScreenshotEntry entry = this.currentEntry();
        this.releaseCurrentImage();
        this.resetPanoramaPlayback();
        this.normalBackgroundColor = ScreenshotImageLoader.VIEWER_BACKGROUND_FALLBACK_COLOR;
        this.loadStatus = LoadStatus.LOADING;
        this.statusMessage = Component.translatable("snappy.browser.loading");
        this.updateButtons();

        if (entry == null) {
            this.loadStatus = LoadStatus.FAILED;
            this.statusMessage = Component.translatable("snappy.viewer.missing");
            return;
        }

        int generation = ++this.loadGeneration;
        if (entry.isPanorama()) {
            Util.ioPool().execute(() -> {
                byte[][] faceBytes = null;
                Throwable failure = null;
                try {
                    faceBytes = ScreenshotImageLoader.readPanoramaViewerFaceBytes(entry);
                } catch (Throwable ex) {
                    failure = ex;
                }
                byte[][] result = faceBytes;
                Throwable error = failure;
                this.minecraft.execute(() -> this.completePanoramaLoad(generation, entry, result, error));
            });
        } else {
            Util.ioPool().execute(() -> {
                byte[] imageBytes = null;
                Throwable failure = null;
                try {
                    imageBytes = ScreenshotImageLoader.readNormalViewerBytes(entry);
                } catch (Throwable ex) {
                    failure = ex;
                }
                byte[] result = imageBytes;
                Throwable error = failure;
                this.minecraft.execute(() -> this.completeNormalLoad(generation, entry, result, error));
            });
        }
    }

    private void completeNormalLoad(
            int generation,
            @NotNull ScreenshotEntry entry,
            byte @Nullable [] imageBytes,
            @Nullable Throwable error
    ) {
        if (!this.isCurrentLoad(generation, entry)) {
            return;
        }
        if (error != null || imageBytes == null) {
            this.failLoad(entry, error);
            return;
        }

        LoadedImage loadedImage = null;
        Identifier textureId = nextTextureId();
        ScreenshotImageTexture texture = null;
        try {
            loadedImage = ScreenshotImageLoader.decodeNormalViewerImage(imageBytes);
            int backgroundColor = ScreenshotImageLoader.createViewerBackgroundColor(loadedImage.image());
            texture = new ScreenshotImageTexture("Snappy screenshot viewer", loadedImage.image());
            int width = texture.getPixels().getWidth();
            int height = texture.getPixels().getHeight();
            this.minecraft.getTextureManager().register(textureId, texture);
            texture = null;
            this.normalTextureId = textureId;
            this.imageWidth = width;
            this.imageHeight = height;
            this.normalBackgroundColor = backgroundColor;
            this.loadStatus = LoadStatus.READY;
            this.statusMessage = Component.empty();
        } catch (Throwable ex) {
            if (texture != null) {
                texture.close();
            } else if (loadedImage != null) {
                loadedImage.image().close();
            }
            this.failLoad(entry, ex);
        }
    }

    private void completePanoramaLoad(
            int generation,
            @NotNull ScreenshotEntry entry,
            byte @Nullable [][] faceBytes,
            @Nullable Throwable error
    ) {
        if (!this.isCurrentLoad(generation, entry)) {
            return;
        }
        if (error != null || faceBytes == null) {
            this.failLoad(entry, error);
            return;
        }

        try {
            this.panoramaTexture = new PreviewCubeMapTexture(ScreenshotImageLoader.decodePanoramaViewerFaces(faceBytes));
            this.imageWidth = 16;
            this.imageHeight = 9;
            this.loadStatus = LoadStatus.READY;
            this.statusMessage = Component.empty();
            this.resetPanoramaPlayback();
        } catch (Throwable ex) {
            this.failLoad(entry, ex);
        }
    }

    private boolean isCurrentLoad(int generation, @NotNull ScreenshotEntry entry) {
        return generation == this.loadGeneration && this.minecraft.gui.screen() == this && entry.equals(this.currentEntry());
    }

    private void failLoad(@NotNull ScreenshotEntry entry, @Nullable Throwable error) {
        this.releaseCurrentImage();
        this.loadStatus = LoadStatus.FAILED;
        this.statusMessage = Component.translatable("snappy.viewer.load_failed");
        if (error != null) {
            LOGGER.warn("[SNAPPY] Could not load screenshot {}.", entry.path(), error);
        }
    }

    private void releaseCurrentImage() {
        Identifier textureId = this.normalTextureId;
        this.normalTextureId = null;
        if (textureId != null) {
            this.minecraft.getTextureManager().release(textureId);
        }
        this.normalBackgroundColor = ScreenshotImageLoader.VIEWER_BACKGROUND_FALLBACK_COLOR;

        PreviewCubeMapTexture texture = this.panoramaTexture;
        this.panoramaTexture = null;
        if (texture != null) {
            texture.close();
        }
    }

    private void updateButtons() {
        ScreenshotEntry entry = this.currentEntry();
        boolean hasEntry = entry != null;
        if (this.previousButton != null) {
            this.previousButton.active = this.index > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = this.index + 1 < this.entries.size();
        }
        if (this.metadataButton != null) {
            this.metadataButton.active = hasEntry;
        }
        if (this.outsideButton != null) {
            this.outsideButton.active = hasEntry;
        }
        if (this.deleteButton != null) {
            this.deleteButton.active = hasEntry;
        }
        this.updateMenuBackgroundSelectionButton(entry);
    }

    @NotNull
    private Component deleteMessage() {
        return Component.translatable("snappy.browser.delete").withStyle(ChatFormatting.RED);
    }

    private void updateMenuBackgroundSelectionButton(@Nullable ScreenshotEntry entry) {
        TexturedIconButton button = this.menuBackgroundSelectionButton;
        if (button == null) {
            return;
        }

        boolean visible = entry != null && entry.isPanorama();
        button.visible = visible;
        button.active = visible;
        if (!visible) {
            return;
        }

        boolean selected = MenuBackgroundSelectionManager.isSelected(entry.path());
        Component tooltip = Component.translatable("snappy.viewer.menu_background.tooltip");
        button.setMessage(tooltip);
        button.setTooltip(Tooltip.create(tooltip));
        button.setIconTexture(selected ? MENU_BACKGROUND_ENABLED_ICON : MENU_BACKGROUND_DISABLED_ICON);
        button.setPosition(this.imageAreaX() + this.imageAreaWidth() - button.getWidth(), this.footerButtonY());
    }

    private void renderHeader(@NotNull GuiGraphicsExtractor graphics) {
        ScreenshotEntry entry = this.currentEntry();
        Component name = entry == null ? Component.translatable("snappy.viewer.missing") : Component.literal(entry.displayName());
        graphics.centeredText(this.font, name, this.width / 2, 14, 0xFFFFFFFF);
        if (entry != null) {
            Component meta = Component.translatable("snappy.viewer.position", this.index + 1, this.entries.size(), Component.translatable(entry.typeLabelKey()));
            graphics.centeredText(this.font, meta, this.width / 2, 27, 0xFFB0B0B0);
        }
    }

    private void renderImage(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int areaX = this.imageAreaX();
        int areaY = this.imageAreaY();
        int areaWidth = this.imageAreaWidth();
        int areaHeight = this.imageAreaHeight();
        RenderingUtils.renderBorder(
                graphics,
                areaX - IMAGE_AREA_BORDER_SIZE,
                areaY - IMAGE_AREA_BORDER_SIZE,
                areaWidth + IMAGE_AREA_BORDER_SIZE * 2,
                areaHeight + IMAGE_AREA_BORDER_SIZE * 2,
                IMAGE_AREA_BORDER_SIZE,
                IMAGE_AREA_BORDER_COLOR
        );
        graphics.fill(areaX, areaY, areaX + areaWidth, areaY + areaHeight, this.imageBackgroundColor());

        if (this.loadStatus != LoadStatus.READY) {
            graphics.centeredText(this.font, this.statusMessage, areaX + areaWidth / 2, areaY + areaHeight / 2 - 4, 0xFFFFFFFF);
            return;
        }

        ScreenshotEntry entry = this.currentEntry();
        if (entry != null && entry.isPanorama()) {
            this.renderPanorama(graphics, areaX, areaY, areaWidth, areaHeight);
            this.renderPanoramaProgressBar(graphics, mouseX, mouseY);
            this.renderPanoramaVerticalProgressBar(graphics, mouseX, mouseY);
        } else {
            int[] bounds = this.fitBounds(areaX, areaY, areaWidth, areaHeight, this.imageWidth, this.imageHeight);
            this.renderNormalImage(graphics, bounds[0], bounds[1], bounds[2], bounds[3]);
        }
    }

    private int imageBackgroundColor() {
        ScreenshotEntry entry = this.currentEntry();
        return this.loadStatus == LoadStatus.READY && entry != null && !entry.isPanorama()
                ? this.normalBackgroundColor
                : ScreenshotImageLoader.VIEWER_BACKGROUND_FALLBACK_COLOR;
    }

    private void renderNormalImage(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        Identifier textureId = this.normalTextureId;
        if (textureId != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, x, y, 0.0F, 0.0F, width, height, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        }
    }

    private void renderPanorama(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        PreviewCubeMapTexture texture = this.panoramaTexture;
        if (texture == null) {
            return;
        }

        float spin = this.currentPanoramaRotationDegrees();
        this.panoramaRenderer.render(texture, width, height, PreviewCubeMapRenderer.DEFAULT_ROT_X_IN_DEGREES + this.panoramaVerticalAngleDegrees, -spin);
        graphics.blit(
                this.panoramaRenderer.getColorTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR),
                x,
                y,
                x + width,
                y + height,
                0.0F,
                1.0F,
                1.0F,
                0.0F
        );
    }

    private void renderPanoramaProgressBar(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.isPanoramaProgressVisible()) {
            return;
        }

        int trackX = this.panoramaProgressX();
        int trackY = this.panoramaProgressY();
        int trackWidth = this.panoramaProgressWidth();
        int dotSize = PANORAMA_PROGRESS_DOT_SIZE + (this.panoramaProgressDragging ? 2 : 0);
        int trackCenterY = this.panoramaProgressCenterY();
        int dotCenterX = this.panoramaProgressDotCenterX();

        graphics.fill(
                trackX - PANORAMA_PROGRESS_HIT_PADDING,
                trackY - PANORAMA_PROGRESS_HIT_PADDING,
                trackX + trackWidth + PANORAMA_PROGRESS_HIT_PADDING,
                trackY + PANORAMA_PROGRESS_TRACK_THICKNESS + PANORAMA_PROGRESS_HIT_PADDING,
                0x99000000
        );
        graphics.fill(trackX, trackY, trackX + trackWidth, trackY + PANORAMA_PROGRESS_TRACK_THICKNESS, 0xAA404040);
        graphics.outline(trackX, trackY, trackWidth, PANORAMA_PROGRESS_TRACK_THICKNESS, 0xCCFFFFFF);
        graphics.fill(
                dotCenterX - dotSize / 2,
                trackCenterY - dotSize / 2,
                dotCenterX + (dotSize + 1) / 2,
                trackCenterY + (dotSize + 1) / 2,
                0xFFFFFFFF
        );
        graphics.outline(
                dotCenterX - dotSize / 2,
                trackCenterY - dotSize / 2,
                dotSize,
                dotSize,
                0xFF000000
        );

        if (this.panoramaProgressDragging || this.isPanoramaProgressGrabberHit(mouseX, mouseY)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void renderPanoramaVerticalProgressBar(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.isPanoramaProgressVisible()) {
            return;
        }

        int trackX = this.panoramaVerticalProgressX();
        int trackY = this.panoramaVerticalProgressY();
        int trackHeight = this.panoramaVerticalProgressHeight();
        int dotSize = PANORAMA_PROGRESS_DOT_SIZE + (this.panoramaVerticalProgressDragging ? 2 : 0);
        int trackCenterX = this.panoramaVerticalProgressCenterX();
        int dotCenterY = this.panoramaVerticalProgressDotCenterY();

        graphics.fill(
                trackX - PANORAMA_PROGRESS_HIT_PADDING,
                trackY - PANORAMA_PROGRESS_HIT_PADDING,
                trackX + PANORAMA_PROGRESS_TRACK_THICKNESS + PANORAMA_PROGRESS_HIT_PADDING,
                trackY + trackHeight + PANORAMA_PROGRESS_HIT_PADDING,
                0x99000000
        );
        graphics.fill(trackX, trackY, trackX + PANORAMA_PROGRESS_TRACK_THICKNESS, trackY + trackHeight, 0xAA404040);
        graphics.outline(trackX, trackY, PANORAMA_PROGRESS_TRACK_THICKNESS, trackHeight, 0xCCFFFFFF);
        graphics.fill(
                trackCenterX - dotSize / 2,
                dotCenterY - dotSize / 2,
                trackCenterX + (dotSize + 1) / 2,
                dotCenterY + (dotSize + 1) / 2,
                0xFFFFFFFF
        );
        graphics.outline(
                trackCenterX - dotSize / 2,
                dotCenterY - dotSize / 2,
                dotSize,
                dotSize,
                0xFF000000
        );

        if (this.panoramaVerticalProgressDragging || this.isPanoramaVerticalProgressGrabberHit(mouseX, mouseY)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private float currentPanoramaRotationDegrees() {
        long now = Util.getMillis();
        if (!this.panoramaProgressDragging && !this.panoramaMouseLookActive) {
            if (this.panoramaRotationLastMillis == 0L) {
                this.panoramaRotationLastMillis = now;
            }
            long elapsedMillis = Math.max(0L, now - this.panoramaRotationLastMillis);
            this.panoramaRotationDegrees = wrapPanoramaRotation(this.panoramaRotationDegrees + elapsedMillis * PANORAMA_ROTATION_DEGREES_PER_MILLI);
            this.panoramaRotationLastMillis = now;
        }
        return this.panoramaRotationDegrees;
    }

    private float panoramaRotationProgress() {
        return wrapPanoramaRotation(this.panoramaRotationDegrees) / PANORAMA_ROTATION_FULL_TURN_DEGREES;
    }

    private void setPanoramaRotationFromMouse(double mouseX) {
        int trackX = this.panoramaProgressX();
        int trackWidth = this.panoramaProgressWidth();
        double progress = Mth.clamp((mouseX - trackX) / (double) trackWidth, 0.0D, 1.0D);
        this.panoramaRotationDegrees = (float) Math.min(progress * PANORAMA_ROTATION_FULL_TURN_DEGREES, PANORAMA_ROTATION_FULL_TURN_DEGREES - 0.001F);
        this.panoramaRotationLastMillis = Util.getMillis();
    }

    private void rotatePanoramaFromMouseDrag(double dx, double dy) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy)) {
            return;
        }

        this.panoramaRotationDegrees = wrapPanoramaRotation(this.panoramaRotationDegrees + (float) dx * PANORAMA_MOUSE_LOOK_SENSITIVITY);
        this.panoramaVerticalAngleDegrees = Mth.clamp(
                this.panoramaVerticalAngleDegrees + (float) dy * PANORAMA_MOUSE_LOOK_SENSITIVITY,
                PANORAMA_VERTICAL_ANGLE_MIN_DEGREES,
                PANORAMA_VERTICAL_ANGLE_MAX_DEGREES
        );
        this.panoramaRotationLastMillis = Util.getMillis();
    }

    private float panoramaVerticalAngleProgress() {
        return (this.panoramaVerticalAngleDegrees - PANORAMA_VERTICAL_ANGLE_MIN_DEGREES)
                / (PANORAMA_VERTICAL_ANGLE_MAX_DEGREES - PANORAMA_VERTICAL_ANGLE_MIN_DEGREES);
    }

    private void setPanoramaVerticalAngleFromMouse(double mouseY) {
        int trackY = this.panoramaVerticalProgressY();
        int trackHeight = this.panoramaVerticalProgressHeight();
        double progress = Mth.clamp((mouseY - trackY) / (double) trackHeight, 0.0D, 1.0D);
        this.panoramaVerticalAngleDegrees = (float) (PANORAMA_VERTICAL_ANGLE_MIN_DEGREES
                + progress * (PANORAMA_VERTICAL_ANGLE_MAX_DEGREES - PANORAMA_VERTICAL_ANGLE_MIN_DEGREES));
    }

    private void resetPanoramaPlayback() {
        boolean wasProgressDragging = this.panoramaProgressDragging || this.panoramaVerticalProgressDragging;
        this.stopPanoramaMouseLook();
        this.panoramaRotationDegrees = 0.0F;
        this.panoramaRotationLastMillis = Util.getMillis();
        this.panoramaProgressDragging = false;
        this.panoramaVerticalProgressDragging = false;
        if (wasProgressDragging) {
            this.setDragging(false);
        }
    }

    private boolean isPanoramaProgressVisible() {
        ScreenshotEntry entry = this.currentEntry();
        return entry != null && entry.isPanorama() && this.loadStatus == LoadStatus.READY && this.panoramaTexture != null;
    }

    private boolean isPanoramaMouseLookHit(double mouseX, double mouseY) {
        return this.isPanoramaProgressVisible()
                && mouseX >= this.imageAreaX()
                && mouseX <= this.imageAreaX() + this.imageAreaWidth()
                && mouseY >= this.imageAreaY()
                && mouseY <= this.imageAreaY() + this.imageAreaHeight();
    }

    private void startPanoramaMouseLook() {
        if (this.minecraft == null || !this.minecraft.isWindowActive()) {
            return;
        }

        this.currentPanoramaRotationDegrees();
        this.panoramaMouseLookActive = true;
        this.panoramaRotationLastMillis = Util.getMillis();
        this.setPanoramaMouseLookCursorGrabbed(true);
    }

    private void stopPanoramaMouseLook() {
        boolean wasActive = this.panoramaMouseLookActive;
        this.panoramaMouseLookActive = false;
        this.setPanoramaMouseLookCursorGrabbed(false);
        if (wasActive) {
            this.panoramaRotationLastMillis = Util.getMillis();
        }
    }

    private void setPanoramaMouseLookCursorGrabbed(boolean grabbed) {
        if (this.panoramaMouseLookCursorGrabbed == grabbed || this.minecraft == null) {
            return;
        }
        if (grabbed && !this.minecraft.isWindowActive()) {
            return;
        }

        this.panoramaMouseLookCursorGrabbed = grabbed;
        double centerX = this.minecraft.getWindow().getScreenWidth() / 2.0D;
        double centerY = this.minecraft.getWindow().getScreenHeight() / 2.0D;
        this.minecraft.mouseHandler.setIgnoreFirstMove();
        InputConstants.grabOrReleaseMouse(
                this.minecraft.getWindow(),
                grabbed ? InputConstants.CURSOR_DISABLED : InputConstants.CURSOR_NORMAL,
                centerX,
                centerY
        );
    }

    private boolean isPanoramaProgressGrabberHit(double mouseX, double mouseY) {
        if (!this.isPanoramaProgressVisible()) {
            return false;
        }
        int dotSize = PANORAMA_PROGRESS_DOT_SIZE + (this.panoramaProgressDragging ? 2 : 0);
        int hitRadius = dotSize / 2 + PANORAMA_PROGRESS_HIT_PADDING;
        int dotCenterX = this.panoramaProgressDotCenterX();
        int dotCenterY = this.panoramaProgressCenterY();
        return mouseX >= dotCenterX - hitRadius
                && mouseX <= dotCenterX + hitRadius
                && mouseY >= dotCenterY - hitRadius
                && mouseY <= dotCenterY + hitRadius;
    }

    private boolean isPanoramaVerticalProgressGrabberHit(double mouseX, double mouseY) {
        if (!this.isPanoramaProgressVisible()) {
            return false;
        }
        int dotSize = PANORAMA_PROGRESS_DOT_SIZE + (this.panoramaVerticalProgressDragging ? 2 : 0);
        int hitRadius = dotSize / 2 + PANORAMA_PROGRESS_HIT_PADDING;
        int dotCenterX = this.panoramaVerticalProgressCenterX();
        int dotCenterY = this.panoramaVerticalProgressDotCenterY();
        return mouseX >= dotCenterX - hitRadius
                && mouseX <= dotCenterX + hitRadius
                && mouseY >= dotCenterY - hitRadius
                && mouseY <= dotCenterY + hitRadius;
    }

    private int panoramaProgressX() {
        return this.width / 2 - this.panoramaProgressWidth() / 2;
    }

    private int panoramaProgressY() {
        return this.imageAreaY() + this.imageAreaHeight() - PANORAMA_PROGRESS_BOTTOM_MARGIN;
    }

    private int panoramaProgressCenterY() {
        return this.panoramaProgressY() + PANORAMA_PROGRESS_TRACK_THICKNESS / 2;
    }

    private int panoramaProgressWidth() {
        return Mth.clamp(this.imageAreaWidth() - PANORAMA_PROGRESS_HORIZONTAL_MARGIN, PANORAMA_PROGRESS_MIN_WIDTH, PANORAMA_PROGRESS_MAX_WIDTH);
    }

    private int panoramaProgressDotCenterX() {
        int trackWidth = this.panoramaProgressWidth();
        int progressWidth = Mth.clamp(Math.round(trackWidth * this.panoramaRotationProgress()), 0, trackWidth);
        return this.panoramaProgressX() + progressWidth;
    }

    private int panoramaVerticalProgressX() {
        return this.imageAreaX() + this.imageAreaWidth() - PANORAMA_VERTICAL_PROGRESS_RIGHT_MARGIN - PANORAMA_PROGRESS_TRACK_THICKNESS;
    }

    private int panoramaVerticalProgressY() {
        return this.imageAreaY() + this.imageAreaHeight() / 2 - this.panoramaVerticalProgressHeight() / 2;
    }

    private int panoramaVerticalProgressCenterX() {
        return this.panoramaVerticalProgressX() + PANORAMA_PROGRESS_TRACK_THICKNESS / 2;
    }

    private int panoramaVerticalProgressHeight() {
        int maxHeight = Math.max(1, Math.min(PANORAMA_VERTICAL_PROGRESS_MAX_HEIGHT, this.imageAreaHeight() - PANORAMA_PROGRESS_HIT_PADDING * 2));
        int minHeight = Math.min(PANORAMA_VERTICAL_PROGRESS_MIN_HEIGHT, maxHeight);
        return Mth.clamp(this.imageAreaHeight() - PANORAMA_VERTICAL_PROGRESS_VERTICAL_MARGIN, minHeight, maxHeight);
    }

    private int panoramaVerticalProgressDotCenterY() {
        int trackHeight = this.panoramaVerticalProgressHeight();
        int progressHeight = Mth.clamp(Math.round(trackHeight * this.panoramaVerticalAngleProgress()), 0, trackHeight);
        return this.panoramaVerticalProgressY() + progressHeight;
    }

    private static float wrapPanoramaRotation(float rotationDegrees) {
        float wrapped = rotationDegrees % PANORAMA_ROTATION_FULL_TURN_DEGREES;
        return wrapped < 0.0F ? wrapped + PANORAMA_ROTATION_FULL_TURN_DEGREES : wrapped;
    }

    private int[] fitBounds(int areaX, int areaY, int areaWidth, int areaHeight, int imageWidth, int imageHeight) {
        float scale = Math.min(areaWidth / (float) imageWidth, areaHeight / (float) imageHeight);
        int renderWidth = Math.max(1, Math.round(imageWidth * scale));
        int renderHeight = Math.max(1, Math.round(imageHeight * scale));
        int x = areaX + (areaWidth - renderWidth) / 2;
        int y = areaY + (areaHeight - renderHeight) / 2;
        return new int[]{x, y, renderWidth, renderHeight};
    }

    private int footerButtonY() {
        return this.height - 30;
    }

    private int imageAreaX() {
        return 54;
    }

    private int imageAreaY() {
        return 48;
    }

    private int imageAreaWidth() {
        return Math.max(80, this.width - 108);
    }

    private int imageAreaHeight() {
        return Math.max(60, this.height - 92);
    }

    private int hoverMouseX(int mouseX) {
        return this.panoramaMouseLookCursorGrabbed ? NO_HOVER_MOUSE_POSITION : mouseX;
    }

    private int hoverMouseY(int mouseY) {
        return this.panoramaMouseLookCursorGrabbed ? NO_HOVER_MOUSE_POSITION : mouseY;
    }

    @NotNull
    private static Identifier nextTextureId() {
        return Identifier.fromNamespaceAndPath(Snappy.MOD_ID, TEXTURE_PATH + textureSequence++);
    }

    private enum LoadStatus {
        LOADING,
        READY,
        FAILED
    }

}
