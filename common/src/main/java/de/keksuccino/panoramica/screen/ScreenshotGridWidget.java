package de.keksuccino.panoramica.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.keksuccino.panoramica.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import de.keksuccino.panoramica.screen.ScreenshotThumbnailCache.Thumbnail;
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
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ScreenshotGridWidget extends AbstractScrollArea implements AutoCloseable {

    private static final int PADDING = 10;
    private static final int TILE_WIDTH = 136;
    private static final int TILE_HEIGHT = 118;
    private static final int TILE_GAP = 8;
    private static final int SCROLL_AREA_BORDER_SIZE = 1;
    private static final int THUMBNAIL_BUFFER_ROWS = 2;
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 68;
    private static final int CHECKBOX_SIZE = 11;
    private static final int CARD_COLOR = 0x66000000;
    private static final int CARD_HOVER_COLOR = 0x80373737;
    private static final int CARD_SELECTED_COLOR = 0x80406090;
    private static final int SCROLL_AREA_BORDER_COLOR = 0xFF707070;
    private static final int BORDER_COLOR = 0xFF707070;
    private static final int BORDER_HOVER_COLOR = 0xFFFFFFFF;
    private static final int BORDER_SELECTED_COLOR = 0xFF75A7FF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB0B0B0;
    private static final int EMPTY_TEXT_COLOR = 0xFFA0A0A0;
    private static final double AUTO_SCROLL_EDGE_DISTANCE = 18.0;
    private static final double AUTO_SCROLL_MIN_SPEED = 70.0;
    private static final double AUTO_SCROLL_SPEED_PER_PIXEL = 4.0;
    private static final double AUTO_SCROLL_MAX_SPEED = 520.0;

    private final Font font;
    private final ScreenshotThumbnailCache thumbnailCache;
    private final Consumer<ScreenshotEntry> openCallback;
    private final Consumer<List<ScreenshotEntry>> deleteCallback;
    private final Runnable selectionChangedCallback;
    private List<ScreenshotEntry> entries = List.of();
    private final Set<ScreenshotEntry> selectedEntries = new HashSet<>();
    private int focusedIndex = -1;
    private int anchorIndex = -1;
    private boolean checkboxDragActive;
    private boolean checkboxDragRangeApplied;
    private int checkboxDragAnchorIndex = -1;
    private int checkboxDragCurrentIndex = -1;
    private double checkboxDragMouseX;
    private double checkboxDragMouseY;
    private long checkboxDragLastUpdateMillis;

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
        super(
                x + SCROLL_AREA_BORDER_SIZE,
                y + SCROLL_AREA_BORDER_SIZE,
                Math.max(1, width - SCROLL_AREA_BORDER_SIZE * 2),
                Math.max(1, height - SCROLL_AREA_BORDER_SIZE * 2),
                Component.translatable("panoramica.browser.grid"),
                AbstractScrollArea.defaultSettings(36)
        );
        this.font = font;
        this.thumbnailCache = new ScreenshotThumbnailCache(minecraft);
        this.openCallback = openCallback;
        this.deleteCallback = deleteCallback;
        this.selectionChangedCallback = selectionChangedCallback;
    }

    public void setEntries(@NotNull List<ScreenshotEntry> entries) {
        Set<String> selectedPaths = new HashSet<>();
        for (ScreenshotEntry entry : this.selectedEntries) {
            selectedPaths.add(entry.path().toString());
        }

        this.entries = List.copyOf(entries);
        this.selectedEntries.clear();
        for (ScreenshotEntry entry : this.entries) {
            if (selectedPaths.contains(entry.path().toString())) {
                this.selectedEntries.add(entry);
            }
        }

        this.focusedIndex = this.entries.isEmpty() ? -1 : Mth.clamp(this.focusedIndex, 0, this.entries.size() - 1);
        this.anchorIndex = this.focusedIndex;
        this.endCheckboxDrag();
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

    public void selectAll() {
        this.endCheckboxDrag();
        this.selectedEntries.clear();
        this.selectedEntries.addAll(this.entries);
        if (!this.entries.isEmpty()) {
            this.focusedIndex = 0;
            this.anchorIndex = 0;
        }
        this.selectionChangedCallback.run();
    }

    public void clearSelection() {
        this.endCheckboxDrag();
        if (!this.selectedEntries.isEmpty()) {
            this.selectedEntries.clear();
            this.selectionChangedCallback.run();
        }
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
        graphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x66000000);
        this.enableGridScissor(graphics);
        try {
            if (this.entries.isEmpty()) {
                this.thumbnailCache.retainOnly(List.of());
                Component emptyMessage = Component.translatable("panoramica.browser.empty");
                graphics.centeredText(this.font, emptyMessage, this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() / 2 - 4, EMPTY_TEXT_COLOR);
            } else {
                this.updateThumbnailWindow();
                this.renderTiles(graphics, mouseX, mouseY);
            }
        } finally {
            graphics.disableScissor();
        }
        this.extractScrollbar(graphics, mouseX, mouseY);
        graphics.outline(
                this.getX() - SCROLL_AREA_BORDER_SIZE,
                this.getY() - SCROLL_AREA_BORDER_SIZE,
                this.getWidth() + SCROLL_AREA_BORDER_SIZE * 2,
                this.getHeight() + SCROLL_AREA_BORDER_SIZE * 2,
                SCROLL_AREA_BORDER_COLOR
        );
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
            boolean hovered = i == hoveredIndex;
            boolean selected = this.selectedEntries.contains(entry);
            boolean focused = i == this.focusedIndex && this.isFocused();
            this.renderTile(graphics, entry, x, y, hovered, selected, focused);
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

    private void renderTile(
            @NotNull GuiGraphicsExtractor graphics,
            @NotNull ScreenshotEntry entry,
            int x,
            int y,
            boolean hovered,
            boolean selected,
            boolean focused
    ) {
        int borderColor = selected ? BORDER_SELECTED_COLOR : hovered || focused ? BORDER_HOVER_COLOR : BORDER_COLOR;
        graphics.fill(x, y, x + TILE_WIDTH, y + TILE_HEIGHT, borderColor);
        graphics.fill(x + 1, y + 1, x + TILE_WIDTH - 1, y + TILE_HEIGHT - 1, selected ? CARD_SELECTED_COLOR : hovered ? CARD_HOVER_COLOR : CARD_COLOR);

        int imageX = x + (TILE_WIDTH - IMAGE_WIDTH) / 2;
        int imageY = y + 8;
        graphics.fill(imageX, imageY, imageX + IMAGE_WIDTH, imageY + IMAGE_HEIGHT, 0xFF101010);
        this.renderThumbnail(graphics, entry, imageX, imageY);
        this.renderCheckbox(graphics, imageX + 4, imageY + 4, selected);

        String name = this.ellipsize(entry.displayName(), TILE_WIDTH - 14);
        graphics.text(this.font, name, x + 7, y + 82, TEXT_COLOR);

        Component type = Component.translatable(entry.typeLabelKey());
        graphics.text(this.font, type, x + 7, y + 94, entry.isPanorama() ? 0xFFFFC44D : 0xFF8FD8FF);
        if (!entry.formattedDate().isEmpty()) {
            String date = this.ellipsize(entry.formattedDate(), TILE_WIDTH - 14);
            graphics.text(this.font, date, x + 7, y + 105, SECONDARY_TEXT_COLOR);
        }
    }

    private void renderThumbnail(@NotNull GuiGraphicsExtractor graphics, @NotNull ScreenshotEntry entry, int x, int y) {
        Thumbnail thumbnail = this.thumbnailCache.thumbnailFor(entry);
        if (thumbnail instanceof Thumbnail.Ready ready) {
            float scale = Math.min(IMAGE_WIDTH / (float) ready.width(), IMAGE_HEIGHT / (float) ready.height());
            int renderWidth = Math.max(1, Math.round(ready.width() * scale));
            int renderHeight = Math.max(1, Math.round(ready.height() * scale));
            int renderX = x + (IMAGE_WIDTH - renderWidth) / 2;
            int renderY = y + (IMAGE_HEIGHT - renderHeight) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, ready.textureId(), renderX, renderY, 0.0F, 0.0F, renderWidth, renderHeight, ready.width(), ready.height(), ready.width(), ready.height());
        } else {
            Component message = thumbnail instanceof Thumbnail.Failed
                    ? Component.translatable("panoramica.browser.thumbnail_failed")
                    : Component.translatable("panoramica.browser.loading");
            graphics.centeredText(this.font, message, x + IMAGE_WIDTH / 2, y + IMAGE_HEIGHT / 2 - 4, EMPTY_TEXT_COLOR);
        }
    }

    private void renderCheckbox(@NotNull GuiGraphicsExtractor graphics, int x, int y, boolean selected) {
        graphics.fill(x - 1, y - 1, x + CHECKBOX_SIZE + 1, y + CHECKBOX_SIZE + 1, 0xCC000000);
        graphics.fill(x, y, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, selected ? 0xFF4CAF50 : 0xFF202020);
        graphics.outline(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE, 0xFFFFFFFF);
        if (selected) {
            graphics.fill(x + 3, y + 5, x + 5, y + 8, 0xFFFFFFFF);
            graphics.fill(x + 5, y + 7, x + 9, y + 9, 0xFFFFFFFF);
            graphics.fill(x + 8, y + 3, x + 10, y + 9, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (!this.visible || !this.active) {
            return false;
        }
        if (this.updateScrolling(event)) {
            return true;
        }

        int index = this.indexAt(event.x(), event.y());
        if (index < 0) {
            return false;
        }

        this.setFocused(true);
        this.focusedIndex = index;
        ScreenshotEntry entry = this.entries.get(index);
        if (this.isOverCheckbox(index, event.x(), event.y()) || event.hasControlDownWithQuirk()) {
            this.toggleSelection(entry);
            this.anchorIndex = index;
            if (this.isOverCheckbox(index, event.x(), event.y())) {
                this.beginCheckboxDrag(index, event.x(), event.y());
            }
            return true;
        }
        if (event.hasShiftDown()) {
            this.selectRange(index);
            return true;
        }

        this.anchorIndex = index;
        this.openCallback.accept(entry);
        return true;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dx, double dy) {
        if (this.checkboxDragActive && event.button() == 0) {
            this.checkboxDragMouseX = event.x();
            this.checkboxDragMouseY = event.y();
            this.updateCheckboxDragSelection();
            this.updateCheckboxDragAutoScroll();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        boolean handled = this.checkboxDragActive;
        this.endCheckboxDrag();
        return super.mouseReleased(event) || handled;
    }

    public void tickDragSelection() {
        if (this.checkboxDragActive) {
            this.updateCheckboxDragAutoScroll();
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
                ScreenshotEntry entry = this.entries.get(this.focusedIndex);
                this.toggleSelection(entry);
                this.anchorIndex = this.focusedIndex;
                return true;
            }
            return false;
        }
        if (event.key() == 257 || event.key() == 335) {
            if (this.focusedIndex >= 0) {
                this.openCallback.accept(this.entries.get(this.focusedIndex));
                return true;
            }
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

    private void moveFocus(int delta, boolean selecting) {
        int index = this.focusedIndex < 0 ? 0 : this.focusedIndex + delta;
        this.setFocusedIndex(Mth.clamp(index, 0, this.entries.size() - 1), selecting);
    }

    private void setFocusedIndex(int index, boolean selecting) {
        if (this.entries.isEmpty()) {
            return;
        }
        this.focusedIndex = Mth.clamp(index, 0, this.entries.size() - 1);
        if (selecting) {
            this.selectRange(this.focusedIndex);
        } else {
            this.anchorIndex = this.focusedIndex;
        }
        this.scrollToIndex(this.focusedIndex);
    }

    private void toggleSelection(@NotNull ScreenshotEntry entry) {
        if (!this.selectedEntries.remove(entry)) {
            this.selectedEntries.add(entry);
        }
        this.selectionChangedCallback.run();
    }

    private void selectRange(int index) {
        this.selectRange(index, true);
    }

    private void selectRange(int index, boolean scrollToTarget) {
        if (this.anchorIndex < 0) {
            this.anchorIndex = this.focusedIndex >= 0 ? this.focusedIndex : index;
        }
        int start = Math.min(this.anchorIndex, index);
        int end = Math.max(this.anchorIndex, index);
        this.selectedEntries.clear();
        for (int i = start; i <= end; i++) {
            this.selectedEntries.add(this.entries.get(i));
        }
        this.focusedIndex = index;
        if (scrollToTarget) {
            this.scrollToIndex(index);
        }
        this.selectionChangedCallback.run();
    }

    private void beginCheckboxDrag(int index, double mouseX, double mouseY) {
        this.checkboxDragActive = true;
        this.checkboxDragRangeApplied = false;
        this.checkboxDragAnchorIndex = index;
        this.checkboxDragCurrentIndex = index;
        this.checkboxDragMouseX = mouseX;
        this.checkboxDragMouseY = mouseY;
        this.checkboxDragLastUpdateMillis = Util.getMillis();
    }

    private void endCheckboxDrag() {
        this.checkboxDragActive = false;
        this.checkboxDragRangeApplied = false;
        this.checkboxDragAnchorIndex = -1;
        this.checkboxDragCurrentIndex = -1;
        this.checkboxDragLastUpdateMillis = 0L;
    }

    private void updateCheckboxDragSelection() {
        int targetIndex = this.dragTargetIndexAt(this.checkboxDragMouseX, this.checkboxDragMouseY);
        if (targetIndex < 0 || targetIndex >= this.entries.size()) {
            return;
        }
        if (!this.checkboxDragRangeApplied && targetIndex == this.checkboxDragAnchorIndex) {
            return;
        }
        if (this.checkboxDragRangeApplied && targetIndex == this.checkboxDragCurrentIndex) {
            return;
        }

        this.checkboxDragRangeApplied = true;
        this.checkboxDragCurrentIndex = targetIndex;
        this.anchorIndex = this.checkboxDragAnchorIndex;
        this.selectRange(targetIndex, false);
    }

    private void updateCheckboxDragAutoScroll() {
        if (!this.checkboxDragActive || !this.scrollable()) {
            this.checkboxDragLastUpdateMillis = Util.getMillis();
            return;
        }

        long now = Util.getMillis();
        long elapsedMillis = this.checkboxDragLastUpdateMillis <= 0L ? 0L : Math.min(100L, now - this.checkboxDragLastUpdateMillis);
        this.checkboxDragLastUpdateMillis = now;
        if (elapsedMillis <= 0L) {
            return;
        }

        double velocity = this.checkboxDragAutoScrollVelocity();
        if (velocity == 0.0) {
            return;
        }

        double previousScroll = this.scrollAmount();
        this.setScrollAmount(previousScroll + velocity * elapsedMillis / 1000.0);
        if (this.scrollAmount() != previousScroll) {
            this.updateCheckboxDragSelection();
        }
    }

    private double checkboxDragAutoScrollVelocity() {
        double topThreshold = this.getY() + AUTO_SCROLL_EDGE_DISTANCE;
        double bottomThreshold = this.getBottom() - AUTO_SCROLL_EDGE_DISTANCE;
        if (this.checkboxDragMouseY < topThreshold) {
            return -this.checkboxDragAutoScrollSpeed(topThreshold - this.checkboxDragMouseY);
        }
        if (this.checkboxDragMouseY > bottomThreshold) {
            return this.checkboxDragAutoScrollSpeed(this.checkboxDragMouseY - bottomThreshold);
        }
        return 0.0;
    }

    private double checkboxDragAutoScrollSpeed(double distanceFromEdge) {
        return Mth.clamp(AUTO_SCROLL_MIN_SPEED + distanceFromEdge * AUTO_SCROLL_SPEED_PER_PIXEL, AUTO_SCROLL_MIN_SPEED, AUTO_SCROLL_MAX_SPEED);
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

    private int dragTargetIndexAt(double mouseX, double mouseY) {
        int preciseIndex = this.indexAt(mouseX, mouseY);
        if (preciseIndex >= 0) {
            return preciseIndex;
        }
        if (this.entries.isEmpty()
                || mouseY >= this.getY() && mouseY < this.getBottom()
                || mouseX < this.getX()
                || mouseX >= this.scrollBarX()) {
            return -1;
        }

        int columns = this.columns();
        int gridLeft = this.gridLeft(columns);
        int gridRight = gridLeft + columns * TILE_WIDTH + Math.max(0, columns - 1) * TILE_GAP;
        int clampedX = Mth.clamp((int) mouseX, gridLeft, gridRight - 1);
        int localX = clampedX - gridLeft;
        int column = Mth.clamp(localX / (TILE_WIDTH + TILE_GAP), 0, columns - 1);
        int columnRemainder = localX % (TILE_WIDTH + TILE_GAP);
        if (columnRemainder >= TILE_WIDTH) {
            column = Mth.clamp(column + (columnRemainder - TILE_WIDTH >= TILE_GAP / 2 ? 1 : 0), 0, columns - 1);
        }

        int clampedY = mouseY < this.getY() ? this.getY() : this.getBottom() - 1;
        int localY = clampedY - this.getY() - PADDING + (int) this.scrollAmount();
        int row = Math.max(0, localY / (TILE_HEIGHT + TILE_GAP));
        int rowRemainder = localY % (TILE_HEIGHT + TILE_GAP);
        if (rowRemainder >= TILE_HEIGHT) {
            row += rowRemainder - TILE_HEIGHT >= TILE_GAP / 2 ? 1 : 0;
        }

        return Mth.clamp(row * columns + column, 0, this.entries.size() - 1);
    }

    private boolean isOverCheckbox(int index, double mouseX, double mouseY) {
        int columns = this.columns();
        int row = index / columns;
        int column = index % columns;
        int tileX = this.gridLeft(columns) + column * (TILE_WIDTH + TILE_GAP);
        int tileY = this.getY() + PADDING - (int) this.scrollAmount() + row * (TILE_HEIGHT + TILE_GAP);
        int imageX = tileX + (TILE_WIDTH - IMAGE_WIDTH) / 2;
        int checkboxX = imageX + 4;
        int checkboxY = tileY + 12;
        return mouseX >= checkboxX - 2 && mouseX < checkboxX + CHECKBOX_SIZE + 2
                && mouseY >= checkboxY - 2 && mouseY < checkboxY + CHECKBOX_SIZE + 2;
    }

    private boolean isOverThumbnail(int index, double mouseX, double mouseY) {
        int columns = this.columns();
        int row = index / columns;
        int column = index % columns;
        int tileX = this.gridLeft(columns) + column * (TILE_WIDTH + TILE_GAP);
        int tileY = this.getY() + PADDING - (int) this.scrollAmount() + row * (TILE_HEIGHT + TILE_GAP);
        int imageX = tileX + (TILE_WIDTH - IMAGE_WIDTH) / 2;
        int imageY = tileY + 8;
        return mouseX >= imageX && mouseX < imageX + IMAGE_WIDTH
                && mouseY >= imageY && mouseY < imageY + IMAGE_HEIGHT;
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
        output.add(NarratedElementType.TITLE, Component.translatable("panoramica.browser.grid"));
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
