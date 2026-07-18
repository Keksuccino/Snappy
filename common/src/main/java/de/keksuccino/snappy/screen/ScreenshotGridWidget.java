package de.keksuccino.snappy.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.client.gui.GuiBackground;
import de.keksuccino.snappy.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import de.keksuccino.snappy.screen.ScreenshotThumbnailCache.Thumbnail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ScreenshotGridWidget extends AbstractScrollArea implements AutoCloseable {

    private static final int SCROLL_AREA_BACKGROUND_GAP = 4;
    private static final int SCROLL_AREA_LEFT_INSET = GuiBackground.DEFAULT.leftBorder() + SCROLL_AREA_BACKGROUND_GAP;
    private static final int SCROLL_AREA_TOP_INSET = GuiBackground.DEFAULT.topBorder() + SCROLL_AREA_BACKGROUND_GAP;
    private static final int SCROLL_AREA_RIGHT_INSET = GuiBackground.DEFAULT.rightBorder() + SCROLL_AREA_BACKGROUND_GAP;
    private static final int SCROLL_AREA_BOTTOM_INSET = GuiBackground.DEFAULT.bottomBorder() + SCROLL_AREA_BACKGROUND_GAP;
    private static final int PADDING = 10;
    private static final Identifier TILE_IDLE_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/backgrounds/screenshot_preview_card_idle_136x118.png");
    private static final Identifier TILE_HOVER_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/gui/backgrounds/screenshot_preview_card_hover_136x118.png");
    private static final Identifier SCREENSHOT_MEDIA_TYPE_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/browser/media_type/screenshot_icon_13x13.png");
    private static final Identifier PANORAMA_MEDIA_TYPE_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/browser/media_type/panorama_icon_13x13.png");
    private static final int TILE_WIDTH = 136;
    private static final int TILE_HEIGHT = 118;
    private static final int TILE_GAP = 8;
    private static final int THUMBNAIL_BUFFER_ROWS = 2;
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 64;
    private static final int IMAGE_TOP_INSET = 8;
    private static final int THUMBNAIL_OVERLAY_INSET = 3;
    private static final int MEDIA_TYPE_ICON_SIZE = 13;
    private static final int SCROLLBAR_EDGE_INSET = 2;
    private static final int SCROLLBAR_TRACK_WIDTH = 2;
    private static final int SCROLLBAR_THUMB_WIDTH = 4;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 18;
    private static final int SCROLLBAR_TRACK_COLOR = 0x66404040;
    private static final int SCROLLBAR_THUMB_COLOR = 0xCCFFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB0B0B0;
    private static final int EMPTY_TEXT_COLOR = 0xFFA0A0A0;

    private final Font font;
    private final ScreenshotThumbnailCache thumbnailCache;
    private final Consumer<ScreenshotEntry> openCallback;
    private final Consumer<List<ScreenshotEntry>> deleteCallback;
    private final Runnable selectionChangedCallback;
    private List<ScreenshotEntry> entries = List.of();
    private final Set<ScreenshotEntry> selectedEntries = new HashSet<>();
    private int focusedIndex = -1;
    private int anchorIndex = -1;
    @Nullable
    private Path lastClickedPath;

    public ScreenshotGridWidget(
            @NotNull Minecraft minecraft,
            @NotNull Font font,
            int x,
            int y,
            int width,
            int height,
            @NotNull Consumer<ScreenshotEntry> openCallback,
            @NotNull Consumer<List<ScreenshotEntry>> deleteCallback,
            @NotNull Runnable selectionChangedCallback
    ) {
        super(x + SCROLL_AREA_LEFT_INSET, y + SCROLL_AREA_TOP_INSET, Math.max(1, width - SCROLL_AREA_LEFT_INSET - SCROLL_AREA_RIGHT_INSET), Math.max(1, height - SCROLL_AREA_TOP_INSET - SCROLL_AREA_BOTTOM_INSET), Component.translatable("snappy.browser.grid"), AbstractScrollArea.defaultSettings(36));
        this.font = font;
        this.thumbnailCache = new ScreenshotThumbnailCache(minecraft);
        this.openCallback = openCallback;
        this.deleteCallback = deleteCallback;
        this.selectionChangedCallback = selectionChangedCallback;
    }

    public void setEntries(@NotNull List<ScreenshotEntry> entries) {
        Set<Path> selectedPaths = new HashSet<>();
        for (ScreenshotEntry entry : this.selectedEntries) {
            selectedPaths.add(entry.path());
        }
        @Nullable Path focusedPath = this.entryPathAt(this.focusedIndex);
        @Nullable Path anchorPath = this.entryPathAt(this.anchorIndex);

        this.entries = List.copyOf(entries);
        this.selectedEntries.clear();
        for (ScreenshotEntry entry : this.entries) {
            if (selectedPaths.contains(entry.path())) {
                this.selectedEntries.add(entry);
            }
        }

        this.focusedIndex = this.indexOfPath(focusedPath);
        this.anchorIndex = this.indexOfPath(anchorPath);
        this.lastClickedPath = null;
        this.refreshScrollAmount();
        this.updateThumbnailWindow();
        this.selectionChangedCallback.run();
    }

    @NotNull
    public List<ScreenshotEntry> entries() {
        return this.entries;
    }

    @NotNull
    public List<ScreenshotEntry> selectedEntries() {
        List<ScreenshotEntry> result = new ArrayList<>();
        for (ScreenshotEntry entry : this.entries) {
            if (this.selectedEntries.contains(entry)) {
                result.add(entry);
            }
        }
        return result;
    }

    public int selectionCount() {
        return this.selectedEntries.size();
    }

    public boolean hasSelection() {
        return !this.selectedEntries.isEmpty();
    }

    public boolean isOverThumbnail(double mouseX, double mouseY) {
        int index = this.indexAt(mouseX, mouseY);
        return index >= 0 && this.isOverThumbnail(index, mouseX, mouseY);
    }

    public boolean isOverCard(double mouseX, double mouseY) {
        return this.indexAt(mouseX, mouseY) >= 0;
    }

    public void cancelPendingDoubleClick() {
        this.lastClickedPath = null;
    }

    public void selectAll() {
        this.selectedEntries.clear();
        this.selectedEntries.addAll(this.entries);
        if (!this.entries.isEmpty()) {
            if (this.focusedIndex < 0 || this.focusedIndex >= this.entries.size()) {
                this.focusedIndex = 0;
            }
            this.anchorIndex = this.focusedIndex;
        } else {
            this.focusedIndex = -1;
            this.anchorIndex = -1;
        }
        this.selectionChangedCallback.run();
    }

    @Override
    protected int contentHeight() {
        if (this.entries.isEmpty()) {
            return this.height;
        }
        int rows = Mth.ceil(this.entries.size() / (float) this.columns());
        return PADDING * 2 + rows * TILE_HEIGHT + Math.max(0, rows - 1) * TILE_GAP;
    }

    @Override
    protected void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        GuiBackground.DEFAULT.render(graphics, this.getX() - SCROLL_AREA_LEFT_INSET, this.getY() - SCROLL_AREA_TOP_INSET, this.getWidth() + SCROLL_AREA_LEFT_INSET + SCROLL_AREA_RIGHT_INSET, this.getHeight() + SCROLL_AREA_TOP_INSET + SCROLL_AREA_BOTTOM_INSET);
        this.enableGridScissor(graphics);
        try {
            if (this.entries.isEmpty()) {
                this.thumbnailCache.retainOnly(List.of());
                Component emptyMessage = Component.translatable("snappy.browser.empty");
                graphics.centeredText(this.font, emptyMessage, this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() / 2 - 4, EMPTY_TEXT_COLOR);
            } else {
                this.updateThumbnailWindow();
                this.renderTiles(graphics, mouseX, mouseY);
            }
        } finally {
            graphics.disableScissor();
        }
        this.extractScrollbar(graphics, mouseX, mouseY);
    }

    @Override
    protected int scrollerHeight() {
        int trackHeight = this.scrollbarTrackHeight();
        return Mth.clamp(this.getHeight() * this.getHeight() / Math.max(this.getHeight(), this.contentHeight()), SCROLLBAR_MIN_THUMB_HEIGHT, trackHeight);
    }

    @Override
    public int scrollBarY() {
        int maxScroll = this.maxScrollAmount();
        if (maxScroll <= 0) {
            return this.getY() + SCROLLBAR_EDGE_INSET;
        }

        int thumbTravel = Math.max(1, this.scrollbarTrackHeight() - this.scrollerHeight());
        return this.getY() + SCROLLBAR_EDGE_INSET + Math.round(thumbTravel * (float) (this.scrollAmount() / maxScroll));
    }

    @Override
    protected void extractScrollbar(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.scrollable()) {
            return;
        }

        int trackX = this.getRight() - 5;
        int trackY = this.getY() + SCROLLBAR_EDGE_INSET;
        int thumbY = this.scrollBarY();
        int thumbHeight = this.scrollerHeight();

        graphics.fill(trackX, trackY, trackX + SCROLLBAR_TRACK_WIDTH, this.getBottom() - SCROLLBAR_EDGE_INSET, SCROLLBAR_TRACK_COLOR);
        graphics.fill(trackX - 1, thumbY, trackX - 1 + SCROLLBAR_THUMB_WIDTH, thumbY + thumbHeight, SCROLLBAR_THUMB_COLOR);
        if (this.isOverScrollbar(mouseX, mouseY)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private int scrollbarTrackHeight() {
        return Math.max(1, this.getHeight() - SCROLLBAR_EDGE_INSET * 2);
    }

    private void renderTiles(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int columns = this.columns();
        int gridLeft = this.gridLeft(columns);
        int top = this.getY() + PADDING - (int) this.scrollAmount();
        int hoveredIndex = this.indexAt(mouseX, mouseY);

        for (int i = 0; i < this.entries.size(); i++) {
            int row = i / columns;
            int column = i % columns;
            int x = gridLeft + column * (TILE_WIDTH + TILE_GAP);
            int y = top + row * (TILE_HEIGHT + TILE_GAP);
            if (y + TILE_HEIGHT < this.getY() || y > this.getBottom()) {
                continue;
            }

            ScreenshotEntry entry = this.entries.get(i);
            boolean highlighted = this.selectedEntries.contains(entry) || i == hoveredIndex;
            this.renderTile(graphics, entry, x, y, highlighted);
        }

        if (hoveredIndex >= 0 || this.isOverScrollbar(mouseX, mouseY)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void updateThumbnailWindow() {
        if (this.entries.isEmpty()) {
            this.thumbnailCache.retainOnly(List.of());
            return;
        }

        int columns = this.columns();
        int rowCount = Mth.ceil(this.entries.size() / (float) columns);
        int rowHeight = TILE_HEIGHT + TILE_GAP;
        int bufferHeight = THUMBNAIL_BUFFER_ROWS * rowHeight;
        int visibleTop = Math.max(0, (int) this.scrollAmount() - bufferHeight);
        int visibleBottom = (int) this.scrollAmount() + this.height + bufferHeight;
        int startRow = Math.max(0, (visibleTop - PADDING) / rowHeight);
        int endRow = Mth.clamp((visibleBottom - PADDING) / rowHeight, 0, rowCount - 1);
        int startIndex = Math.min(this.entries.size(), startRow * columns);
        int endIndex = Math.min(this.entries.size(), (endRow + 1) * columns);
        List<ScreenshotEntry> retainedEntries = this.entries.subList(startIndex, Math.max(startIndex, endIndex));

        this.thumbnailCache.retainOnly(retainedEntries);
        for (ScreenshotEntry entry : retainedEntries) {
            this.thumbnailCache.thumbnailFor(entry);
        }
    }

    private void enableGridScissor(@NotNull GuiGraphicsExtractor graphics) {
        graphics.enableScissor(
                Mth.clamp(this.getX(), 0, graphics.guiWidth()),
                Mth.clamp(this.getY(), 0, graphics.guiHeight()),
                Mth.clamp(this.getRight(), 0, graphics.guiWidth()),
                Mth.clamp(this.getBottom(), 0, graphics.guiHeight())
        );
    }

    private void renderTile(@NotNull GuiGraphicsExtractor graphics, @NotNull ScreenshotEntry entry, int x, int y, boolean highlighted) {
        Identifier backgroundTexture = highlighted ? TILE_HOVER_BACKGROUND_TEXTURE : TILE_IDLE_BACKGROUND_TEXTURE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, x, y, 0.0F, 0.0F, TILE_WIDTH, TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);

        int imageX = x + (TILE_WIDTH - IMAGE_WIDTH) / 2;
        int imageY = y + IMAGE_TOP_INSET;
        graphics.fill(imageX, imageY, imageX + IMAGE_WIDTH, imageY + IMAGE_HEIGHT, 0xFF101010);
        this.renderThumbnail(graphics, entry, imageX, imageY);
        this.renderMediaTypeIcon(graphics, entry, imageX + IMAGE_WIDTH - THUMBNAIL_OVERLAY_INSET - MEDIA_TYPE_ICON_SIZE, imageY + THUMBNAIL_OVERLAY_INSET);

        String name = this.ellipsize(entry.displayName(), TILE_WIDTH - 14);
        graphics.text(this.font, name, x + 7, y + 92, TEXT_COLOR);

        if (!entry.formattedDate().isEmpty()) {
            String date = this.ellipsize(entry.formattedDate(), TILE_WIDTH - 14);
            graphics.text(this.font, date, x + 7, y + 103, SECONDARY_TEXT_COLOR);
        }
    }

    private void renderThumbnail(@NotNull GuiGraphicsExtractor graphics, @NotNull ScreenshotEntry entry, int x, int y) {
        Thumbnail thumbnail = this.thumbnailCache.thumbnailFor(entry);
        if (thumbnail instanceof Thumbnail.Ready ready) {
            float scale = Math.max(IMAGE_WIDTH / (float) ready.width(), IMAGE_HEIGHT / (float) ready.height());
            int renderWidth = Math.max(IMAGE_WIDTH, (int) Math.ceil(ready.width() * scale));
            int renderHeight = Math.max(IMAGE_HEIGHT, (int) Math.ceil(ready.height() * scale));
            int renderX = x + Math.floorDiv(IMAGE_WIDTH - renderWidth, 2);
            int renderY = y + Math.floorDiv(IMAGE_HEIGHT - renderHeight, 2);

            // This clip is nested inside the grid clip. Keep the push and pop paired so later tile content retains the scroll-area scissor.
            graphics.enableScissor(x, y, x + IMAGE_WIDTH, y + IMAGE_HEIGHT);
            try {
                graphics.blit(RenderPipelines.GUI_TEXTURED, ready.textureId(), renderX, renderY, 0.0F, 0.0F, renderWidth, renderHeight, ready.width(), ready.height(), ready.width(), ready.height());
            } finally {
                graphics.disableScissor();
            }
        } else {
            Component message = thumbnail instanceof Thumbnail.Failed
                    ? Component.translatable("snappy.browser.thumbnail_failed")
                    : Component.translatable("snappy.browser.loading");
            graphics.centeredText(this.font, message, x + IMAGE_WIDTH / 2, y + IMAGE_HEIGHT / 2 - 4, EMPTY_TEXT_COLOR);
        }
    }

    private void renderMediaTypeIcon(@NotNull GuiGraphicsExtractor graphics, @NotNull ScreenshotEntry entry, int x, int y) {
        Identifier icon = entry.isPanorama() ? PANORAMA_MEDIA_TYPE_ICON : SCREENSHOT_MEDIA_TYPE_ICON;
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0F, 0.0F, MEDIA_TYPE_ICON_SIZE, MEDIA_TYPE_ICON_SIZE, MEDIA_TYPE_ICON_SIZE, MEDIA_TYPE_ICON_SIZE);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (!this.visible || !this.active || event.button() != 0) {
            return false;
        }
        if (this.updateScrolling(event)) {
            this.lastClickedPath = null;
            return true;
        }

        int index = this.indexAt(event.x(), event.y());
        if (index < 0) {
            this.lastClickedPath = null;
            return false;
        }

        this.setFocused(true);
        ScreenshotEntry entry = this.entries.get(index);
        // Minecraft reports double-click timing per screen and mouse button, not per card. Comparing paths prevents two rapid clicks on different controls or screenshots from opening the second screenshot.
        boolean openEntry = doubleClick && entry.path().equals(this.lastClickedPath);
        this.lastClickedPath = entry.path();
        if (openEntry) {
            this.focusedIndex = index;
            this.openCallback.accept(entry);
            return true;
        }
        // This event helper uses Control on Windows/Linux and Command on macOS while preserving native modifier remapping.
        this.selectFromModifiers(index, event.hasShiftDown(), event.hasControlDownWithQuirk());
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        // Pointer interactions apply their selection explicitly. Only keyboard navigation entering the grid should select its focused card like a normal click.
        boolean gainedKeyboardFocus = focused && !this.isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
        super.setFocused(focused);
        if (gainedKeyboardFocus && !this.entries.isEmpty()) {
            int index = this.focusedIndex >= 0 ? this.focusedIndex : this.firstSelectedIndex();
            this.selectOnly(index >= 0 ? index : 0);
            this.scrollToIndex(this.focusedIndex);
        }
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (!this.visible || !this.active || this.entries.isEmpty()) {
            return false;
        }

        if (event.isSelectAll()) {
            this.selectAll();
            return true;
        }
        if (event.key() == 259 || event.key() == 261) {
            if (this.hasSelection()) {
                this.deleteCallback.accept(this.selectedEntries());
                return true;
            }
            return false;
        }
        if (event.key() == 32) {
            if (this.focusedIndex >= 0) {
                this.selectFromModifiers(this.focusedIndex, event.hasShiftDown(), event.hasControlDownWithQuirk());
                return true;
            }
            return false;
        }
        if (event.isConfirmation()) {
            if (this.focusedIndex >= 0) {
                this.openCallback.accept(this.entries.get(this.focusedIndex));
                return true;
            }
            return false;
        }
        if ((event.isUp() && this.focusedIndexInTopRow()) || (event.isDown() && this.focusedIndexInBottomRow())) {
            return false;
        }
        if (event.isLeft() || event.isRight() || event.isUp() || event.isDown()) {
            this.moveFocus(this.navigationDelta(event), event.hasShiftDown());
            return true;
        }
        if (event.key() == 268) {
            this.setFocusedIndex(0, event.hasShiftDown());
            return true;
        }
        if (event.key() == 269) {
            this.setFocusedIndex(this.entries.size() - 1, event.hasShiftDown());
            return true;
        }

        return false;
    }

    private int navigationDelta(@NotNull KeyEvent event) {
        if (event.isLeft()) {
            return -1;
        }
        if (event.isRight()) {
            return 1;
        }
        if (event.isUp()) {
            return -this.columns();
        }
        return this.columns();
    }

    private boolean focusedIndexInTopRow() {
        return this.focusedIndex >= 0 && this.focusedIndex < this.columns();
    }

    private boolean focusedIndexInBottomRow() {
        if (this.focusedIndex < 0) {
            return false;
        }
        int columns = this.columns();
        return this.focusedIndex / columns == (this.entries.size() - 1) / columns;
    }

    private void moveFocus(int delta, boolean selecting) {
        int index = this.focusedIndex < 0 ? 0 : this.focusedIndex + delta;
        this.setFocusedIndex(Mth.clamp(index, 0, this.entries.size() - 1), selecting);
    }

    private void setFocusedIndex(int index, boolean selecting) {
        if (this.entries.isEmpty()) {
            return;
        }
        int clampedIndex = Mth.clamp(index, 0, this.entries.size() - 1);
        if (selecting) {
            this.selectRange(clampedIndex);
        } else {
            this.selectOnly(clampedIndex);
        }
        this.scrollToIndex(this.focusedIndex);
    }

    private void selectFromModifiers(int index, boolean rangeSelection, boolean additiveSelection) {
        if (rangeSelection) {
            this.selectRange(index);
        } else if (additiveSelection) {
            this.addSelection(index);
        } else {
            this.selectOnly(index);
        }
    }

    private void selectOnly(int index) {
        this.focusedIndex = Mth.clamp(index, 0, this.entries.size() - 1);
        this.anchorIndex = this.focusedIndex;
        this.selectedEntries.clear();
        this.selectedEntries.add(this.entries.get(this.focusedIndex));
        this.selectionChangedCallback.run();
    }

    private void addSelection(int index) {
        this.focusedIndex = Mth.clamp(index, 0, this.entries.size() - 1);
        this.anchorIndex = this.focusedIndex;
        this.selectedEntries.add(this.entries.get(this.focusedIndex));
        this.selectionChangedCallback.run();
    }

    private void selectRange(int index) {
        int targetIndex = Mth.clamp(index, 0, this.entries.size() - 1);
        if (this.anchorIndex < 0 || this.anchorIndex >= this.entries.size()) {
            this.anchorIndex = this.focusedIndex >= 0 && this.selectedEntries.contains(this.entries.get(this.focusedIndex)) ? this.focusedIndex : targetIndex;
        }
        int start = Math.min(this.anchorIndex, targetIndex);
        int end = Math.max(this.anchorIndex, targetIndex);
        this.selectedEntries.clear();
        for (int i = start; i <= end; i++) {
            this.selectedEntries.add(this.entries.get(i));
        }
        this.focusedIndex = targetIndex;
        this.selectionChangedCallback.run();
    }

    private void scrollToIndex(int index) {
        int columns = this.columns();
        int row = index / columns;
        int itemTop = PADDING + row * (TILE_HEIGHT + TILE_GAP);
        int itemBottom = itemTop + TILE_HEIGHT;
        int visibleTop = (int) this.scrollAmount();
        int visibleBottom = visibleTop + this.height;

        if (itemTop < visibleTop) {
            this.setScrollAmount(itemTop);
        } else if (itemBottom > visibleBottom) {
            this.setScrollAmount(itemBottom - this.height + PADDING);
        }
    }

    private int indexAt(double mouseX, double mouseY) {
        if (!this.isMouseOver(mouseX, mouseY) || this.isOverScrollbar(mouseX, mouseY)) {
            return -1;
        }

        int columns = this.columns();
        int gridLeft = this.gridLeft(columns);
        int localX = (int) mouseX - gridLeft;
        int localY = (int) mouseY - this.getY() - PADDING + (int) this.scrollAmount();
        if (localX < 0 || localY < 0) {
            return -1;
        }

        int column = localX / (TILE_WIDTH + TILE_GAP);
        int columnRemainder = localX % (TILE_WIDTH + TILE_GAP);
        int row = localY / (TILE_HEIGHT + TILE_GAP);
        int rowRemainder = localY % (TILE_HEIGHT + TILE_GAP);
        if (column >= columns || columnRemainder >= TILE_WIDTH || rowRemainder >= TILE_HEIGHT) {
            return -1;
        }

        int index = row * columns + column;
        return index >= 0 && index < this.entries.size() ? index : -1;
    }

    private boolean isOverThumbnail(int index, double mouseX, double mouseY) {
        int columns = this.columns();
        int row = index / columns;
        int column = index % columns;
        int tileX = this.gridLeft(columns) + column * (TILE_WIDTH + TILE_GAP);
        int tileY = this.getY() + PADDING - (int) this.scrollAmount() + row * (TILE_HEIGHT + TILE_GAP);
        int imageX = tileX + (TILE_WIDTH - IMAGE_WIDTH) / 2;
        int imageY = tileY + IMAGE_TOP_INSET;
        return mouseX >= imageX && mouseX < imageX + IMAGE_WIDTH
                && mouseY >= imageY && mouseY < imageY + IMAGE_HEIGHT;
    }

    private int firstSelectedIndex() {
        for (int i = 0; i < this.entries.size(); i++) {
            if (this.selectedEntries.contains(this.entries.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private Path entryPathAt(int index) {
        return index >= 0 && index < this.entries.size() ? this.entries.get(index).path() : null;
    }

    private int indexOfPath(@Nullable Path path) {
        if (path == null) {
            return -1;
        }
        for (int i = 0; i < this.entries.size(); i++) {
            if (this.entries.get(i).path().equals(path)) {
                return i;
            }
        }
        return -1;
    }

    private int columns() {
        int availableWidth = Math.max(TILE_WIDTH, this.getWidth() - PADDING * 2 - this.scrollbarWidth() - 2);
        return Math.max(1, (availableWidth + TILE_GAP) / (TILE_WIDTH + TILE_GAP));
    }

    private int gridLeft(int columns) {
        int gridWidth = columns * TILE_WIDTH + (columns - 1) * TILE_GAP;
        int contentWidth = this.getWidth() - this.scrollbarWidth();
        return this.getX() + Math.max(PADDING, (contentWidth - gridWidth) / 2);
    }

    @NotNull
    private String ellipsize(@NotNull String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = this.font.width(ellipsis);
        return this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - ellipsisWidth)) + ellipsis;
    }

    @Override
    public void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("snappy.browser.grid"));
        if (this.focusedIndex >= 0 && this.focusedIndex < this.entries.size()) {
            ScreenshotEntry entry = this.entries.get(this.focusedIndex);
            output.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", this.focusedIndex + 1, this.entries.size()));
            output.add(NarratedElementType.HINT, Component.literal(entry.displayName()));
        }
    }

    @Override
    public void close() {
        this.thumbnailCache.close();
    }

}
