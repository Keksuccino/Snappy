package de.keksuccino.panoramica.screen;

import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.photo.PhotoModeManager;
import de.keksuccino.panoramica.photo.PhotoModeTimePreset;
import de.keksuccino.panoramica.photo.PhotoModeWeatherPreset;
import de.keksuccino.panoramica.photo.PhotoPose;
import de.keksuccino.panoramica.photo.PhotoPoseManager;
import de.keksuccino.panoramica.util.rendering.RenderingUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class PhotoModeScreen extends Screen {

    private static final Identifier CAMERA_ICON = PanoramicaButtons.SCREENSHOT_BROWSER_ICON;
    private static final Identifier LENS_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/lens_icon_15x15.png");
    private static final Identifier CLOCK_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/clock_icon_15x15.png");
    private static final int PANEL_WIDTH = 236;
    private static final int PANEL_PADDING = 8;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_GAP = 5;
    private static final int TAB_GAP = 4;
    private static final int SCREEN_MARGIN = 12;
    private static final int ACTION_GAP = 4;
    private static final int ACTION_ROW_GAP = 5;
    private static final int PANEL_BACKGROUND_COLOR = ARGB.color(174, 0, 0, 0);
    private static final int PANEL_ACCENT_COLOR = ARGB.color(255, 255, 209, 102);
    private static final int PANEL_BORDER_COLOR = ARGB.color(210, 116, 128, 142);
    private static final int SECTION_BACKGROUND_COLOR = ARGB.color(82, 24, 28, 34);
    private static final int VALUE_COLOR = 0xFFFFAA00;

    private Tab selectedTab = Tab.CAMERA;
    @Nullable
    private Confirmation confirmation;
    private boolean rotatingView;
    private int panelX;
    private int panelY;
    private int panelHeight;
    @Nullable
    private Button pauseButton;
    @Nullable
    private Button hideSelfButton;
    @Nullable
    private Button hideOthersButton;
    @Nullable
    private Button poseButton;
    @Nullable
    private Button timeButton;
    @Nullable
    private Button weatherButton;

    public PhotoModeScreen() {
        super(Component.translatable("panoramica.photo_mode.title"));
    }

    @Override
    protected void init() {
        this.rebuildPhotoWidgets();
    }

    @Override
    public void tick() {
        if (this.minecraft != null) {
            if (!PhotoModeManager.isActive()) {
                this.minecraft.gui.setScreen(null);
                return;
            }
            PhotoModeManager.updateMovement(this.minecraft);
        }
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.minecraft != null) {
            PhotoModeManager.updateMovement(this.minecraft);
        }
        if (PhotoModeManager.shouldHidePhotoModeUi()) {
            return;
        }

        this.renderPanel(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        this.rotatingView = false;
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0 && !this.isInsidePhotoModeUi(event.x(), event.y())) {
            this.clearFocus();
            this.rotatingView = true;
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dx, double dy) {
        if (this.rotatingView && event.button() == 0) {
            PhotoModeManager.rotateFromMouseDrag(dx, dy);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (event.button() == 0) {
            this.rotatingView = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (event.isEscape()) {
            this.confirmation = Confirmation.LEAVE;
            this.rebuildPhotoWidgets();
            return true;
        }
        if (isCameraMovementKey(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return this.minecraft != null && PhotoModeManager.isPauseScreen(this.minecraft);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void removed() {
        PhotoModeManager.close();
    }

    private void rebuildPhotoWidgets() {
        this.clearWidgets();
        this.updatePanelBounds();
        if (this.confirmation != null) {
            this.addConfirmationWidgets();
            return;
        }

        int x = this.panelX + PANEL_PADDING;
        int y = this.panelY + PANEL_PADDING;
        for (Tab tab : Tab.values()) {
            TexturedIconButton button = this.addRenderableWidget(new TexturedIconButton(tab.message(), ignored -> {
                this.selectedTab = tab;
                this.rebuildPhotoWidgets();
            }, tab.icon()));
            button.setPosition(x, y);
            button.setTooltip(Tooltip.create(tab.message()));
            x += TexturedIconButton.DEFAULT_BUTTON_SIZE + TAB_GAP;
        }

        y += TexturedIconButton.DEFAULT_BUTTON_SIZE + CONTROL_GAP + 2;
        switch (this.selectedTab) {
            case CAMERA -> this.addCameraControls(y);
            case LENS -> this.addLensControls(y);
            case ENVIRONMENT -> this.addEnvironmentControls(y);
        }
        this.addActionButtons();
        this.updateButtonMessages();
    }

    private void addCameraControls(int y) {
        int x = this.panelX + PANEL_PADDING;
        int width = this.controlWidth();
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }

        this.pauseButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            PhotoModeManager.togglePaused(Minecraft.getInstance());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.pause.desc"))).build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                30.0D,
                110.0D,
                active.fieldOfView(),
                value -> active.setFieldOfView((float) value),
                value -> optionMessage("panoramica.photo_mode.fov", Component.literal(String.format(Locale.ROOT, "%.0f", value)).withStyle(Style.EMPTY.withColor(VALUE_COLOR)))
        ));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                -180.0D,
                180.0D,
                active.roll(),
                value -> active.setRoll((float) value),
                value -> optionMessage("panoramica.photo_mode.roll", Component.translatable("panoramica.photo_mode.degrees", String.format(Locale.ROOT, "%.0f", value)).withStyle(Style.EMPTY.withColor(VALUE_COLOR)))
        ));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.hideSelfButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.setHideSelfPlayer(!active.hideSelfPlayer());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.hideOthersButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.setHideOtherPlayers(!active.hideOtherPlayers());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.poseButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.cyclePose();
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.pose.desc"))).build());
    }

    private void addLensControls(int y) {
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }
        this.addRenderableWidget(new PhotoModeSlider(
                this.panelX + PANEL_PADDING,
                y,
                this.controlWidth(),
                CONTROL_HEIGHT,
                0.0D,
                1.0D,
                active.vignette(),
                value -> active.setVignette((float) value),
                value -> optionMessage("panoramica.photo_mode.vignette", Component.translatable("panoramica.photo_mode.percent", Math.round(value * 100.0D)).withStyle(Style.EMPTY.withColor(VALUE_COLOR)))
        ));
    }

    private void addEnvironmentControls(int y) {
        int x = this.panelX + PANEL_PADDING;
        int width = this.controlWidth();
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }

        this.timeButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.setTimePreset(active.timePreset().next());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.time.desc"))).build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.weatherButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.setWeatherPreset(active.weatherPreset().next());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.weather.desc"))).build());
    }

    private void addActionButtons() {
        int x = this.panelX;
        int y = this.actionY();
        int buttonWidth = (this.actionRowWidth() - ACTION_GAP * 2) / 3;
        this.addRenderableWidget(Button.builder(Component.translatable("panoramica.photo_mode.take_photo"), button -> PhotoModeManager.requestScreenshot(Minecraft.getInstance()))
                .bounds(x, y, buttonWidth, CONTROL_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.take_photo")))
                .build());
        x += buttonWidth + ACTION_GAP;
        this.addRenderableWidget(Button.builder(Component.translatable("panoramica.photo_mode.reset"), button -> {
            this.confirmation = Confirmation.RESET;
            this.rebuildPhotoWidgets();
        }).bounds(x, y, buttonWidth, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.reset"))).build());
        x += buttonWidth + ACTION_GAP;
        int leaveWidth = this.actionRowWidth() - buttonWidth * 2 - ACTION_GAP * 2;
        this.addRenderableWidget(Button.builder(Component.translatable("panoramica.photo_mode.leave_short"), button -> {
            this.confirmation = Confirmation.LEAVE;
            this.rebuildPhotoWidgets();
        }).bounds(x, y, leaveWidth, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.leave"))).build());
    }

    private void addConfirmationWidgets() {
        int buttonWidth = (this.controlWidth() - CONTROL_GAP) / 2;
        int y = this.panelY + this.panelHeight - PANEL_PADDING - CONTROL_HEIGHT;
        int x = this.panelX + PANEL_PADDING;
        this.addRenderableWidget(Button.builder(this.confirmation.confirmMessage(), button -> {
            Confirmation pending = this.confirmation;
            this.confirmation = null;
            if (pending == Confirmation.RESET) {
                PhotoModeManager.resetToDefaults(Minecraft.getInstance());
                this.rebuildPhotoWidgets();
            } else {
                PhotoModeManager.close();
                Minecraft.getInstance().gui.setScreen(null);
            }
        }).bounds(x, y, buttonWidth, CONTROL_HEIGHT).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.confirmation = null;
            this.rebuildPhotoWidgets();
        }).bounds(x + buttonWidth + CONTROL_GAP, y, this.controlWidth() - buttonWidth - CONTROL_GAP, CONTROL_HEIGHT).build());
    }

    private void updateButtonMessages() {
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }
        if (this.pauseButton != null) {
            boolean canPause = PhotoModeManager.canPause(Minecraft.getInstance());
            Component value = Component.translatable(active.paused() && canPause
                    ? "panoramica.photo_mode.pause.paused"
                    : "panoramica.photo_mode.pause.live").withStyle(active.paused() && canPause ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
            this.pauseButton.active = canPause;
            this.pauseButton.setMessage(optionMessage("panoramica.photo_mode.pause", value));
            this.pauseButton.setTooltip(Tooltip.create(Component.translatable(canPause
                    ? "panoramica.photo_mode.pause.desc"
                    : "panoramica.photo_mode.pause.unavailable_server")));
        }
        if (this.hideSelfButton != null) {
            this.hideSelfButton.setMessage(optionMessage("panoramica.photo_mode.hide_self", visibilityValue(!active.hideSelfPlayer())));
        }
        if (this.hideOthersButton != null) {
            this.hideOthersButton.setMessage(optionMessage("panoramica.photo_mode.hide_others", visibilityValue(!active.hideOtherPlayers())));
        }
        if (this.poseButton != null) {
            this.poseButton.setMessage(optionMessage("panoramica.photo_mode.pose", this.poseValue(active.poseId())));
        }
        if (this.timeButton != null) {
            PhotoModeTimePreset preset = active.timePreset();
            this.timeButton.setMessage(optionMessage("panoramica.photo_mode.time", Component.translatable(preset.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.weatherButton != null) {
            PhotoModeWeatherPreset preset = active.weatherPreset();
            this.weatherButton.setMessage(optionMessage("panoramica.photo_mode.weather", Component.translatable(preset.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
    }

    private void renderPanel(@NotNull GuiGraphicsExtractor graphics) {
        RenderingUtils.renderBorder(graphics, this.panelX - 1, this.panelY - 1, PANEL_WIDTH + 2, this.panelHeight + 2, 1, PANEL_BORDER_COLOR);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + this.panelHeight, PANEL_BACKGROUND_COLOR);

        if (this.confirmation != null) {
            int contentX = this.panelX + PANEL_PADDING;
            int contentY = this.panelY + PANEL_PADDING;
            int width = this.controlWidth();
            graphics.fill(contentX, contentY, contentX + width, this.panelY + this.panelHeight - PANEL_PADDING - CONTROL_HEIGHT - CONTROL_GAP, SECTION_BACKGROUND_COLOR);
            graphics.outline(contentX, contentY, width, this.panelHeight - PANEL_PADDING * 2 - CONTROL_HEIGHT - CONTROL_GAP, PANEL_BORDER_COLOR);
            graphics.centeredText(this.font, this.confirmation.title(), contentX + width / 2, contentY + 14, PANEL_ACCENT_COLOR);
            graphics.textWithWordWrap(this.font, this.confirmation.message(), contentX + 10, contentY + 36, width - 20, 0xFFFFFFFF);
            return;
        }

        int tabY = this.panelY + PANEL_PADDING;
        int tabX = this.panelX + PANEL_PADDING + this.selectedTab.ordinal() * (TexturedIconButton.DEFAULT_BUTTON_SIZE + TAB_GAP);
        graphics.outline(tabX - 1, tabY - 1, TexturedIconButton.DEFAULT_BUTTON_SIZE + 2, TexturedIconButton.DEFAULT_BUTTON_SIZE + 2, PANEL_ACCENT_COLOR);
    }

    private void updatePanelBounds() {
        int actionRowReserve = this.confirmation == null ? CONTROL_HEIGHT + ACTION_ROW_GAP : 0;
        int availablePanelHeight = Math.max(CONTROL_HEIGHT, this.height - SCREEN_MARGIN * 2 - actionRowReserve);
        this.panelHeight = Math.min(availablePanelHeight, this.confirmation != null ? 118 : this.selectedTab.panelHeight());
        this.panelX = Math.max(SCREEN_MARGIN, this.width - PANEL_WIDTH - SCREEN_MARGIN);
        this.panelY = Math.max(SCREEN_MARGIN, this.height - this.panelHeight - SCREEN_MARGIN - actionRowReserve);
    }

    private int actionY() {
        return this.panelY + this.panelHeight + ACTION_ROW_GAP;
    }

    private int actionRowWidth() {
        return PANEL_WIDTH;
    }

    private int controlWidth() {
        return PANEL_WIDTH - PANEL_PADDING * 2;
    }

    private boolean isInsidePhotoModeUi(double mouseX, double mouseY) {
        boolean insidePanel = mouseX >= this.panelX && mouseX <= this.panelX + PANEL_WIDTH && mouseY >= this.panelY && mouseY <= this.panelY + this.panelHeight;
        if (insidePanel || this.confirmation != null) {
            return insidePanel;
        }
        int actionY = this.actionY();
        return mouseX >= this.panelX && mouseX <= this.panelX + this.actionRowWidth() && mouseY >= actionY && mouseY <= actionY + CONTROL_HEIGHT;
    }

    private static boolean isCameraMovementKey(int key) {
        return key == GLFW.GLFW_KEY_W
                || key == GLFW.GLFW_KEY_A
                || key == GLFW.GLFW_KEY_S
                || key == GLFW.GLFW_KEY_D
                || key == GLFW.GLFW_KEY_UP
                || key == GLFW.GLFW_KEY_DOWN
                || key == GLFW.GLFW_KEY_LEFT
                || key == GLFW.GLFW_KEY_RIGHT;
    }

    @NotNull
    private static Component optionMessage(@NotNull String key, @NotNull Component value) {
        return Component.translatable(key, value);
    }

    @NotNull
    private static Component visibilityValue(boolean visible) {
        return Component.translatable(visible ? "panoramica.photo_mode.visible" : "panoramica.photo_mode.hidden")
                .withStyle(Style.EMPTY.withColor(visible ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @NotNull
    private Component poseValue(@Nullable Identifier poseId) {
        if (poseId == null) {
            return Component.translatable("panoramica.photo_mode.pose.none").withStyle(Style.EMPTY.withColor(VALUE_COLOR));
        }
        PhotoPose pose = PhotoPoseManager.pose(poseId);
        return Component.translatable(pose == null ? "panoramica.photo_mode.pose.none" : pose.nameKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR));
    }

    private enum Tab {
        CAMERA(CAMERA_ICON, "panoramica.photo_mode.tab.camera", 188),
        LENS(LENS_ICON, "panoramica.photo_mode.tab.lens", 63),
        ENVIRONMENT(CLOCK_ICON, "panoramica.photo_mode.tab.environment", 88);

        private final Identifier icon;
        private final String labelKey;
        private final int panelHeight;

        Tab(@NotNull Identifier icon, @NotNull String labelKey, int panelHeight) {
            this.icon = icon;
            this.labelKey = labelKey;
            this.panelHeight = panelHeight;
        }

        @NotNull
        private Identifier icon() {
            return this.icon;
        }

        @NotNull
        private Component message() {
            return Component.translatable(this.labelKey);
        }

        private int panelHeight() {
            return this.panelHeight;
        }
    }

    private enum Confirmation {
        RESET("panoramica.photo_mode.confirm.reset.title", "panoramica.photo_mode.confirm.reset.message", "panoramica.photo_mode.confirm.reset.confirm"),
        LEAVE("panoramica.photo_mode.confirm.leave.title", "panoramica.photo_mode.confirm.leave.message", "panoramica.photo_mode.confirm.leave.confirm");

        private final String titleKey;
        private final String messageKey;
        private final String confirmKey;

        Confirmation(@NotNull String titleKey, @NotNull String messageKey, @NotNull String confirmKey) {
            this.titleKey = titleKey;
            this.messageKey = messageKey;
            this.confirmKey = confirmKey;
        }

        @NotNull
        private Component title() {
            return Component.translatable(this.titleKey);
        }

        @NotNull
        private Component message() {
            return Component.translatable(this.messageKey);
        }

        @NotNull
        private Component confirmMessage() {
            return Component.translatable(this.confirmKey);
        }
    }

}
