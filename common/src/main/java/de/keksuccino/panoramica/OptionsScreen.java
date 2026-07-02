package de.keksuccino.panoramica;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.panoramica.menu.PanoramaMenuManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OptionsScreen extends Screen {

    protected static final int BUTTON_HEIGHT = 20;
    protected static final int BUTTON_ROW_MAX_WIDTH = 360;
    protected static final int CYCLE_VALUE_COLOR = 0xFFAA00;
    protected static final int KEYBIND_RESET_BUTTON_WIDTH = 50;
    protected static final int KEYBIND_GAP = 5;

    @Nullable
    protected Screen parent;
    @Nullable
    private Button cycleIntervalButton;
    @Nullable
    private Button keybindButton;
    @Nullable
    private Button keybindResetButton;
    private boolean waitingForPanoramaKey;

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

        this.keybindButton = this.buildKeybindButton();
        this.keybindResetButton = this.buildKeybindResetButton();
        this.addKeybindRow(currentY, this.keybindButton, this.keybindResetButton);
        this.updateKeybindButtons();
        currentY += spacing;

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
    protected Button buildKeybindButton() {
        return Button.builder(Component.empty(), button -> {
                    this.waitingForPanoramaKey = true;
                    this.updateKeybindButtons();
                }).bounds(0, 0, this.getButtonWidth() - KEYBIND_RESET_BUTTON_WIDTH - KEYBIND_GAP, BUTTON_HEIGHT)
                .createNarration(defaultNarrationSupplier -> KeyMappings.KEY_TAKE_PANORAMA.isUnbound()
                        ? Component.translatable("narrator.controls.unbound", Component.translatable(KeyMappings.KEY_TAKE_PANORAMA.getName()))
                        : Component.translatable("narrator.controls.bound", Component.translatable(KeyMappings.KEY_TAKE_PANORAMA.getName()), defaultNarrationSupplier.get()))
                .build();
    }

    @NotNull
    protected Button buildKeybindResetButton() {
        return Button.builder(Component.translatable("controls.reset"), button -> {
                    KeyMappings.KEY_TAKE_PANORAMA.setKey(KeyMappings.KEY_TAKE_PANORAMA.getDefaultKey());
                    this.afterKeybindChanged();
                }).bounds(0, 0, KEYBIND_RESET_BUTTON_WIDTH, BUTTON_HEIGHT)
                .createNarration(defaultNarrationSupplier -> Component.translatable("narrator.controls.reset", Component.translatable(KeyMappings.KEY_TAKE_PANORAMA.getName())))
                .build();
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

    protected void updateKeybindButtons() {
        if (this.keybindButton != null) {
            this.keybindButton.setMessage(this.keybindMessage());
            this.keybindButton.setTooltip(this.keybindTooltip());
        }
        if (this.keybindResetButton != null) {
            this.keybindResetButton.active = !KeyMappings.KEY_TAKE_PANORAMA.isDefault();
        }
    }

    @NotNull
    protected Component resolutionMessage() {
        Options.ResolutionPreset preset = Panoramica.getOptions().getScreenshotResolution();
        return this.optionMessage("panoramica.options.resolution", this.genericCycleValue(Component.translatable(preset.labelKey())));
    }

    @NotNull
    protected Tooltip resolutionTooltip() {
        Options.ResolutionPreset preset = Panoramica.getOptions().getScreenshotResolution();
        return Tooltip.create(Component.translatable(preset.tooltipKey()));
    }

    @NotNull
    protected Component menuModeMessage() {
        return this.optionMessage("panoramica.options.menu_mode", this.genericCycleValue(Component.translatable(Panoramica.getOptions().getMenuPanoramaMode().labelKey())));
    }

    @NotNull
    protected Component cycleIntervalMessage() {
        Component value = Component.translatable(Panoramica.getOptions().getCycleInterval().labelKey());
        if (Panoramica.getOptions().getMenuPanoramaMode() != Options.MenuPanoramaMode.CYCLE_ALL) {
            value = value.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        } else {
            value = this.genericCycleValue(value);
        }
        return this.optionMessage("panoramica.options.cycle_interval", value);
    }

    @NotNull
    protected Component storageLocationMessage() {
        return this.optionMessage("panoramica.options.storage", this.genericCycleValue(Component.translatable(Panoramica.getOptions().getStorageLocation().labelKey())));
    }

    @NotNull
    protected Component keybindMessage() {
        Component value = this.keybindValue();
        Component message = this.optionMessage("panoramica.options.keybind", value);
        if (this.waitingForPanoramaKey) {
            return Component.literal("> ").append(message.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE)).append(" <").withStyle(ChatFormatting.YELLOW);
        }
        if (this.hasKeybindCollision()) {
            return Component.literal("[ ").append(message.copy().withStyle(ChatFormatting.WHITE)).append(" ]").withStyle(ChatFormatting.YELLOW);
        }
        return message;
    }

    @NotNull
    protected Component keybindValue() {
        return KeyMappings.KEY_TAKE_PANORAMA.getTranslatedKeyMessage().copy().withStyle(Style.EMPTY.withColor(CYCLE_VALUE_COLOR));
    }

    @Nullable
    protected Tooltip keybindTooltip() {
        if (!this.hasKeybindCollision()) {
            return Tooltip.create(Component.translatable("panoramica.options.keybind.desc"));
        }

        MutableComponent collisions = Component.empty();
        boolean first = true;
        if (this.minecraft != null) {
            for (KeyMapping otherKey : this.minecraft.options.keyMappings) {
                if (otherKey != KeyMappings.KEY_TAKE_PANORAMA && KeyMappings.KEY_TAKE_PANORAMA.same(otherKey)
                        && (!otherKey.isDefault() || !KeyMappings.KEY_TAKE_PANORAMA.isDefault())) {
                    if (!first) {
                        collisions.append(", ");
                    }
                    collisions.append(Component.translatable(otherKey.getName()));
                    first = false;
                }
            }
        }

        return Tooltip.create(Component.translatable("panoramica.options.keybind.duplicate_desc", collisions));
    }

    @NotNull
    protected Component optionMessage(@NotNull String labelKey, @NotNull Component value) {
        return Component.translatable(labelKey, value);
    }

    @NotNull
    protected Component genericCycleValue(@NotNull Component value) {
        return value.copy().withStyle(Style.EMPTY.withColor(CYCLE_VALUE_COLOR));
    }

    @NotNull
    protected Component booleanCycleValue(boolean enabled) {
        return Component.translatable(enabled ? "panoramica.options.toggle.enabled" : "panoramica.options.toggle.disabled")
                .withStyle(Style.EMPTY.withColor(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    protected void addButtonRow(int y, @NotNull Button button) {
        int buttonWidth = this.getButtonWidth();
        button.setPosition((this.width / 2) - (buttonWidth / 2), y);
        this.addRenderableWidget(button);
    }

    protected void addKeybindRow(int y, @NotNull Button keyButton, @NotNull Button resetButton) {
        int rowWidth = this.getButtonWidth();
        int leftX = (this.width / 2) - (rowWidth / 2);
        int resetX = leftX + rowWidth - resetButton.getWidth();
        keyButton.setWidth(rowWidth - resetButton.getWidth() - KEYBIND_GAP);
        keyButton.setPosition(resetX - KEYBIND_GAP - keyButton.getWidth(), y);
        resetButton.setPosition(resetX, y);
        this.addRenderableWidget(keyButton);
        this.addRenderableWidget(resetButton);
    }

    protected int getButtonWidth() {
        return Math.min(BUTTON_ROW_MAX_WIDTH, this.width - 40);
    }

    protected boolean hasKeybindCollision() {
        if (this.minecraft == null || KeyMappings.KEY_TAKE_PANORAMA.isUnbound()) {
            return false;
        }

        for (KeyMapping otherKey : this.minecraft.options.keyMappings) {
            if (otherKey != KeyMappings.KEY_TAKE_PANORAMA && KeyMappings.KEY_TAKE_PANORAMA.same(otherKey)
                    && (!otherKey.isDefault() || !KeyMappings.KEY_TAKE_PANORAMA.isDefault())) {
                return true;
            }
        }

        return false;
    }

    protected void afterKeybindChanged() {
        this.waitingForPanoramaKey = false;
        KeyMapping.resetMapping();
        if (this.minecraft != null) {
            this.minecraft.options.save();
        }
        this.updateKeybindButtons();
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (this.waitingForPanoramaKey) {
            KeyMappings.KEY_TAKE_PANORAMA.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            this.afterKeybindChanged();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (this.waitingForPanoramaKey) {
            KeyMappings.KEY_TAKE_PANORAMA.setKey(event.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(event));
            this.afterKeybindChanged();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

}
