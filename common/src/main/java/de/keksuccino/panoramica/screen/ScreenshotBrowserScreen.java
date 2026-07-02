package de.keksuccino.panoramica.screen;

import de.keksuccino.panoramica.menu.PanoramaMenuManager;
import de.keksuccino.panoramica.screen.ScreenshotBrowserCatalog.DeletionResult;
import de.keksuccino.panoramica.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScreenshotBrowserScreen extends Screen {

    private static final int HEADER_HEIGHT = 74;
    private static final int FOOTER_HEIGHT = 64;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SIDE_MARGIN = 20;
    private static final int BUTTON_GAP = 6;
    private static final int STATUS_MESSAGE_MARGIN = 20;
    private static final long STATUS_MESSAGE_VISIBLE_MILLIS = 10_000L;
    private static final int STATUS_SUCCESS_COLOR = 0xFF78E878;
    private static final int STATUS_WARNING_COLOR = 0xFFFFD166;

    @Nullable
    private final Screen parent;
    private List<ScreenshotEntry> allEntries = List.of();
    private List<ScreenshotEntry> filteredEntries = List.of();
    @Nullable
    private ScreenshotGridWidget grid;
    @Nullable
    private EditBox searchBox;
    @Nullable
    private Button deleteSelectedButton;
    @Nullable
    private Button clearSelectionButton;
    private String searchQuery = "";
    private Component statusMessage = Component.empty();
    private int statusMessageColor = 0xFFFFFFFF;
    private long statusMessageExpiresAtMillis;

    public ScreenshotBrowserScreen(@Nullable Screen parent) {
        super(Component.translatable("panoramica.browser.title"));
        this.parent = parent;
    }

    @Override
    public void added() {
        this.refreshEntries();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        StringWidget titleWidget = this.addRenderableWidget(new StringWidget(this.title, this.font));
        titleWidget.setX(centerX - titleWidget.getWidth() / 2);
        titleWidget.setY(12);

        int searchWidth = Math.min(260, Math.max(120, this.width - SIDE_MARGIN * 2));
        this.searchBox = this.addRenderableWidget(new EditBox(this.font, this.width - SIDE_MARGIN - searchWidth, 42, searchWidth, BUTTON_HEIGHT, Component.translatable("panoramica.browser.search")));
        this.searchBox.setMaxLength(128);
        this.searchBox.setHint(Component.translatable("panoramica.browser.search_hint"));
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(value -> {
            this.searchQuery = value;
            this.applyFilter();
        });

        int gridY = HEADER_HEIGHT;
        int gridHeight = Math.max(80, this.height - HEADER_HEIGHT - FOOTER_HEIGHT);
        this.grid = this.addRenderableWidget(new ScreenshotGridWidget(
                this.minecraft,
                this.font,
                SIDE_MARGIN,
                gridY,
                this.width - SIDE_MARGIN * 2,
                gridHeight,
                this::openEntry,
                this::confirmDelete,
                this::updateSelectionButtons
        ));
        this.applyFilter();

        int firstRowY = this.height - 54;
        int secondRowY = this.height - 28;
        int rowButtonWidth = Math.max(54, (this.width - SIDE_MARGIN * 2 - BUTTON_GAP * 3) / 4);
        int leftX = SIDE_MARGIN;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).bounds(leftX, firstRowY, rowButtonWidth, BUTTON_HEIGHT).build());
        leftX += rowButtonWidth + BUTTON_GAP;
        this.addRenderableWidget(Button.builder(Component.translatable("panoramica.browser.refresh"), button -> this.refreshEntries()).bounds(leftX, firstRowY, rowButtonWidth, BUTTON_HEIGHT).build());
        leftX += rowButtonWidth + BUTTON_GAP;
        this.addRenderableWidget(Button.builder(Component.translatable("panoramica.browser.select_all"), button -> {
            ScreenshotGridWidget currentGrid = this.grid;
            if (currentGrid != null) {
                currentGrid.selectAll();
            }
        }).bounds(leftX, firstRowY, rowButtonWidth, BUTTON_HEIGHT).build());
        leftX += rowButtonWidth + BUTTON_GAP;
        this.clearSelectionButton = this.addRenderableWidget(Button.builder(Component.translatable("panoramica.browser.clear_selection"), button -> {
            ScreenshotGridWidget currentGrid = this.grid;
            if (currentGrid != null) {
                currentGrid.clearSelection();
            }
        }).bounds(leftX, firstRowY, rowButtonWidth, BUTTON_HEIGHT).build());

        int deleteWidth = Math.min(210, Math.max(150, this.width - SIDE_MARGIN * 2));
        this.deleteSelectedButton = this.addRenderableWidget(Button.builder(Component.translatable("panoramica.browser.delete_selected", 0), button -> {
            ScreenshotGridWidget currentGrid = this.grid;
            if (currentGrid != null && currentGrid.hasSelection()) {
                this.confirmDelete(currentGrid.selectedEntries());
            }
        }).bounds(centerX - deleteWidth / 2, secondRowY, deleteWidth, BUTTON_HEIGHT).build());
        this.updateSelectionButtons();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int countY = this.searchBox == null ? 42 : this.searchBox.getY() + (this.searchBox.getHeight() - this.font.lineHeight) / 2;
        Component countText = Component.translatable("panoramica.browser.count", this.filteredEntries.size(), this.allEntries.size());
        graphics.text(this.font, countText, SIDE_MARGIN, countY, 0xFFFFFFFF);
        this.renderStatusMessage(graphics);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            return super.keyPressed(event);
        }
        if (event.isSelectAll()) {
            ScreenshotGridWidget currentGrid = this.grid;
            if (currentGrid != null) {
                currentGrid.selectAll();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        ScreenshotGridWidget currentGrid = this.grid;
        if (currentGrid != null) {
            currentGrid.close();
            this.grid = null;
        }
    }

    @Override
    public void tick() {
        ScreenshotGridWidget currentGrid = this.grid;
        if (currentGrid != null) {
            currentGrid.tickDragSelection();
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    public void refreshEntries() {
        if (this.minecraft == null) {
            return;
        }
        this.allEntries = ScreenshotBrowserCatalog.scan(this.minecraft);
        this.applyFilter();
    }

    private void applyFilter() {
        String query = this.searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            this.filteredEntries = this.allEntries;
        } else {
            List<ScreenshotEntry> result = new ArrayList<>();
            for (ScreenshotEntry entry : this.allEntries) {
                String type = entry.isPanorama() ? "panorama" : "normal screenshot";
                if (entry.lowerCaseDisplayName().contains(query)
                        || entry.formattedDate().toLowerCase(Locale.ROOT).contains(query)
                        || type.contains(query)) {
                    result.add(entry);
                }
            }
            this.filteredEntries = List.copyOf(result);
        }

        ScreenshotGridWidget currentGrid = this.grid;
        if (currentGrid != null) {
            currentGrid.setEntries(this.filteredEntries);
        }
    }

    private void openEntry(@NotNull ScreenshotEntry entry) {
        int index = this.filteredEntries.indexOf(entry);
        if (index >= 0) {
            this.minecraft.gui.setScreen(new ScreenshotViewerScreen(this, this.filteredEntries, index));
        }
    }

    private void confirmDelete(@NotNull List<ScreenshotEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        Component title = Component.translatable("panoramica.browser.delete_confirm.title");
        Component message = entries.size() == 1
                ? Component.translatable("panoramica.browser.delete_confirm.single")
                : Component.translatable("panoramica.browser.delete_confirm.multiple", entries.size());
        this.minecraft.gui.setScreen(new ConfirmScreen(result -> {
            this.minecraft.gui.setScreen(this);
            if (result) {
                this.deleteEntries(entries);
            }
        }, title, message, Component.translatable("panoramica.browser.delete"), CommonComponents.GUI_CANCEL));
    }

    private void deleteEntries(@NotNull List<ScreenshotEntry> entries) {
        DeletionResult result = ScreenshotBrowserCatalog.deleteAll(entries);
        PanoramaMenuManager.invalidate();
        this.refreshEntries();

        if (result.failed() > 0) {
            this.showStatusMessage(Component.translatable("panoramica.browser.delete_result.partial", result.deleted(), result.failed()), STATUS_WARNING_COLOR);
        } else {
            this.showStatusMessage(result.deleted() == 1
                    ? Component.translatable("panoramica.browser.delete_result.single")
                    : Component.translatable("panoramica.browser.delete_result.multiple", result.deleted()), STATUS_SUCCESS_COLOR);
        }
    }

    private void showStatusMessage(@NotNull Component message, int color) {
        this.statusMessage = message;
        this.statusMessageColor = color;
        this.statusMessageExpiresAtMillis = Util.getMillis() + STATUS_MESSAGE_VISIBLE_MILLIS;
    }

    private void renderStatusMessage(@NotNull GuiGraphicsExtractor graphics) {
        if (this.statusMessage.getString().isEmpty()) {
            return;
        }
        if (Util.getMillis() >= this.statusMessageExpiresAtMillis) {
            this.statusMessage = Component.empty();
            this.statusMessageExpiresAtMillis = 0L;
            return;
        }

        int maxWidth = Math.max(40, this.width - STATUS_MESSAGE_MARGIN * 2);
        String message = this.ellipsize(this.statusMessage.getString(), maxWidth);
        int textWidth = this.font.width(message);
        int x = Math.max(STATUS_MESSAGE_MARGIN, this.width - STATUS_MESSAGE_MARGIN - textWidth);
        graphics.text(this.font, message, x, STATUS_MESSAGE_MARGIN, this.statusMessageColor);
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

    private void updateSelectionButtons() {
        ScreenshotGridWidget currentGrid = this.grid;
        int selected = currentGrid == null ? 0 : currentGrid.selectionCount();
        if (this.deleteSelectedButton != null) {
            this.deleteSelectedButton.active = selected > 0;
            this.deleteSelectedButton.setMessage(Component.translatable("panoramica.browser.delete_selected", selected));
        }
        if (this.clearSelectionButton != null) {
            this.clearSelectionButton.active = selected > 0;
        }
    }

}
