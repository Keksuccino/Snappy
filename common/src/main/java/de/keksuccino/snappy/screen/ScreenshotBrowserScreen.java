package de.keksuccino.snappy.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.keksuccino.snappy.Options;
import de.keksuccino.snappy.OptionsScreen;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.menu.MenuBackgroundSelectionManager;
import de.keksuccino.snappy.menu.PanoramaMenuManager;
import de.keksuccino.snappy.screen.ScreenshotBrowserCatalog.DeletionResult;
import de.keksuccino.snappy.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import de.keksuccino.snappy.util.rendering.gui.widget.IconButton;
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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ScreenshotBrowserScreen extends Screen {

    private static final int HEADER_HEIGHT = 74;
    private static final int FOOTER_HEIGHT = 42;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SIDE_MARGIN = 20;
    private static final int BUTTON_GAP = 6;
    private static final int HEADER_CONTROL_Y = 42;
    private static final int SEARCH_WIDTH = 108;
    private static final int MIN_SEARCH_WIDTH = 80;
    private static final int STATUS_MESSAGE_MARGIN = 20;
    private static final long STATUS_MESSAGE_VISIBLE_MILLIS = 10_000L;
    private static final int STATUS_SUCCESS_COLOR = 0xFF78E878;
    private static final int STATUS_WARNING_COLOR = 0xFFFFD166;
    private static final Identifier BACK_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/shared/back_icon_15x15.png");
    private static final Identifier SETTINGS_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/browser/settings_icon_15x15.png");
    private static final Identifier SORT_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/browser/sort_icon_15x15.png");
    private static final Identifier FILTER_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/browser/filter_icon_15x15.png");
    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/browser/search_icon_15x15.png");
    private static final Identifier REFRESH_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/browser/refresh_icon_15x15.png");
    private static final Identifier SELECT_ALL_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/browser/select_all_icon_15x15.png");
    private static final Identifier CLEAR_SELECTION_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/browser/clear_selection_icon_15x15.png");
    private static final Identifier DELETE_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/shared/delete_icon_15x15.png");

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
    private Button filterButton;
    @Nullable
    private Button searchButton;
    @Nullable
    private EditBox searchBox;
    @Nullable
    private Button deleteSelectedButton;
    @Nullable
    private Button clearSelectionButton;
    private Options.BrowserSortMode sortMode = Options.BrowserSortMode.NEWEST_FIRST;
    private Options.BrowserFilterMode filterMode = Options.BrowserFilterMode.NONE;
    private boolean searchExpanded;
    private String searchQuery = "";
    private Component statusMessage = Component.empty();
    private int statusMessageColor = 0xFFFFFFFF;
    private long statusMessageExpiresAtMillis;

    public ScreenshotBrowserScreen(@Nullable Screen parent) {
        super(Component.translatable("snappy.browser.title"));
        this.parent = parent;
    }

    public static boolean openDetailViewer(@NotNull Minecraft minecraft, @NotNull Screen parent, @NotNull Path screenshotPath) {
        List<ScreenshotEntry> entries = sortedEntries(ScreenshotBrowserCatalog.scan(minecraft), Snappy.getOptions().getBrowserSortMode());
        int index = findEntryIndex(entries, screenshotPath);
        if (index < 0) {
            return false;
        }

        ScreenshotBrowserScreen browserScreen = new ScreenshotBrowserScreen(parent);
        minecraft.gui.setScreen(new ScreenshotViewerScreen(browserScreen, entries, index));
        return true;
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
        this.sortMode = Snappy.getOptions().getBrowserSortMode();
        this.filterMode = Snappy.getOptions().getBrowserFilterMode();

        int headerControlWidth = Math.max(120, this.width - SIDE_MARGIN * 2);
        int headerIconControlsWidth = IconButton.DEFAULT_BUTTON_SIZE * 3;
        int availableSearchWidth = Math.max(0, headerControlWidth - headerIconControlsWidth - BUTTON_GAP * 3);
        int searchWidth = this.headerSearchWidth(availableSearchWidth);
        this.settingsButton = this.addRenderableWidget(new IconButton(
                Component.translatable("snappy.browser.settings"),
                button -> Minecraft.getInstance().gui.setScreen(new OptionsScreen(this)),
                SETTINGS_ICON
        ));

        this.sortButton = this.addRenderableWidget(new IconButton(
                this.sortModeMessage(),
                button -> {
                    this.sortMode = this.sortMode.next();
                    Snappy.getOptions().setBrowserSortMode(this.sortMode);
                    this.updateSortButton();
                    this.applyFilter();
                },
                SORT_ICON
        ));

        this.filterButton = this.addRenderableWidget(new IconButton(
                this.filterModeMessage(),
                button -> {
                    this.filterMode = this.filterMode.next();
                    Snappy.getOptions().setBrowserFilterMode(this.filterMode);
                    this.updateFilterButton();
                    this.applyFilter();
                },
                FILTER_ICON
        ));

        this.searchButton = this.addRenderableWidget(new IconButton(
                Component.translatable("snappy.browser.search"),
                button -> this.expandSearch(),
                SEARCH_ICON
        ));

        this.searchBox = this.addRenderableWidget(new EditBox(this.font, 0, HEADER_CONTROL_Y, searchWidth, BUTTON_HEIGHT, Component.translatable("snappy.browser.search")));
        this.searchBox.setMaxLength(128);
        this.searchBox.setHint(Component.translatable("snappy.browser.search_hint"));
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(value -> {
            this.searchQuery = value;
            this.applyFilter();
        });
        this.updateSearchControls();

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
        int iconButtonWidth = IconButton.DEFAULT_BUTTON_SIZE;
        int totalFooterWidth = iconButtonWidth * 5 + BUTTON_GAP * 4;
        int footerX = centerX - totalFooterWidth / 2;

        this.addFooterIconButton(footerX, footerY, CommonComponents.GUI_BACK, button -> this.onClose(), BACK_ICON);
        footerX += iconButtonWidth + BUTTON_GAP;
        this.addFooterIconButton(footerX, footerY, Component.translatable("snappy.browser.refresh"), button -> this.refreshEntries(), REFRESH_ICON);
        footerX += iconButtonWidth + BUTTON_GAP;
        this.addFooterIconButton(footerX, footerY, Component.translatable("snappy.browser.select_all"), button -> {
            ScreenshotGridWidget currentGrid = this.grid;
            if (currentGrid != null) {
                currentGrid.selectAll();
            }
        }, SELECT_ALL_ICON);
        footerX += iconButtonWidth + BUTTON_GAP;
        this.clearSelectionButton = this.addFooterIconButton(footerX, footerY, Component.translatable("snappy.browser.clear_selection"), button -> {
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
        Component countText = Component.translatable("snappy.browser.count", this.filteredEntries.size());
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
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            Button currentSearchButton = this.searchButton;
            if (!this.searchExpanded && currentSearchButton != null && currentSearchButton.isMouseOver(event.x(), event.y())) {
                currentSearchButton.playDownSound(Minecraft.getInstance().getSoundManager());
                this.expandSearch();
                return true;
            }

            EditBox currentSearchBox = this.searchBox;
            if (this.searchExpanded && currentSearchBox != null && !currentSearchBox.isMouseOver(event.x(), event.y())) {
                boolean handled = super.mouseClicked(event, doubleClick);
                this.collapseSearch();
                return handled;
            }
        }

        return super.mouseClicked(event, doubleClick);
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
        if (this.searchExpanded && this.searchBox != null && !this.searchBox.isFocused()) {
            this.collapseSearch();
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
        Options.BrowserFilterMode activeFilter = this.filterMode;
        Set<Path> selectedMenuBackgrounds = activeFilter == Options.BrowserFilterMode.MENU_BACKGROUNDS
                ? new HashSet<>(MenuBackgroundSelectionManager.getSelectedPanoramaFolders())
                : Set.of();
        ZoneId todayZone = activeFilter == Options.BrowserFilterMode.TODAY ? ZoneId.systemDefault() : null;
        LocalDate today = todayZone == null ? null : LocalDate.now(todayZone);
        List<ScreenshotEntry> result = new ArrayList<>();
        for (ScreenshotEntry entry : this.allEntries) {
            if (this.matchesSearch(entry, query)
                    && this.matchesBrowserFilter(entry, activeFilter, selectedMenuBackgrounds, today, todayZone)) {
                result.add(entry);
            }
        }
        this.filteredEntries = sortedEntries(result, this.sortMode);

        ScreenshotGridWidget currentGrid = this.grid;
        if (currentGrid != null) {
            currentGrid.setEntries(this.filteredEntries);
        }
    }

    private boolean matchesSearch(@NotNull ScreenshotEntry entry, @NotNull String query) {
        if (query.isEmpty()) {
            return true;
        }

        String type = entry.isPanorama() ? "panorama" : "normal screenshot";
        return entry.lowerCaseDisplayName().contains(query)
                || entry.formattedDate().toLowerCase(Locale.ROOT).contains(query)
                || type.contains(query);
    }

    private boolean matchesBrowserFilter(
            @NotNull ScreenshotEntry entry,
            @NotNull Options.BrowserFilterMode filterMode,
            @NotNull Set<Path> selectedMenuBackgrounds,
            @Nullable LocalDate today,
            @Nullable ZoneId todayZone
    ) {
        return switch (filterMode) {
            case NONE -> true;
            case MENU_BACKGROUNDS -> entry.isPanorama() && selectedMenuBackgrounds.contains(entry.path());
            case ONLY_PANORAMAS -> entry.isPanorama();
            case ONLY_NORMAL_SCREENSHOTS -> !entry.isPanorama();
            case TODAY -> entry.isPanorama() && today != null && todayZone != null && this.isModifiedOnDate(entry, today, todayZone);
        };
    }

    private boolean isModifiedOnDate(@NotNull ScreenshotEntry entry, @NotNull LocalDate date, @NotNull ZoneId zone) {
        long modifiedMillis = entry.modifiedMillis();
        if (modifiedMillis <= 0L) {
            return false;
        }
        return Instant.ofEpochMilli(modifiedMillis).atZone(zone).toLocalDate().equals(date);
    }

    private void updateSortButton() {
        if (this.sortButton != null) {
            this.updateButtonMessageAndTooltip(this.sortButton, this.sortModeMessage());
        }
    }

    private void updateFilterButton() {
        if (this.filterButton != null) {
            this.updateButtonMessageAndTooltip(this.filterButton, this.filterModeMessage());
        }
    }

    private void updateButtonMessageAndTooltip(@NotNull Button button, @NotNull Component message) {
        button.setMessage(message);
        button.setTooltip(Tooltip.create(message));
    }

    private void expandSearch() {
        this.searchExpanded = true;
        this.updateSearchControls();
        EditBox currentSearchBox = this.searchBox;
        if (currentSearchBox != null) {
            this.setFocused(currentSearchBox);
            currentSearchBox.setFocused(true);
        }
    }

    private void collapseSearch() {
        this.searchExpanded = false;
        EditBox currentSearchBox = this.searchBox;
        if (currentSearchBox != null) {
            currentSearchBox.setFocused(false);
            if (this.getFocused() == currentSearchBox) {
                this.setFocused(null);
            }
        }
        this.updateSearchControls();
    }

    private void updateSearchControls() {
        this.updateHeaderControlLayout();

        Button currentSearchButton = this.searchButton;
        if (currentSearchButton != null) {
            currentSearchButton.visible = !this.searchExpanded;
            currentSearchButton.active = !this.searchExpanded;
        }

        EditBox currentSearchBox = this.searchBox;
        if (currentSearchBox != null) {
            currentSearchBox.visible = this.searchExpanded;
            currentSearchBox.active = this.searchExpanded;
        }
    }

    private void updateHeaderControlLayout() {
        int searchControlWidth = this.searchExpanded && this.searchBox != null
                ? this.searchBox.getWidth()
                : IconButton.DEFAULT_BUTTON_SIZE;
        int searchX = this.width - SIDE_MARGIN - searchControlWidth;

        if (this.searchButton != null) {
            this.searchButton.setPosition(searchX, HEADER_CONTROL_Y);
        }
        if (this.searchBox != null) {
            this.searchBox.setPosition(searchX, HEADER_CONTROL_Y);
        }

        int controlX = searchX - BUTTON_GAP;
        if (this.filterButton != null) {
            controlX -= this.filterButton.getWidth();
            this.filterButton.setPosition(controlX, HEADER_CONTROL_Y);
            controlX -= BUTTON_GAP;
        }
        if (this.sortButton != null) {
            controlX -= this.sortButton.getWidth();
            this.sortButton.setPosition(controlX, HEADER_CONTROL_Y);
            controlX -= BUTTON_GAP;
        }
        if (this.settingsButton != null) {
            controlX -= this.settingsButton.getWidth();
            this.settingsButton.setPosition(controlX, HEADER_CONTROL_Y);
        }
    }

    @NotNull
    private Component sortModeMessage() {
        return Component.translatable("snappy.browser.sort", Component.translatable(this.sortMode.labelKey()));
    }

    @NotNull
    private Component filterModeMessage() {
        return Component.translatable("snappy.browser.filter", Component.translatable(this.filterMode.labelKey()));
    }

    @NotNull
    private IconButton addFooterIconButton(
            int x,
            int y,
            @NotNull Component message,
            @NotNull Button.OnPress onPress,
            @NotNull Identifier icon
    ) {
        IconButton button = this.addRenderableWidget(new IconButton(message, onPress, icon));
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

        Component title = Component.translatable("snappy.browser.delete_confirm.title");
        Component message = entries.size() == 1
                ? Component.translatable("snappy.browser.delete_confirm.single")
                : Component.translatable("snappy.browser.delete_confirm.multiple", entries.size());
        this.minecraft.gui.setScreen(new ConfirmScreen(result -> {
            this.minecraft.gui.setScreen(this);
            if (result) {
                this.deleteEntries(entries);
            }
        }, title, message, Component.translatable("snappy.browser.delete"), CommonComponents.GUI_CANCEL));
    }

    private void deleteEntries(@NotNull List<ScreenshotEntry> entries) {
        DeletionResult result = ScreenshotBrowserCatalog.deleteAll(entries);
        PanoramaMenuManager.invalidate();
        this.refreshEntries();

        if (result.failed() > 0) {
            this.showStatusMessage(Component.translatable("snappy.browser.delete_result.partial", result.deleted(), result.failed()), STATUS_WARNING_COLOR);
        } else {
            this.showStatusMessage(result.deleted() == 1
                    ? Component.translatable("snappy.browser.delete_result.single")
                    : Component.translatable("snappy.browser.delete_result.multiple", result.deleted()), STATUS_SUCCESS_COLOR);
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
        Component message = Component.translatable("snappy.browser.delete_selected", selected);
        return selected > 0 ? message.copy().withStyle(ChatFormatting.RED) : message;
    }

    private int headerSearchWidth(int availableSearchWidth) {
        if (availableSearchWidth < MIN_SEARCH_WIDTH) {
            return Math.max(0, availableSearchWidth);
        }
        return Math.min(SEARCH_WIDTH, availableSearchWidth);
    }

    @NotNull
    private static Comparator<ScreenshotEntry> comparator(@NotNull Options.BrowserSortMode mode) {
        return switch (mode) {
            case NEWEST_FIRST -> newestFirstComparator();
            case OLDEST_FIRST -> oldestFirstComparator();
            case BY_TYPE -> Comparator.comparingInt((ScreenshotEntry entry) -> entry.isPanorama() ? 1 : 0)
                    .thenComparing(newestFirstComparator());
        };
    }

    @NotNull
    private static List<ScreenshotEntry> sortedEntries(@NotNull List<ScreenshotEntry> entries, @NotNull Options.BrowserSortMode sortMode) {
        List<ScreenshotEntry> sorted = new ArrayList<>(entries);
        sorted.sort(comparator(sortMode));
        return List.copyOf(sorted);
    }

    private static int findEntryIndex(@NotNull List<ScreenshotEntry> entries, @NotNull Path screenshotPath) {
        Path normalizedPath = screenshotPath.toAbsolutePath().normalize();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).path().equals(normalizedPath)) {
                return i;
            }
        }
        return -1;
    }

    @NotNull
    private static Comparator<ScreenshotEntry> newestFirstComparator() {
        return Comparator.comparingLong(ScreenshotEntry::modifiedMillis).reversed().thenComparing(entry -> entry.path().toString());
    }

    @NotNull
    private static Comparator<ScreenshotEntry> oldestFirstComparator() {
        return Comparator.comparingLong(ScreenshotEntry::modifiedMillis).thenComparing(entry -> entry.path().toString());
    }

}
