package de.keksuccino.panoramica.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.keksuccino.panoramica.Options;
import de.keksuccino.panoramica.OptionsScreen;
import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.menu.PanoramaMenuManager;
import de.keksuccino.panoramica.screen.ScreenshotBrowserCatalog.DeletionResult;
import de.keksuccino.panoramica.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ScreenshotBrowserScreen extends Screen {

    private static final int HEADER_HEIGHT = 74;
    private static final int FOOTER_HEIGHT = 42;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SIDE_MARGIN = 20;
    private static final int BUTTON_GAP = 6;
    private static final int HEADER_CONTROL_Y = 42;
    private static final int SORT_BUTTON_WIDTH = 126;
    private static final int SEARCH_WIDTH = 210;
    private static final int STATUS_MESSAGE_MARGIN = 20;
    private static final long STATUS_MESSAGE_VISIBLE_MILLIS = 10_000L;
    private static final int STATUS_SUCCESS_COLOR = 0xFF78E878;
    private static final int STATUS_WARNING_COLOR = 0xFFFFD166;
    private static final Identifier BACK_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/detail_back_icon_15x15.png");
    private static final Identifier SETTINGS_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/settings_icon_15x15.png");
    private static final Identifier REFRESH_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/refresh_icon_15x15.png");
    private static final Identifier SELECT_ALL_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/select_all_icon_15x15.png");
    private static final Identifier CLEAR_SELECTION_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/clear_selection_icon_15x15.png");
    private static final Identifier DELETE_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/delete_icon_15x15.png");

    @Nullable
    private final Screen parent;
    private List<ScreenshotEntry> allEntries = List.of();
    private List<ScreenshotEntry> filteredEntries = List.of();
    @Nullable
    private ScreenshotGridWidget grid;
    @Nullable
    private Button settingsButton;
    @Nullable
    private Button sortButton;
    @Nullable
    private EditBox searchBox;
    @Nullable
    private Button deleteSelectedButton;
    @Nullable
    private Button clearSelectionButton;
    private Options.BrowserSortMode sortMode = Options.BrowserSortMode.NEWEST_FIRST;
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
        this.sortMode = Panoramica.getOptions().getBrowserSortMode();

        int headerControlWidth = Math.max(120, this.width - SIDE_MARGIN * 2);
        int settingsButtonWidth = TexturedIconButton.DEFAULT_BUTTON_SIZE;
        int searchWidth = Math.min(SEARCH_WIDTH, Math.max(80, headerControlWidth - SORT_BUTTON_WIDTH - settingsButtonWidth - BUTTON_GAP * 2));
        int sortButtonWidth = Math.min(SORT_BUTTON_WIDTH, Math.max(70, headerControlWidth - searchWidth - settingsButtonWidth - BUTTON_GAP * 2));
        int searchX = this.width - SIDE_MARGIN - searchWidth;
        int sortButtonX = searchX - BUTTON_GAP - sortButtonWidth;
        int settingsButtonX = sortButtonX - BUTTON_GAP - settingsButtonWidth;
        this.settingsButton = this.addRenderableWidget(new TexturedIconButton(
                Component.translatable("panoramica.browser.settings"),
                button -> Minecraft.getInstance().gui.setScreen(new OptionsScreen(this)),
                SETTINGS_ICON
        ));
        this.settingsButton.setPosition(settingsButtonX, HEADER_CONTROL_Y);

        this.sortButton = this.addRenderableWidget(Button.builder(this.sortModeMessage(), button -> {
            this.sortMode = this.sortMode.next();
            Panoramica.getOptions().setBrowserSortMode(this.sortMode);
            this.updateSortButton();
            this.applyFilter();
        }).bounds(sortButtonX, HEADER_CONTROL_Y, sortButtonWidth, BUTTON_HEIGHT).build());

        this.searchBox = this.addRenderableWidget(new EditBox(this.font, searchX, HEADER_CONTROL_Y, searchWidth, BUTTON_HEIGHT, Component.translatable("panoramica.browser.search")));
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

        int footerY = this.height - 30;
        int iconButtonWidth = TexturedIconButton.DEFAULT_BUTTON_SIZE;
        int totalFooterWidth = iconButtonWidth * 5 + BUTTON_GAP * 4;
        int footerX = centerX - totalFooterWidth / 2;

        this.addFooterIconButton(footerX, footerY, CommonComponents.GUI_BACK, button -> this.onClose(), BACK_ICON);
        footerX += iconButtonWidth + BUTTON_GAP;
        this.addFooterIconButton(footerX, footerY, Component.translatable("panoramica.browser.refresh"), button -> this.refreshEntries(), REFRESH_ICON);
        footerX += iconButtonWidth + BUTTON_GAP;
        this.addFooterIconButton(footerX, footerY, Component.translatable("panoramica.browser.select_all"), button -> {
            ScreenshotGridWidget currentGrid = this.grid;
            if (currentGrid != null) {
                currentGrid.selectAll();
            }
        }, SELECT_ALL_ICON);
        footerX += iconButtonWidth + BUTTON_GAP;
        this.clearSelectionButton = this.addFooterIconButton(footerX, footerY, Component.translatable("panoramica.browser.clear_selection"), button -> {
            ScreenshotGridWidget currentGrid = this.grid;
            if (currentGrid != null) {
                currentGrid.clearSelection();
            }
        }, CLEAR_SELECTION_ICON);
        footerX += iconButtonWidth + BUTTON_GAP;
        this.deleteSelectedButton = this.addFooterIconButton(footerX, footerY, this.deleteSelectedMessage(0), button -> {
            ScreenshotGridWidget currentGrid = this.grid;
            if (currentGrid != null && currentGrid.hasSelection()) {
                this.confirmDelete(currentGrid.selectedEntries());
            }
        }, DELETE_ICON);
        this.updateSelectionButtons();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int countY = this.searchBox == null ? HEADER_CONTROL_Y : this.searchBox.getY() + this.searchBox.getHeight() - this.font.lineHeight;
        Component countText = Component.translatable("panoramica.browser.count", this.filteredEntries.size());
        Button leftmostHeaderButton = this.settingsButton != null ? this.settingsButton : this.sortButton;
        int countMaxWidth = leftmostHeaderButton == null ? this.width - SIDE_MARGIN * 2 : Math.max(20, leftmostHeaderButton.getX() - SIDE_MARGIN - BUTTON_GAP);
        graphics.text(this.font, this.ellipsize(countText.getString(), countMaxWidth), SIDE_MARGIN, countY, 0xFFFFFFFF);
        this.renderStatusMessage(graphics);

        if (this.grid != null && this.grid.isOverThumbnail(mouseX, mouseY)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
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
        List<ScreenshotEntry> result = new ArrayList<>();
        if (query.isEmpty()) {
            result.addAll(this.allEntries);
        } else {
            for (ScreenshotEntry entry : this.allEntries) {
                String type = entry.isPanorama() ? "panorama" : "normal screenshot";
                if (entry.lowerCaseDisplayName().contains(query)
                        || entry.formattedDate().toLowerCase(Locale.ROOT).contains(query)
                        || type.contains(query)) {
                    result.add(entry);
                }
            }
        }
        result.sort(this.comparator(this.sortMode));
        this.filteredEntries = List.copyOf(result);

        ScreenshotGridWidget currentGrid = this.grid;
        if (currentGrid != null) {
            currentGrid.setEntries(this.filteredEntries);
        }
    }

    private void updateSortButton() {
        if (this.sortButton != null) {
            this.sortButton.setMessage(this.sortModeMessage());
        }
    }

    @NotNull
    private Component sortModeMessage() {
        return Component.translatable("panoramica.browser.sort", Component.translatable(this.sortMode.labelKey()));
    }

    @NotNull
    private TexturedIconButton addFooterIconButton(
            int x,
            int y,
            @NotNull Component message,
            @NotNull Button.OnPress onPress,
            @NotNull Identifier icon
    ) {
        TexturedIconButton button = this.addRenderableWidget(new TexturedIconButton(message, onPress, icon));
        button.setPosition(x, y);
        return button;
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
            Component message = this.deleteSelectedMessage(selected);
            this.deleteSelectedButton.active = selected > 0;
            this.deleteSelectedButton.setMessage(message);
            this.deleteSelectedButton.setTooltip(Tooltip.create(message));
        }
        if (this.clearSelectionButton != null) {
            this.clearSelectionButton.active = selected > 0;
        }
    }

    @NotNull
    private Component deleteSelectedMessage(int selected) {
        Component message = Component.translatable("panoramica.browser.delete_selected", selected);
        return selected > 0 ? message.copy().withStyle(ChatFormatting.RED) : message;
    }

    @NotNull
    private Comparator<ScreenshotEntry> comparator(@NotNull Options.BrowserSortMode mode) {
        return switch (mode) {
            case NEWEST_FIRST -> this.newestFirstComparator();
            case OLDEST_FIRST -> this.oldestFirstComparator();
            case BY_TYPE -> Comparator.comparingInt((ScreenshotEntry entry) -> entry.isPanorama() ? 1 : 0)
                    .thenComparing(this.newestFirstComparator());
        };
    }

    @NotNull
    private Comparator<ScreenshotEntry> newestFirstComparator() {
        return Comparator.comparingLong(ScreenshotEntry::modifiedMillis).reversed().thenComparing(entry -> entry.path().toString());
    }

    @NotNull
    private Comparator<ScreenshotEntry> oldestFirstComparator() {
        return Comparator.comparingLong(ScreenshotEntry::modifiedMillis).thenComparing(entry -> entry.path().toString());
    }

}
