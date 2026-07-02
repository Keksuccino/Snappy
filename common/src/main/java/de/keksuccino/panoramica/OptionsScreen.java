package de.keksuccino.panoramica;

import de.keksuccino.panoramica.menu.PanoramaMenuManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OptionsScreen extends Screen {

    protected static final int BUTTON_HEIGHT = 20;
    protected static final int BUTTON_ROW_MAX_WIDTH = 360;

    @Nullable
    protected Screen parent;
    @Nullable
    private Button cycleIntervalButton;

    public OptionsScreen(@Nullable Screen parent) {
        super(Component.translatable("panoramica.options"));
        this.parent = parent;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;
        int topY = 50;
        int spacing = 26;

        StringWidget titleWidget = this.addRenderableWidget(new StringWidget(this.getTitle(), this.font));
        titleWidget.setX(centerX - (titleWidget.getWidth() / 2));
        titleWidget.setY(20);

        int currentY = topY;

        this.addButtonRow(currentY, this.buildResolutionButton());
        currentY += spacing;

        this.addButtonRow(currentY, this.buildMenuModeButton());
        currentY += spacing;

        this.cycleIntervalButton = this.buildCycleIntervalButton();
        this.addButtonRow(currentY, this.cycleIntervalButton);
        this.updateCycleIntervalButton();
        currentY += spacing;

        this.addButtonRow(currentY, this.buildStorageLocationButton());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(centerX - 75, this.height - 40, 150, BUTTON_HEIGHT).build());

    }

    @NotNull
    protected Button buildResolutionButton() {
        return Button.builder(this.resolutionMessage(), button -> {
                    Options options = Panoramica.getOptions();
                    options.setScreenshotResolution(options.getScreenshotResolution().next());
                    button.setMessage(this.resolutionMessage());
                    button.setTooltip(this.resolutionTooltip());
                }).bounds(0, 0, this.getButtonWidth(), BUTTON_HEIGHT)
                .tooltip(this.resolutionTooltip()).build();
    }

    @NotNull
    protected Button buildMenuModeButton() {
        return Button.builder(this.menuModeMessage(), button -> {
                    Options options = Panoramica.getOptions();
                    options.setMenuPanoramaMode(options.getMenuPanoramaMode().next());
                    button.setMessage(this.menuModeMessage());
                    this.updateCycleIntervalButton();
                    PanoramaMenuManager.invalidate();
                }).bounds(0, 0, this.getButtonWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("panoramica.options.menu_mode.desc"))).build();
    }

    @NotNull
    protected Button buildCycleIntervalButton() {
        return Button.builder(this.cycleIntervalMessage(), button -> {
                    Options options = Panoramica.getOptions();
                    options.setCycleInterval(options.getCycleInterval().next());
                    button.setMessage(this.cycleIntervalMessage());
                    PanoramaMenuManager.invalidate();
                }).bounds(0, 0, this.getButtonWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("panoramica.options.cycle_interval.desc"))).build();
    }

    @NotNull
    protected Button buildStorageLocationButton() {
        return Button.builder(this.storageLocationMessage(), button -> {
                    Options options = Panoramica.getOptions();
                    options.setStorageLocation(options.getStorageLocation().next());
                    button.setMessage(this.storageLocationMessage());
                }).bounds(0, 0, this.getButtonWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("panoramica.options.storage.desc"))).build();
    }

    protected void updateCycleIntervalButton() {
        if (this.cycleIntervalButton != null) {
            boolean active = Panoramica.getOptions().getMenuPanoramaMode() == Options.MenuPanoramaMode.CYCLE_ALL;
            this.cycleIntervalButton.active = active;
            this.cycleIntervalButton.setMessage(this.cycleIntervalMessage());
            this.cycleIntervalButton.setTooltip(Tooltip.create(Component.translatable(active
                    ? "panoramica.options.cycle_interval.desc"
                    : "panoramica.options.cycle_interval.inactive_desc")));
        }
    }

    @NotNull
    protected Component resolutionMessage() {
        Options.ResolutionPreset preset = Panoramica.getOptions().getScreenshotResolution();
        return this.optionMessage("panoramica.options.resolution", Component.translatable(preset.labelKey()));
    }

    @NotNull
    protected Tooltip resolutionTooltip() {
        Options.ResolutionPreset preset = Panoramica.getOptions().getScreenshotResolution();
        return Tooltip.create(Component.translatable(preset.tooltipKey()));
    }

    @NotNull
    protected Component menuModeMessage() {
        return this.optionMessage("panoramica.options.menu_mode", Component.translatable(Panoramica.getOptions().getMenuPanoramaMode().labelKey()));
    }

    @NotNull
    protected Component cycleIntervalMessage() {
        Component value = Component.translatable(Panoramica.getOptions().getCycleInterval().labelKey());
        if (Panoramica.getOptions().getMenuPanoramaMode() != Options.MenuPanoramaMode.CYCLE_ALL) {
            value = value.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        }
        return this.optionMessage("panoramica.options.cycle_interval", value);
    }

    @NotNull
    protected Component storageLocationMessage() {
        return this.optionMessage("panoramica.options.storage", Component.translatable(Panoramica.getOptions().getStorageLocation().labelKey()));
    }

    @NotNull
    protected Component optionMessage(@NotNull String labelKey, @NotNull Component value) {
        return Component.translatable(labelKey, value);
    }

    protected void addButtonRow(int y, @NotNull Button button) {
        int buttonWidth = this.getButtonWidth();
        button.setPosition((this.width / 2) - (buttonWidth / 2), y);
        this.addRenderableWidget(button);
    }

    protected int getButtonWidth() {
        return Math.min(BUTTON_ROW_MAX_WIDTH, this.width - 40);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

}
