package de.keksuccino.panoramica;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.panoramica.menu.PanoramaMenuManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OptionsScreen extends Screen {

    protected static final int BUTTON_HEIGHT = 20;
    protected static final int BUTTON_ROW_MAX_WIDTH = 360;
    protected static final int CYCLE_VALUE_COLOR = 0xFFAA00;
    protected static final int KEYBIND_RESET_BUTTON_WIDTH = 50;
    protected static final int KEYBIND_GAP = 5;
    protected static final int OPTION_ROW_ADVANCE = 26;
    protected static final Identifier TAB_HEADER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/tab_header_background.png");
    protected static final KeybindSetting PANORAMA_KEYBIND = new KeybindSetting(
            KeyMappings.KEY_TAKE_PANORAMA,
            "panoramica.options.keybind",
            "panoramica.options.keybind.desc"
    );
    protected static final KeybindSetting PHOTO_MODE_KEYBIND = new KeybindSetting(
            KeyMappings.KEY_OPEN_PHOTO_MODE,
            "panoramica.options.photo_mode_keybind",
            "panoramica.options.photo_mode_keybind.desc"
    );
    protected static final KeybindSetting PHOTO_MODE_GRID_KEYBIND = new KeybindSetting(
            KeyMappings.KEY_PHOTO_MODE_TOGGLE_GRID,
            "panoramica.options.photo_mode_grid_keybind",
            "panoramica.options.photo_mode_grid_keybind.desc"
    );
    protected static final KeybindSetting PHOTO_MODE_HIDE_UI_KEYBIND = new KeybindSetting(
            KeyMappings.KEY_PHOTO_MODE_HIDE_UI,
            "panoramica.options.photo_mode_hide_ui_keybind",
            "panoramica.options.photo_mode_hide_ui_keybind.desc"
    );
    protected static final KeybindSetting PHOTO_MODE_SLOW_CAMERA_KEYBIND = new KeybindSetting(
            KeyMappings.KEY_PHOTO_MODE_SLOW_CAMERA,
            "panoramica.options.photo_mode_slow_camera_keybind",
            "panoramica.options.photo_mode_slow_camera_keybind.desc"
    );
    protected static final List<KeybindSetting> KEYBIND_SETTINGS = List.of(
            PANORAMA_KEYBIND,
            PHOTO_MODE_KEYBIND,
            PHOTO_MODE_GRID_KEYBIND,
            PHOTO_MODE_HIDE_UI_KEYBIND,
            PHOTO_MODE_SLOW_CAMERA_KEYBIND
    );

    @Nullable
    protected Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    @Nullable
    private Button cycleIntervalButton;
    @Nullable
    private MenuTabBar tabNavigationBar;
    @Nullable
    private KeyMapping waitingForKeybind;
    private final List<Button> fullWidthOptionButtons = new ArrayList<>();
    private final List<KeybindControl> keybindControls = new ArrayList<>();

    public OptionsScreen(@Nullable Screen parent) {
        super(Component.translatable("panoramica.options"));
        this.parent = parent;
    }

    @Override
    protected void init() {

        this.layout.removeChildren();
        this.fullWidthOptionButtons.clear();
        this.keybindControls.clear();

        OptionsTab generalTab = this.buildGeneralTab();
        OptionsTab controlsTab = this.buildControlsTab();
        this.tabNavigationBar = MenuTabBar.builder(this.tabManager, this.width)
                .addTabs(generalTab, controlsTab)
                .build();
        this.addRenderableWidget(this.tabNavigationBar);

        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(150).build());
        this.layout.visitWidgets(widget -> {
            widget.setTabOrderGroup(1);
            this.addRenderableWidget(widget);
        });

        this.updateOptionButtonWidths();
        this.updateCycleIntervalButton();
        this.updateKeybindButtons();
        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();

    }

    @NotNull
    protected OptionsTab buildGeneralTab() {
        OptionsTab tab = new OptionsTab(Component.translatable("panoramica.options.tab.general"));
        this.addFullWidthOption(tab, this.buildResolutionButton());
        this.addFullWidthOption(tab, this.buildMenuModeButton());

        this.cycleIntervalButton = this.buildCycleIntervalButton();
        this.addFullWidthOption(tab, this.cycleIntervalButton);
        this.addFullWidthOption(tab, this.buildMenuParallaxButton());

        this.addFullWidthOption(tab, this.buildStorageLocationButton());
        this.addFullWidthOption(tab, this.buildHideHudInNormalScreenshotsButton());
        this.addFullWidthOption(tab, this.buildPreviewModeButton());
        this.addFullWidthOption(tab, this.buildScreenshotChatMessagesButton());
        this.addFullWidthOption(tab, this.buildScreenshotButtonsVisibilityButton(), settings -> settings.paddingTop(OPTION_ROW_ADVANCE));
        return tab;
    }

    @NotNull
    protected OptionsTab buildControlsTab() {
        OptionsTab tab = new OptionsTab(Component.translatable("panoramica.options.tab.controls"));
        for (KeybindSetting setting : KEYBIND_SETTINGS) {
            this.addKeybindRow(tab, setting);
        }
        return tab;
    }

    protected void addFullWidthOption(@NotNull OptionsTab tab, @NotNull Button button) {
        this.fullWidthOptionButtons.add(button);
        tab.addChild(button);
    }

    protected void addFullWidthOption(@NotNull OptionsTab tab, @NotNull Button button, @NotNull Consumer<LayoutSettings> settings) {
        this.fullWidthOptionButtons.add(button);
        tab.addChild(button, settings);
    }

    protected void addKeybindRow(@NotNull OptionsTab tab, @NotNull KeybindSetting setting) {
        Button keybindButton = this.buildKeybindButton(setting);
        Button keybindResetButton = this.buildKeybindResetButton(setting);
        this.keybindControls.add(new KeybindControl(setting, keybindButton, keybindResetButton));
        tab.addChild(this.buildKeybindRowLayout(keybindButton, keybindResetButton));
    }

    @NotNull
    protected Button buildKeybindButton(@NotNull KeybindSetting setting) {
        KeyMapping keyMapping = setting.keyMapping();
        return Button.builder(Component.empty(), button -> {
                    this.waitingForKeybind = keyMapping;
                    this.updateKeybindButtons();
                }).bounds(0, 0, this.getButtonWidth() - KEYBIND_RESET_BUTTON_WIDTH - KEYBIND_GAP, BUTTON_HEIGHT)
                .createNarration(defaultNarrationSupplier -> keyMapping.isUnbound()
                        ? Component.translatable("narrator.controls.unbound", Component.translatable(keyMapping.getName()))
                        : Component.translatable("narrator.controls.bound", Component.translatable(keyMapping.getName()), defaultNarrationSupplier.get()))
                .build();
    }

    @NotNull
    protected Button buildKeybindResetButton(@NotNull KeybindSetting setting) {
        KeyMapping keyMapping = setting.keyMapping();
        return Button.builder(Component.translatable("controls.reset"), button -> {
                    keyMapping.setKey(keyMapping.getDefaultKey());
                    this.afterKeybindChanged();
                }).bounds(0, 0, KEYBIND_RESET_BUTTON_WIDTH, BUTTON_HEIGHT)
                .createNarration(defaultNarrationSupplier -> Component.translatable("narrator.controls.reset", Component.translatable(keyMapping.getName())))
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
    protected Button buildMenuParallaxButton() {
        return Button.builder(this.menuParallaxMessage(), button -> {
                    Options options = Panoramica.getOptions();
                    options.setMenuPanoramaParallaxEnabled(!options.isMenuPanoramaParallaxEnabled());
                    button.setMessage(this.menuParallaxMessage());
                }).bounds(0, 0, this.getButtonWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("panoramica.options.menu_parallax.desc"))).build();
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

    @NotNull
    protected Button buildHideHudInNormalScreenshotsButton() {
        return Button.builder(this.hideHudInNormalScreenshotsMessage(), button -> {
                    Options options = Panoramica.getOptions();
                    options.setHideHudInNormalScreenshots(!options.shouldHideHudInNormalScreenshots());
                    button.setMessage(this.hideHudInNormalScreenshotsMessage());
                }).bounds(0, 0, this.getButtonWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("panoramica.options.hide_hud_normal_screenshots.desc"))).build();
    }

    @NotNull
    protected Button buildPreviewModeButton() {
        return Button.builder(this.previewModeMessage(), button -> {
                    Options options = Panoramica.getOptions();
                    options.setScreenshotPreviewMode(options.getScreenshotPreviewMode().next());
                    button.setMessage(this.previewModeMessage());
                }).bounds(0, 0, this.getButtonWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("panoramica.options.preview_mode.desc"))).build();
    }

    @NotNull
    protected Button buildScreenshotChatMessagesButton() {
        return Button.builder(this.screenshotChatMessagesMessage(), button -> {
                    Options options = Panoramica.getOptions();
                    options.setScreenshotChatMessagesEnabled(!options.areScreenshotChatMessagesEnabled());
                    button.setMessage(this.screenshotChatMessagesMessage());
                }).bounds(0, 0, this.getButtonWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("panoramica.options.screenshot_chat_messages.desc"))).build();
    }

    @NotNull
    protected Button buildScreenshotButtonsVisibilityButton() {
        return Button.builder(this.screenshotButtonsVisibilityMessage(), button -> this.toggleScreenshotButtonsVisibility(button))
                .bounds(0, 0, this.getButtonWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("panoramica.options.screenshot_buttons_visibility.desc"))).build();
    }

    protected void updateCycleIntervalButton() {
        if (this.cycleIntervalButton != null) {
            boolean active = Panoramica.getOptions().getMenuPanoramaMode().usesCycleInterval();
            this.cycleIntervalButton.active = active;
            this.cycleIntervalButton.setMessage(this.cycleIntervalMessage());
            this.cycleIntervalButton.setTooltip(Tooltip.create(Component.translatable(active
                    ? "panoramica.options.cycle_interval.desc"
                    : "panoramica.options.cycle_interval.inactive_desc")));
        }
    }

    protected void updateKeybindButtons() {
        for (KeybindControl control : this.keybindControls) {
            KeyMapping keyMapping = control.setting().keyMapping();
            control.keybindButton().setMessage(this.keybindMessage(control.setting()));
            control.keybindButton().setTooltip(this.keybindTooltip(control.setting()));
            control.resetButton().active = !keyMapping.isDefault();
        }
    }

    protected void updateOptionButtonWidths() {
        int rowWidth = this.getButtonWidth();
        for (Button button : this.fullWidthOptionButtons) {
            button.setWidth(rowWidth);
        }
        for (KeybindControl control : this.keybindControls) {
            control.keybindButton().setWidth(rowWidth - KEYBIND_RESET_BUTTON_WIDTH - KEYBIND_GAP);
            control.resetButton().setWidth(KEYBIND_RESET_BUTTON_WIDTH);
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
    protected Component menuParallaxMessage() {
        return this.optionMessage("panoramica.options.menu_parallax", this.booleanCycleValue(Panoramica.getOptions().isMenuPanoramaParallaxEnabled()));
    }

    @NotNull
    protected Component cycleIntervalMessage() {
        Component value = Component.translatable(Panoramica.getOptions().getCycleInterval().labelKey());
        if (!Panoramica.getOptions().getMenuPanoramaMode().usesCycleInterval()) {
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
    protected Component hideHudInNormalScreenshotsMessage() {
        return this.optionMessage("panoramica.options.hide_hud_normal_screenshots", this.booleanCycleValue(Panoramica.getOptions().shouldHideHudInNormalScreenshots()));
    }

    @NotNull
    protected Component previewModeMessage() {
        return this.optionMessage("panoramica.options.preview_mode", this.genericCycleValue(Component.translatable(Panoramica.getOptions().getScreenshotPreviewMode().labelKey())));
    }

    @NotNull
    protected Component screenshotChatMessagesMessage() {
        return this.optionMessage("panoramica.options.screenshot_chat_messages", this.booleanCycleValue(Panoramica.getOptions().areScreenshotChatMessagesEnabled()));
    }

    @NotNull
    protected Component screenshotButtonsVisibilityMessage() {
        boolean hidden = Panoramica.getOptions().areScreenshotButtonsHidden();
        return Component.translatable(hidden
                        ? "panoramica.options.screenshot_buttons_visibility.show"
                        : "panoramica.options.screenshot_buttons_visibility.hide")
                .withStyle(hidden ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @NotNull
    protected Component keybindMessage(@NotNull KeybindSetting setting) {
        KeyMapping keyMapping = setting.keyMapping();
        Component value = this.keybindValue(keyMapping);
        Component message = this.optionMessage(setting.labelKey(), value);
        if (this.waitingForKeybind == keyMapping) {
            return Component.literal("> ").append(message.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE)).append(" <").withStyle(ChatFormatting.YELLOW);
        }
        if (this.hasKeybindCollision(keyMapping)) {
            return Component.literal("[ ").append(message.copy().withStyle(ChatFormatting.WHITE)).append(" ]").withStyle(ChatFormatting.YELLOW);
        }
        return message;
    }

    @NotNull
    protected Component keybindValue(@NotNull KeyMapping keyMapping) {
        return keyMapping.getTranslatedKeyMessage().copy().withStyle(Style.EMPTY.withColor(CYCLE_VALUE_COLOR));
    }

    @Nullable
    protected Tooltip keybindTooltip(@NotNull KeybindSetting setting) {
        KeyMapping keyMapping = setting.keyMapping();
        if (!this.hasKeybindCollision(keyMapping)) {
            return Tooltip.create(Component.translatable(setting.descriptionKey()));
        }

        MutableComponent collisions = Component.empty();
        boolean first = true;
        if (this.minecraft != null) {
            for (KeyMapping otherKey : this.minecraft.options.keyMappings) {
                if (otherKey != keyMapping && keyMapping.same(otherKey)
                        && (!otherKey.isDefault() || !keyMapping.isDefault())) {
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

    @NotNull
    protected LinearLayout buildKeybindRowLayout(@NotNull Button keyButton, @NotNull Button resetButton) {
        int rowWidth = this.getButtonWidth();
        keyButton.setWidth(rowWidth - resetButton.getWidth() - KEYBIND_GAP);
        LinearLayout row = LinearLayout.horizontal().spacing(KEYBIND_GAP);
        row.addChild(keyButton);
        row.addChild(resetButton);
        return row;
    }

    protected int getButtonWidth() {
        return Math.min(BUTTON_ROW_MAX_WIDTH, this.width - 40);
    }

    protected boolean hasKeybindCollision(@NotNull KeyMapping keyMapping) {
        if (this.minecraft == null || keyMapping.isUnbound()) {
            return false;
        }

        for (KeyMapping otherKey : this.minecraft.options.keyMappings) {
            if (otherKey != keyMapping && keyMapping.same(otherKey)
                    && (!otherKey.isDefault() || !keyMapping.isDefault())) {
                return true;
            }
        }

        return false;
    }

    protected void toggleScreenshotButtonsVisibility(@NotNull Button button) {
        Options options = Panoramica.getOptions();
        if (options.areScreenshotButtonsHidden()) {
            options.setScreenshotButtonsHidden(false);
            button.setMessage(this.screenshotButtonsVisibilityMessage());
            return;
        }

        this.minecraft.gui.setScreen(new ConfirmScreen(result -> {
            if (result) {
                Panoramica.getOptions().setScreenshotButtonsHidden(true);
                button.setMessage(this.screenshotButtonsVisibilityMessage());
            }
            this.minecraft.gui.setScreen(this);
        }, Component.translatable("panoramica.options.screenshot_buttons_visibility.confirm.title"),
                Component.translatable("panoramica.options.screenshot_buttons_visibility.confirm.message"),
                Component.translatable("panoramica.options.screenshot_buttons_visibility.confirm.hide"),
                CommonComponents.GUI_CANCEL));
    }

    protected void afterKeybindChanged() {
        this.waitingForKeybind = null;
        KeyMapping.resetMapping();
        if (this.minecraft != null) {
            this.minecraft.options.save();
        }
        this.updateKeybindButtons();
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (this.waitingForKeybind != null) {
            this.waitingForKeybind.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            this.afterKeybindChanged();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (this.waitingForKeybind != null) {
            this.waitingForKeybind.setKey(event.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(event));
            this.afterKeybindChanged();
            return true;
        }
        if (this.tabNavigationBar != null && this.tabNavigationBar.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void repositionElements() {
        if (this.tabNavigationBar == null) {
            return;
        }

        this.tabNavigationBar.arrangeElements(this.width);
        int tabAreaTop = this.tabNavigationBar.getRectangle().bottom();
        ScreenRectangle tabArea = new ScreenRectangle(0, tabAreaTop, this.width, Math.max(0, this.height - this.layout.getFooterHeight() - tabAreaTop));
        this.tabManager.setTabArea(tabArea);
        this.layout.setHeaderHeight(tabAreaTop);
        this.layout.arrangeElements();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight(), 0.0F, 0.0F, this.width, 2, 32, 2);
    }

    @Override
    protected void extractMenuBackground(@NotNull GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.extractMenuBackground(graphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    protected record KeybindSetting(@NotNull KeyMapping keyMapping, @NotNull String labelKey, @NotNull String descriptionKey) {
    }

    private record KeybindControl(@NotNull KeybindSetting setting, @NotNull Button keybindButton, @NotNull Button resetButton) {
    }

    protected class OptionsTab implements Tab {

        private final Component title;
        private final LinearLayout optionsLayout;
        private final ScrollableLayout scrollableLayout;

        protected OptionsTab(@NotNull Component title) {
            this.title = title;
            this.optionsLayout = LinearLayout.vertical().spacing(OPTION_ROW_ADVANCE - BUTTON_HEIGHT);
            this.optionsLayout.defaultCellSetting().alignHorizontallyCenter();
            this.scrollableLayout = new ScrollableLayout(OptionsScreen.this.minecraft, this.optionsLayout, BUTTON_HEIGHT, ScrollableLayout.ReserveStrategy.BOTH);
            this.scrollableLayout.setScrollbarSpacing(2);
        }

        protected void addChild(@NotNull LayoutElement child) {
            this.optionsLayout.addChild(child);
        }

        protected void addChild(@NotNull LayoutElement child, @NotNull Consumer<LayoutSettings> settings) {
            this.optionsLayout.addChild(child, settings);
        }

        @Override
        public Component getTabTitle() {
            return this.title;
        }

        @Override
        public Component getTabExtraNarration() {
            return Component.empty();
        }

        @Override
        public void visitChildren(@NotNull Consumer<AbstractWidget> childrenConsumer) {
            // ScrollableLayout caches its content widgets, so refresh before TabManager registers it.
            this.scrollableLayout.arrangeElements();
            this.scrollableLayout.visitWidgets(childrenConsumer);
        }

        @Override
        public void doLayout(@NotNull ScreenRectangle screenRectangle) {
            OptionsScreen.this.updateOptionButtonWidths();
            int topY = Math.max(screenRectangle.top() + 4, Math.min(50, screenRectangle.bottom() - BUTTON_HEIGHT));
            this.scrollableLayout.setMinWidth(OptionsScreen.this.getButtonWidth());
            this.scrollableLayout.setMaxHeight(Math.max(BUTTON_HEIGHT, screenRectangle.bottom() - topY));
            this.scrollableLayout.arrangeElements();
            this.scrollableLayout.setPosition((OptionsScreen.this.width - this.scrollableLayout.getWidth()) / 2, topY);
        }

        @Override
        public Layout getLayout() {
            return this.scrollableLayout;
        }

    }

}
