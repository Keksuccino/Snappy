package de.keksuccino.panoramica.screen;

import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager.PlayerInfo;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager.ScreenshotMetadata;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager.TimeInfo;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager.WorldInfo;
import de.keksuccino.panoramica.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScreenshotMetadataScreen extends Screen {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final long TICKS_PER_DAY = 24_000L;
    private static final int SIDE_MARGIN = 44;
    private static final int TOP_MARGIN = 48;
    private static final int BOTTOM_MARGIN = 44;
    private static final int PANEL_PADDING = 12;
    private static final int SECTION_GAP = 8;
    private static final int SECTION_PADDING = 10;
    private static final int SECTION_TITLE_HEIGHT = 13;
    private static final int ROW_HEIGHT = 13;
    private static final int SECTION_BACKGROUND_COLOR = 0x66000000;
    private static final int SECTION_BORDER_COLOR = 0xFF707070;
    private static final int SCROLL_STEP = 24;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BACK_BUTTON_WIDTH = 72;

    private final Screen parent;
    private final ScreenshotEntry entry;
    private int scrollOffset;
    private int contentHeight;

    public ScreenshotMetadataScreen(@NotNull Screen parent, @NotNull ScreenshotEntry entry) {
        super(Component.translatable("panoramica.metadata.title"));
        this.parent = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
                .bounds(this.width / 2 - BACK_BUTTON_WIDTH / 2, this.height - 28, BACK_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.renderHeader(graphics);
        this.renderPanel(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.isOverPanel(x, y) && this.maxScroll() > 0) {
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
        graphics.centeredText(this.font, Component.literal(this.entry.displayName()), this.width / 2, 27, 0xFFB0B0B0);
    }

    private void renderPanel(@NotNull GuiGraphicsExtractor graphics) {
        int panelX = this.panelX();
        int panelY = this.panelY();
        int panelWidth = this.panelWidth();
        int panelHeight = this.panelHeight();
        graphics.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF707070);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);

        ScreenshotMetadata metadata = ScreenshotMetadataManager.find(this.entry.path()).orElse(null);
        if (metadata == null) {
            Component message = Component.translatable("panoramica.metadata.missing");
            graphics.centeredText(this.font, message, panelX + panelWidth / 2, panelY + panelHeight / 2 - 4, 0xFFB0B0B0);
            this.contentHeight = panelHeight;
            this.scrollOffset = 0;
            return;
        }

        int contentX = panelX + PANEL_PADDING;
        int contentWidth = panelWidth - PANEL_PADDING * 2 - (this.maxScroll() > 0 ? 8 : 0);
        int contentY = panelY + PANEL_PADDING - this.scrollOffset;
        int y = contentY;

        graphics.enableScissor(panelX, panelY, panelX + panelWidth, panelY + panelHeight);
        try {
            y = this.renderSection(graphics, contentX, y, contentWidth, Component.translatable("panoramica.metadata.section.image"), this.imageRows(metadata));
            y = this.renderSection(graphics, contentX, y, contentWidth, Component.translatable("panoramica.metadata.section.world"), this.worldRows(metadata));
            y = this.renderSection(graphics, contentX, y, contentWidth, Component.translatable("panoramica.metadata.section.time"), this.timeRows(metadata.time()));
            y = this.renderSection(graphics, contentX, y, contentWidth, Component.translatable("panoramica.metadata.section.player"), this.playerRows(metadata.player()));
        } finally {
            graphics.disableScissor();
        }

        this.contentHeight = Math.max(panelHeight, y - contentY + PANEL_PADDING);
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
        graphics.fill(x, y, x + width, y + sectionHeight, SECTION_BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 1, y + sectionHeight - 1, SECTION_BACKGROUND_COLOR);
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
        rows.add(row("panoramica.metadata.type", Component.translatable(metadata.kind().labelKey())));
        rows.add(row("panoramica.metadata.resolution", this.formatResolution(metadata)));
        rows.add(row("panoramica.metadata.hud", Component.translatable(metadata.hudHidden()
                ? "panoramica.metadata.hud.hidden"
                : "panoramica.metadata.hud.visible")));
        rows.add(row("panoramica.metadata.captured", this.formatDate(metadata.capturedAtEpochMillis(), metadata.capturedAtIso())));
        rows.add(row("panoramica.metadata.file_name", metadata.fileName()));
        rows.add(row("panoramica.metadata.saved_at", metadata.path()));
        return rows;
    }

    @NotNull
    private List<MetadataRow> worldRows(@NotNull ScreenshotMetadata metadata) {
        WorldInfo world = metadata.world();
        List<MetadataRow> rows = new ArrayList<>();
        rows.add(row("panoramica.metadata.world_name", this.orUnknown(world.name())));
        rows.add(row("panoramica.metadata.world_type", this.formatIdentifier(world.type())));
        rows.add(row("panoramica.metadata.world_identifier", this.orUnknown(world.identifier())));
        if (!world.savePath().isBlank()) {
            rows.add(row("panoramica.metadata.world_save", world.savePath()));
        }
        rows.add(row("panoramica.metadata.dimension", this.orUnknown(world.dimension())));
        rows.add(row("panoramica.metadata.biome", this.orUnknown(metadata.player().biome())));
        rows.add(row("panoramica.metadata.difficulty", this.formatIdentifier(metadata.difficulty())));
        rows.add(row("panoramica.metadata.render_distance", Component.translatable("panoramica.metadata.render_distance.value", metadata.renderDistanceChunks())));
        return rows;
    }

    @NotNull
    private List<MetadataRow> timeRows(@NotNull TimeInfo time) {
        List<MetadataRow> rows = new ArrayList<>();
        rows.add(row("panoramica.metadata.game_time", Component.translatable("panoramica.metadata.ticks", time.gameTime())));
        rows.add(row("panoramica.metadata.clock_time", Component.translatable("panoramica.metadata.ticks", time.clockTime())));
        rows.add(row("panoramica.metadata.time_of_day", this.formatTimeOfDay(time.timeOfDay())));
        rows.add(row("panoramica.metadata.world_day", String.valueOf(time.worldDay())));
        return rows;
    }

    @NotNull
    private List<MetadataRow> playerRows(@NotNull PlayerInfo player) {
        List<MetadataRow> rows = new ArrayList<>();
        rows.add(row("panoramica.metadata.position", this.formatPosition(player.x(), player.y(), player.z())));
        rows.add(row("panoramica.metadata.block_position", this.formatBlockPosition(player.blockX(), player.blockY(), player.blockZ())));
        rows.add(row("panoramica.metadata.game_mode", this.formatIdentifier(player.gameMode())));
        rows.add(row("panoramica.metadata.facing", String.format(Locale.ROOT, "%.1f / %.1f", player.yaw(), player.pitch())));
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
            return Component.translatable("panoramica.metadata.resolution.panorama", metadata.width(), metadata.height()).getString();
        }
        return Component.translatable("panoramica.metadata.resolution.normal", metadata.width(), metadata.height()).getString();
    }

    @NotNull
    private String formatDate(long epochMillis, @NotNull String fallbackIso) {
        if (epochMillis > 0L) {
            return DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis));
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
        return Component.translatable("panoramica.metadata.time_of_day.value", wrappedTicks, String.format(Locale.ROOT, "%02d:%02d", hour, minute)).getString();
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
        return value.isBlank() ? Component.translatable("panoramica.metadata.unknown").getString() : value;
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
        int trackX = panelX + panelWidth - 5;
        int trackHeight = panelHeight - 4;
        int thumbHeight = Mth.clamp(panelHeight * panelHeight / Math.max(panelHeight, this.contentHeight), 18, trackHeight);
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = panelY + 2 + Math.round(thumbTravel * (this.scrollOffset / (float) maxScroll));

        graphics.fill(trackX, panelY + 2, trackX + 2, panelY + panelHeight - 2, 0x66404040);
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
        return Math.max(0, this.contentHeight - this.panelHeight());
    }

    private boolean isOverPanel(double x, double y) {
        return x >= this.panelX()
                && x < this.panelX() + this.panelWidth()
                && y >= this.panelY()
                && y < this.panelY() + this.panelHeight();
    }

    private record MetadataRow(@NotNull Component label, @NotNull String value) {
    }

}
