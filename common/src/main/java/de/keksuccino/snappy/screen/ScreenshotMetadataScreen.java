package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.client.gui.GuiBackground;
import de.keksuccino.snappy.metadata.ScreenshotMetadataManager;
import de.keksuccino.snappy.metadata.ScreenshotMetadataManager.PlayerInfo;
import de.keksuccino.snappy.metadata.ScreenshotMetadataManager.ScreenshotMetadata;
import de.keksuccino.snappy.metadata.ScreenshotMetadataManager.TimeInfo;
import de.keksuccino.snappy.metadata.ScreenshotMetadataManager.WorldInfo;
import de.keksuccino.snappy.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import de.keksuccino.snappy.util.rendering.RenderingUtils;
import de.keksuccino.snappy.util.rendering.gui.widget.SnappyButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScreenshotMetadataScreen extends Screen {

    private static final long TICKS_PER_DAY = 24_000L;
    private static final int SIDE_MARGIN = 44;
    private static final int TOP_MARGIN = 48;
    private static final int BOTTOM_MARGIN = 44;
    private static final int PANEL_PADDING = 12;
    private static final int SECTION_GAP = 8;
    private static final int SECTION_PADDING = 10;
    private static final int SECTION_TITLE_HEIGHT = 13;
    private static final int ROW_HEIGHT = 13;
    private static final int SECTION_BACKGROUND_COLOR = ARGB.color(102, 0, 0, 0);
    private static final int SECTION_BORDER_SIZE = 1;
    private static final int SECTION_BORDER_COLOR = ARGB.color(255, 112, 112, 112);
    private static final int SCROLL_STEP = 24;
    private static final int SCROLL_AREA_BACKGROUND_GAP = 4;
    private static final int SCROLL_AREA_LEFT_INSET = GuiBackground.DEFAULT.leftBorder() + SCROLL_AREA_BACKGROUND_GAP;
    private static final int SCROLL_AREA_TOP_INSET = GuiBackground.DEFAULT.topBorder() + SCROLL_AREA_BACKGROUND_GAP;
    private static final int SCROLL_AREA_RIGHT_INSET = GuiBackground.DEFAULT.rightBorder() + SCROLL_AREA_BACKGROUND_GAP;
    private static final int SCROLL_AREA_BOTTOM_INSET = GuiBackground.DEFAULT.bottomBorder() + SCROLL_AREA_BACKGROUND_GAP;
    private static final int SCROLLBAR_EDGE_INSET = 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BACK_BUTTON_WIDTH = 72;

    private final Screen parent;
    private final ScreenshotEntry entry;
    private int scrollOffset;
    private int contentHeight;

    public ScreenshotMetadataScreen(@NotNull Screen parent, @NotNull ScreenshotEntry entry) {
        super(Component.translatable("snappy.metadata.title"));
        this.parent = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(new SnappyButton(this.width / 2 - BACK_BUTTON_WIDTH / 2, this.height - 28, BACK_BUTTON_WIDTH, BUTTON_HEIGHT, CommonComponents.GUI_BACK, ignored -> this.onClose()));
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.renderHeader(graphics);
        this.renderPanel(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.isOverScrollArea(x, y) && this.maxScroll() > 0) {
            this.scrollOffset = Mth.clamp(this.scrollOffset - (int) Math.round(scrollY * SCROLL_STEP), 0, this.maxScroll());
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private void renderHeader(@NotNull GuiGraphicsExtractor graphics) {
        graphics.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.literal(this.entry.fileName()), this.width / 2, 27, 0xFFB0B0B0);
    }

    private void renderPanel(@NotNull GuiGraphicsExtractor graphics) {
        int panelX = this.panelX();
        int panelY = this.panelY();
        int panelWidth = this.panelWidth();
        int panelHeight = this.panelHeight();
        GuiBackground.DEFAULT.render(graphics, panelX, panelY, panelWidth, panelHeight);

        ScreenshotMetadata metadata = ScreenshotMetadataManager.find(this.entry.path()).orElse(null);
        if (metadata == null) {
            Component message = Component.translatable("snappy.metadata.missing");
            graphics.centeredText(this.font, message, panelX + panelWidth / 2, panelY + panelHeight / 2 - 4, 0xFFB0B0B0);
            this.contentHeight = this.scrollAreaHeight();
            this.scrollOffset = 0;
            return;
        }

        int contentX = panelX + PANEL_PADDING;
        int contentWidth = panelWidth - PANEL_PADDING * 2 - (this.maxScroll() > 0 ? 8 : 0);
        int contentY = panelY + PANEL_PADDING - this.scrollOffset;
        int y = contentY;

        graphics.enableScissor(panelX + SCROLL_AREA_LEFT_INSET, panelY + SCROLL_AREA_TOP_INSET, panelX + panelWidth - SCROLL_AREA_RIGHT_INSET, panelY + panelHeight - SCROLL_AREA_BOTTOM_INSET);
        try {
            y = this.renderSection(graphics, contentX, y, contentWidth, Component.translatable("snappy.metadata.section.image"), this.imageRows(metadata));
            y = this.renderSection(graphics, contentX, y, contentWidth, Component.translatable("snappy.metadata.section.world"), this.worldRows(metadata));
            y = this.renderSection(graphics, contentX, y, contentWidth, Component.translatable("snappy.metadata.section.time"), this.timeRows(metadata.time()));
            y = this.renderSection(graphics, contentX, y, contentWidth, Component.translatable("snappy.metadata.section.player"), this.playerRows(metadata.player()));
        } finally {
            graphics.disableScissor();
        }

        // Content begins four pixels inside the viewport, while renderSection returns eight pixels past the final section. Their difference leaves the matching four-pixel bottom space at maximum scroll.
        this.contentHeight = Math.max(this.scrollAreaHeight(), y - contentY);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll());
        this.renderScrollbar(graphics);
    }

    private int renderSection(
            @NotNull GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            @NotNull Component title,
            @NotNull List<MetadataRow> rows
    ) {
        int sectionHeight = SECTION_PADDING * 2 + SECTION_TITLE_HEIGHT + rows.size() * ROW_HEIGHT;
        RenderingUtils.renderBorder(graphics, x, y, width, sectionHeight, SECTION_BORDER_SIZE, SECTION_BORDER_COLOR);
        graphics.fill(x + SECTION_BORDER_SIZE, y + SECTION_BORDER_SIZE, x + width - SECTION_BORDER_SIZE, y + sectionHeight - SECTION_BORDER_SIZE, SECTION_BACKGROUND_COLOR);
        graphics.text(this.font, title, x + SECTION_PADDING, y + SECTION_PADDING, 0xFFFFD166);

        int labelWidth = Math.min(132, Math.max(84, width / 3));
        int rowY = y + SECTION_PADDING + SECTION_TITLE_HEIGHT;
        for (MetadataRow row : rows) {
            graphics.text(this.font, row.label(), x + SECTION_PADDING, rowY, 0xFF9FC7FF);
            int valueX = x + SECTION_PADDING + labelWidth;
            int valueWidth = Math.max(20, width - SECTION_PADDING * 2 - labelWidth);
            graphics.text(this.font, this.ellipsize(row.value(), valueWidth), valueX, rowY, 0xFFFFFFFF);
            rowY += ROW_HEIGHT;
        }

        return y + sectionHeight + SECTION_GAP;
    }

    @NotNull
    private List<MetadataRow> imageRows(@NotNull ScreenshotMetadata metadata) {
        List<MetadataRow> rows = new ArrayList<>();
        rows.add(row("snappy.metadata.type", Component.translatable(metadata.kind().labelKey())));
        rows.add(row("snappy.metadata.resolution", this.formatResolution(metadata)));
        rows.add(row("snappy.metadata.hud", Component.translatable(metadata.hudHidden()
                ? "snappy.metadata.hud.hidden"
                : "snappy.metadata.hud.visible")));
        rows.add(row("snappy.metadata.captured", this.formatDate(metadata.capturedAtEpochMillis(), metadata.capturedAtIso())));
        rows.add(row("snappy.metadata.file_name", metadata.fileName()));
        rows.add(row("snappy.metadata.saved_at", metadata.path()));
        return rows;
    }

    @NotNull
    private List<MetadataRow> worldRows(@NotNull ScreenshotMetadata metadata) {
        WorldInfo world = metadata.world();
        List<MetadataRow> rows = new ArrayList<>();
        rows.add(row("snappy.metadata.world_name", this.orUnknown(world.name())));
        rows.add(row("snappy.metadata.world_type", this.formatIdentifier(world.type())));
        rows.add(row("snappy.metadata.world_identifier", this.orUnknown(world.identifier())));
        if (!world.savePath().isBlank()) {
            rows.add(row("snappy.metadata.world_save", world.savePath()));
        }
        rows.add(row("snappy.metadata.dimension", this.orUnknown(world.dimension())));
        rows.add(row("snappy.metadata.biome", this.orUnknown(metadata.player().biome())));
        rows.add(row("snappy.metadata.difficulty", this.formatIdentifier(metadata.difficulty())));
        rows.add(row("snappy.metadata.render_distance", Component.translatable("snappy.metadata.render_distance.value", metadata.renderDistanceChunks())));
        return rows;
    }

    @NotNull
    private List<MetadataRow> timeRows(@NotNull TimeInfo time) {
        List<MetadataRow> rows = new ArrayList<>();
        rows.add(row("snappy.metadata.game_time", Component.translatable("snappy.metadata.ticks", time.gameTime())));
        rows.add(row("snappy.metadata.clock_time", Component.translatable("snappy.metadata.ticks", time.clockTime())));
        rows.add(row("snappy.metadata.time_of_day", this.formatTimeOfDay(time.timeOfDay())));
        rows.add(row("snappy.metadata.world_day", String.valueOf(time.worldDay())));
        return rows;
    }

    @NotNull
    private List<MetadataRow> playerRows(@NotNull PlayerInfo player) {
        List<MetadataRow> rows = new ArrayList<>();
        rows.add(row("snappy.metadata.position", this.formatPosition(player.x(), player.y(), player.z())));
        rows.add(row("snappy.metadata.block_position", this.formatBlockPosition(player.blockX(), player.blockY(), player.blockZ())));
        rows.add(row("snappy.metadata.game_mode", this.formatIdentifier(player.gameMode())));
        rows.add(row("snappy.metadata.facing", String.format(Locale.ROOT, "%.1f / %.1f", player.yaw(), player.pitch())));
        return rows;
    }

    @NotNull
    private MetadataRow row(@NotNull String labelKey, @NotNull Component value) {
        return new MetadataRow(Component.translatable(labelKey), value.getString());
    }

    @NotNull
    private MetadataRow row(@NotNull String labelKey, @NotNull String value) {
        return new MetadataRow(Component.translatable(labelKey), value);
    }

    @NotNull
    private String formatResolution(@NotNull ScreenshotMetadata metadata) {
        if (metadata.kind() == ScreenshotMetadataManager.ScreenshotKind.PANORAMA) {
            return Component.translatable("snappy.metadata.resolution.panorama", metadata.width(), metadata.height()).getString();
        }
        return Component.translatable("snappy.metadata.resolution.normal", metadata.width(), metadata.height()).getString();
    }

    @NotNull
    private String formatDate(long epochMillis, @NotNull String fallbackIso) {
        if (epochMillis > 0L) {
            return ScreenshotTimestampFormatter.format(epochMillis);
        }
        if (!fallbackIso.isBlank()) {
            try {
                return ScreenshotTimestampFormatter.format(Instant.parse(fallbackIso));
            } catch (DateTimeParseException ignored) {
                // Keep malformed legacy values visible rather than hiding potentially useful metadata.
            }
        }
        return this.orUnknown(fallbackIso);
    }

    @NotNull
    private String formatPosition(double x, double y, double z) {
        return String.format(Locale.ROOT, "X: %.2f, Y: %.2f, Z: %.2f", x, y, z);
    }

    @NotNull
    private String formatBlockPosition(int x, int y, int z) {
        return String.format(Locale.ROOT, "X: %d, Y: %d, Z: %d", x, y, z);
    }

    @NotNull
    private String formatTimeOfDay(long timeOfDayTicks) {
        long wrappedTicks = Math.floorMod(timeOfDayTicks, TICKS_PER_DAY);
        int totalMinutes = (int) (((wrappedTicks + 6_000L) % TICKS_PER_DAY) * 1_440L / TICKS_PER_DAY);
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return Component.translatable("snappy.metadata.time_of_day.value", wrappedTicks, String.format(Locale.ROOT, "%02d:%02d", hour, minute)).getString();
    }

    @NotNull
    private String formatIdentifier(@NotNull String value) {
        if (value.isBlank()) {
            return this.orUnknown(value);
        }

        String[] parts = value.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return result.toString();
    }

    @NotNull
    private String orUnknown(@NotNull String value) {
        return value.isBlank() ? Component.translatable("snappy.metadata.unknown").getString() : value;
    }

    @NotNull
    private String ellipsize(@NotNull String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = this.font.width(suffix);
        if (maxWidth <= suffixWidth) {
            return suffix;
        }
        return this.font.plainSubstrByWidth(text, maxWidth - suffixWidth) + suffix;
    }

    private void renderScrollbar(@NotNull GuiGraphicsExtractor graphics) {
        int maxScroll = this.maxScroll();
        if (maxScroll <= 0) {
            return;
        }

        int panelX = this.panelX();
        int panelY = this.panelY();
        int panelWidth = this.panelWidth();
        int panelHeight = this.panelHeight();
        int scrollAreaHeight = this.scrollAreaHeight();
        int trackX = panelX + panelWidth - SCROLL_AREA_RIGHT_INSET - 5;
        int trackY = panelY + SCROLL_AREA_TOP_INSET + SCROLLBAR_EDGE_INSET;
        int trackBottom = panelY + panelHeight - SCROLL_AREA_BOTTOM_INSET - SCROLLBAR_EDGE_INSET;
        int trackHeight = Math.max(1, trackBottom - trackY);
        int thumbHeight = Mth.clamp(trackHeight * scrollAreaHeight / Math.max(scrollAreaHeight, this.contentHeight), 18, trackHeight);
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = trackY + Math.round(thumbTravel * (this.scrollOffset / (float) maxScroll));

        graphics.fill(trackX, trackY, trackX + 2, trackBottom, 0x66404040);
        graphics.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xCCFFFFFF);
    }

    private int panelX() {
        return Math.min(SIDE_MARGIN, Math.max(12, this.width / 12));
    }

    private int panelY() {
        return TOP_MARGIN;
    }

    private int panelWidth() {
        return Math.max(120, this.width - this.panelX() * 2);
    }

    private int panelHeight() {
        return Math.max(80, this.height - TOP_MARGIN - BOTTOM_MARGIN);
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - this.scrollAreaHeight());
    }

    private int scrollAreaHeight() {
        return Math.max(1, this.panelHeight() - SCROLL_AREA_TOP_INSET - SCROLL_AREA_BOTTOM_INSET);
    }

    private boolean isOverScrollArea(double x, double y) {
        return x >= this.panelX() + SCROLL_AREA_LEFT_INSET
                && x < this.panelX() + this.panelWidth() - SCROLL_AREA_RIGHT_INSET
                && y >= this.panelY() + SCROLL_AREA_TOP_INSET
                && y < this.panelY() + this.panelHeight() - SCROLL_AREA_BOTTOM_INSET;
    }

    private record MetadataRow(@NotNull Component label, @NotNull String value) {
    }

}
