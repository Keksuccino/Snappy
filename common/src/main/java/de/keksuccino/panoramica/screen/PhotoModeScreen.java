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
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public class PhotoModeScreen extends Screen {

    private static final Identifier GENERAL_ICON = PanoramicaButtons.SCREENSHOT_BROWSER_ICON;
    private static final Identifier PLAYER_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/player_head_icon_15x15.png");
    private static final Identifier LENS_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/lens_icon_15x15.png");
    private static final Identifier GLOBE_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/globe_icon_15x15.png");
    private static final int PANEL_WIDTH = 236;
    private static final int PANEL_PADDING = 8;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_GAP = 5;
    private static final int TAB_GAP = 4;
    private static final int SCREEN_MARGIN = 12;
    private static final int ACTION_GAP = 4;
    private static final int ACTION_ROW_GAP = 5;
    private static final int COLOR_PICKER_GAP = 6;
    private static final int ACTION_BUTTON_TEXT_PADDING = 16;
    private static final int ACTION_TAKE_PHOTO_MIN_WIDTH = 76;
    private static final int ACTION_RETURN_TO_PLAYER_MIN_WIDTH = 116;
    private static final int ACTION_HIDE_GUI_MIN_WIDTH = 72;
    private static final int ACTION_RESET_MIN_WIDTH = 58;
    private static final int ACTION_LEAVE_MIN_WIDTH = 64;
    private static final int PANEL_BACKGROUND_COLOR = ARGB.color(174, 0, 0, 0);
    private static final int PANEL_ACCENT_COLOR = ARGB.color(255, 255, 209, 102);
    private static final int PANEL_BORDER_COLOR = ARGB.color(210, 116, 128, 142);
    private static final int SECTION_BACKGROUND_COLOR = ARGB.color(82, 24, 28, 34);
    private static final int GRID_LINE_COLOR = ARGB.color(112, 255, 255, 255);
    private static final int VALUE_COLOR = 0xFFFFAA00;
    private static final double FOV_SNAP_RADIUS = 2.0D;
    private static final double ROLL_SNAP_RADIUS = 5.0D;
    private static final double VIGNETTE_SNAP_RADIUS = 0.05D;
    private static final double PLAYER_POSITION_OFFSET_MIN = -5.0D;
    private static final double PLAYER_POSITION_OFFSET_MAX = 5.0D;
    private static final double PLAYER_POSITION_OFFSET_SNAP_RADIUS = 0.08D;
    private static final double PLAYER_POSITION_OFFSET_STEP = 0.01D;
    private static final double PLAYER_ROTATION_OFFSET_MIN = -180.0D;
    private static final double PLAYER_ROTATION_OFFSET_MAX = 180.0D;
    private static final double PLAYER_ROTATION_OFFSET_SNAP_RADIUS = 5.0D;
    private static final double FOG_INTENSITY_SNAP_RADIUS = 0.05D;
    private static final double FOG_DISTANCE_SNAP_RADIUS = 4.0D;
    private static final double FOG_DISTANCE_STEP = 1.0D;

    private Tab selectedTab = Tab.GENERAL;
    @Nullable
    private Confirmation confirmation;
    private boolean rotatingView;
    private int panelX;
    private int panelY;
    private int panelHeight;
    @Nullable
    private Button pauseButton;
    @Nullable
    private Button gridButton;
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
    @Nullable
    private PhotoModeColorButton fogColorButton;
    @Nullable
    private PhotoModeColorPicker colorPicker;
    private boolean fogColorPickerOpen;

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
        if (PhotoModeManager.shouldRenderPhotoModeGrid()) {
            this.renderGrid(graphics);
        }
        if (PhotoModeManager.shouldHidePhotoModeUi()) {
            return;
        }

        this.renderPanel(graphics);
        if (this.colorPicker != null) {
            this.colorPicker.extractRenderState(graphics, this.font, mouseX, mouseY);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        this.rotatingView = false;
        if (PhotoModeManager.isPhotoModeUiHidden()) {
            if (event.button() == 0) {
                this.clearFocus();
                this.rotatingView = true;
            }
            return true;
        }
        if (this.colorPicker != null && this.colorPicker.mouseClicked(event, doubleClick)) {
            this.clearFocus();
            this.updateButtonMessages();
            return true;
        }
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
        if (this.colorPicker != null && this.colorPicker.mouseDragged(event, dx, dy)) {
            this.updateButtonMessages();
            return true;
        }
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
        if (this.colorPicker != null && this.colorPicker.mouseReleased(event)) {
            this.updateButtonMessages();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!PhotoModeManager.isPhotoModeUiHidden() && super.mouseScrolled(x, y, scrollX, scrollY)) {
            return true;
        }
        PhotoModeManager.zoomFromScroll(Minecraft.getInstance(), scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        boolean configuredCameraControlKey = this.isConfiguredCameraControlKey(event);
        if (configuredCameraControlKey) {
            PhotoModeManager.setConfiguredCameraControlKeyState(event, true);
        }
        if (isHideGuiKey(event.key())) {
            this.setPhotoModeUiHidden(!PhotoModeManager.isPhotoModeUiHidden());
            return true;
        }
        if (isGridKey(event.key())) {
            this.toggleGrid();
            return true;
        }
        if (PhotoModeManager.isPhotoModeUiHidden()) {
            if (event.isEscape()) {
                this.setPhotoModeUiHidden(false);
            }
            return true;
        }
        if (event.isEscape() && this.fogColorPickerOpen) {
            this.fogColorPickerOpen = false;
            this.rebuildPhotoWidgets();
            return true;
        }
        if (event.isEscape()) {
            this.confirmation = Confirmation.LEAVE;
            this.rebuildPhotoWidgets();
            return true;
        }
        if (configuredCameraControlKey) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NotNull KeyEvent event) {
        if (this.isConfiguredCameraControlKey(event)) {
            PhotoModeManager.setConfiguredCameraControlKeyState(event, false);
        }
        return super.keyReleased(event);
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
        this.clearControlReferences();
        this.updatePanelBounds();
        if (PhotoModeManager.isPhotoModeUiHidden()) {
            return;
        }
        if (this.confirmation != null) {
            this.fogColorPickerOpen = false;
            this.addConfirmationWidgets();
            return;
        }

        int x = this.panelX + PANEL_PADDING;
        int y = this.panelY + PANEL_PADDING;
        for (Tab tab : Tab.values()) {
            TexturedIconButton button = this.addRenderableWidget(new TexturedIconButton(tab.message(), ignored -> {
                this.selectedTab = tab;
                this.fogColorPickerOpen = false;
                this.rebuildPhotoWidgets();
            }, tab.icon()));
            button.setPosition(x, y);
            button.setTooltip(Tooltip.create(tab.message()));
            x += TexturedIconButton.DEFAULT_BUTTON_SIZE + TAB_GAP;
        }

        y += TexturedIconButton.DEFAULT_BUTTON_SIZE + CONTROL_GAP + 2;
        switch (this.selectedTab) {
            case GENERAL -> this.addGeneralControls(y);
            case PLAYER -> this.addPlayerControls(y);
            case EFFECTS -> this.addEffectsControls(y);
            case ENVIRONMENT -> this.addEnvironmentControls(y);
        }
        this.addActionButtons();
        this.updateButtonMessages();
    }

    private void clearControlReferences() {
        this.pauseButton = null;
        this.gridButton = null;
        this.hideSelfButton = null;
        this.hideOthersButton = null;
        this.poseButton = null;
        this.timeButton = null;
        this.weatherButton = null;
        this.fogColorButton = null;
        this.colorPicker = null;
    }

    private void addGeneralControls(int y) {
        int x = this.panelX + PANEL_PADDING;
        int width = this.controlWidth();
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }

        this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                30.0D,
                110.0D,
                active.fieldOfView(),
                Minecraft.getInstance().options.fov().get().doubleValue(),
                FOV_SNAP_RADIUS,
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
                0.0D,
                ROLL_SNAP_RADIUS,
                value -> active.setRoll((float) value),
                value -> optionMessage("panoramica.photo_mode.roll", Component.translatable("panoramica.photo_mode.degrees", String.format(Locale.ROOT, "%.0f", value)).withStyle(Style.EMPTY.withColor(VALUE_COLOR)))
        ));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.gridButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.setGridEnabled(!active.gridEnabled());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.grid.desc"))).build());
    }

    private void addPlayerControls(int y) {
        int x = this.panelX + PANEL_PADDING;
        int width = this.controlWidth();
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }

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
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.addPlayerTransformSlider(
                x,
                y,
                width,
                PLAYER_POSITION_OFFSET_MIN,
                PLAYER_POSITION_OFFSET_MAX,
                active.selfPlayerPositionOffsetX(),
                PLAYER_POSITION_OFFSET_SNAP_RADIUS,
                PLAYER_POSITION_OFFSET_STEP,
                active::setSelfPlayerPositionOffsetX,
                value -> optionMessage("panoramica.photo_mode.player_offset_x", this.blockValue(value))
        );
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.addPlayerTransformSlider(
                x,
                y,
                width,
                PLAYER_POSITION_OFFSET_MIN,
                PLAYER_POSITION_OFFSET_MAX,
                active.selfPlayerPositionOffsetY(),
                PLAYER_POSITION_OFFSET_SNAP_RADIUS,
                PLAYER_POSITION_OFFSET_STEP,
                active::setSelfPlayerPositionOffsetY,
                value -> optionMessage("panoramica.photo_mode.player_offset_y", this.blockValue(value))
        );
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.addPlayerTransformSlider(
                x,
                y,
                width,
                PLAYER_POSITION_OFFSET_MIN,
                PLAYER_POSITION_OFFSET_MAX,
                active.selfPlayerPositionOffsetZ(),
                PLAYER_POSITION_OFFSET_SNAP_RADIUS,
                PLAYER_POSITION_OFFSET_STEP,
                active::setSelfPlayerPositionOffsetZ,
                value -> optionMessage("panoramica.photo_mode.player_offset_z", this.blockValue(value))
        );
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.addPlayerTransformSlider(
                x,
                y,
                width,
                PLAYER_ROTATION_OFFSET_MIN,
                PLAYER_ROTATION_OFFSET_MAX,
                active.selfPlayerRotationOffsetX(),
                PLAYER_ROTATION_OFFSET_SNAP_RADIUS,
                0.0D,
                active::setSelfPlayerRotationOffsetX,
                value -> optionMessage("panoramica.photo_mode.player_rotation_x", this.degreeValue(value))
        );
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.addPlayerTransformSlider(
                x,
                y,
                width,
                PLAYER_ROTATION_OFFSET_MIN,
                PLAYER_ROTATION_OFFSET_MAX,
                active.selfPlayerRotationOffsetY(),
                PLAYER_ROTATION_OFFSET_SNAP_RADIUS,
                0.0D,
                active::setSelfPlayerRotationOffsetY,
                value -> optionMessage("panoramica.photo_mode.player_rotation_y", this.degreeValue(value))
        );
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.addPlayerTransformSlider(
                x,
                y,
                width,
                PLAYER_ROTATION_OFFSET_MIN,
                PLAYER_ROTATION_OFFSET_MAX,
                active.selfPlayerRotationOffsetZ(),
                PLAYER_ROTATION_OFFSET_SNAP_RADIUS,
                0.0D,
                active::setSelfPlayerRotationOffsetZ,
                value -> optionMessage("panoramica.photo_mode.player_rotation_z", this.degreeValue(value))
        );
    }

    private void addEffectsControls(int y) {
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
                0.0D,
                VIGNETTE_SNAP_RADIUS,
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

        this.pauseButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            PhotoModeManager.togglePaused(Minecraft.getInstance());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.pause.desc"))).build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.timeButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.setTimePreset(active.timePreset().next());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.time.desc"))).build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.weatherButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.setWeatherPreset(active.weatherPreset().next());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.weather.desc"))).build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                0.0D,
                1.0D,
                active.fogIntensity(),
                0.0D,
                FOG_INTENSITY_SNAP_RADIUS,
                value -> active.setFogIntensity((float) value),
                value -> optionMessage("panoramica.photo_mode.fog_intensity", Component.translatable("panoramica.photo_mode.percent", Math.round(value * 100.0D)).withStyle(Style.EMPTY.withColor(VALUE_COLOR)))
        ));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                PhotoModeManager.PHOTO_FOG_MIN_DISTANCE,
                PhotoModeManager.PHOTO_FOG_MAX_DISTANCE,
                active.fogDistance(),
                PhotoModeManager.PHOTO_FOG_DEFAULT_DISTANCE,
                FOG_DISTANCE_SNAP_RADIUS,
                FOG_DISTANCE_STEP,
                value -> active.setFogDistance((float) value),
                value -> optionMessage("panoramica.photo_mode.fog_distance", this.blockValue(value))
        ));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.fogColorButton = this.addRenderableWidget(new PhotoModeColorButton(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                Component.empty(),
                button -> {
                    this.fogColorPickerOpen = !this.fogColorPickerOpen;
                    this.rebuildPhotoWidgets();
                },
                active::fogColor
        ));
        if (this.fogColorPickerOpen) {
            this.colorPicker = new PhotoModeColorPicker(
                    Component.translatable("panoramica.photo_mode.fog_color_picker"),
                    active::fogColor,
                    active::setFogColor
            );
            this.updateColorPickerPosition();
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
                this.fogColorPickerOpen = false;
                this.rebuildPhotoWidgets();
            }).bounds(
                    this.colorPicker.doneButtonX(),
                    this.colorPicker.doneButtonY(),
                    this.colorPicker.doneButtonWidth(),
                    this.colorPicker.doneButtonHeight()
            ).build());
        }
    }

    private void addActionButtons() {
        Component takePhotoMessage = Component.translatable("panoramica.photo_mode.take_photo");
        Component returnToPlayerMessage = Component.translatable("panoramica.photo_mode.return_to_player");
        Component hideGuiMessage = Component.translatable("panoramica.photo_mode.hide_gui");
        Component resetMessage = Component.translatable("panoramica.photo_mode.reset");
        Component leaveMessage = Component.translatable("panoramica.photo_mode.leave_short");
        int rowWidth = this.actionRowWidth();
        int takePhotoWidth = this.actionButtonWidth(takePhotoMessage, ACTION_TAKE_PHOTO_MIN_WIDTH);
        int returnToPlayerWidth = this.actionButtonWidth(returnToPlayerMessage, ACTION_RETURN_TO_PLAYER_MIN_WIDTH);
        int hideGuiWidth = this.actionButtonWidth(hideGuiMessage, ACTION_HIDE_GUI_MIN_WIDTH);
        int resetWidth = this.actionButtonWidth(resetMessage, ACTION_RESET_MIN_WIDTH);
        int leaveWidth = this.actionButtonWidth(leaveMessage, ACTION_LEAVE_MIN_WIDTH);
        int preferredWidth = takePhotoWidth + returnToPlayerWidth + hideGuiWidth + resetWidth + leaveWidth + ACTION_GAP * 4;
        if (rowWidth < preferredWidth) {
            takePhotoWidth = Math.max(1, (rowWidth - ACTION_GAP * 4) / 5);
            returnToPlayerWidth = takePhotoWidth;
            hideGuiWidth = takePhotoWidth;
            resetWidth = takePhotoWidth;
            leaveWidth = rowWidth - takePhotoWidth * 4 - ACTION_GAP * 4;
        }

        int x = this.actionX();
        int y = this.actionY();
        this.addActionButton(takePhotoMessage, x, y, takePhotoWidth, button -> PhotoModeManager.requestScreenshot(Minecraft.getInstance()), takePhotoMessage);
        x += takePhotoWidth + ACTION_GAP;
        this.addActionButton(returnToPlayerMessage, x, y, returnToPlayerWidth, button -> PhotoModeManager.returnCameraToPlayer(Minecraft.getInstance()), Component.translatable("panoramica.photo_mode.return_to_player.desc"));
        x += returnToPlayerWidth + ACTION_GAP;
        this.addActionButton(hideGuiMessage, x, y, hideGuiWidth, button -> {
            this.fogColorPickerOpen = false;
            this.confirmation = Confirmation.HIDE_GUI;
            this.rebuildPhotoWidgets();
        }, Component.translatable("panoramica.photo_mode.hide_gui.desc"));
        x += hideGuiWidth + ACTION_GAP;
        this.addActionButton(resetMessage, x, y, resetWidth, button -> {
            this.fogColorPickerOpen = false;
            this.confirmation = Confirmation.RESET;
            this.rebuildPhotoWidgets();
        }, resetMessage);
        x += resetWidth + ACTION_GAP;
        this.addActionButton(leaveMessage, x, y, leaveWidth, button -> {
            this.fogColorPickerOpen = false;
            this.confirmation = Confirmation.LEAVE;
            this.rebuildPhotoWidgets();
        }, Component.translatable("panoramica.photo_mode.leave"));
    }

    private void addPlayerTransformSlider(
            int x,
            int y,
            int width,
            double minValue,
            double maxValue,
            double currentValue,
            double snapRadius,
            double actualStep,
            @NotNull DoubleConsumer valueConsumer,
            @NotNull DoubleFunction<Component> messageFactory
    ) {
        this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                minValue,
                maxValue,
                currentValue,
                0.0D,
                snapRadius,
                actualStep,
                valueConsumer,
                messageFactory
        ));
    }

    private void addActionButton(@NotNull Component message, int x, int y, int width, @NotNull Button.OnPress onPress, @NotNull Component tooltip) {
        this.addRenderableWidget(Button.builder(message, onPress)
                .bounds(x, y, width, CONTROL_HEIGHT)
                .tooltip(Tooltip.create(tooltip))
                .build());
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
            } else if (pending == Confirmation.HIDE_GUI) {
                this.setPhotoModeUiHidden(true);
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
        if (this.gridButton != null) {
            this.gridButton.setMessage(optionMessage("panoramica.photo_mode.grid", enabledValue(active.gridEnabled())));
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
        if (this.fogColorButton != null) {
            this.fogColorButton.setMessage(optionMessage("panoramica.photo_mode.fog_color", Component.literal(PhotoModeColorPicker.formatHexColor(active.fogColor())).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
    }

    private void renderGrid(@NotNull GuiGraphicsExtractor graphics) {
        for (int line = 1; line < 3; line++) {
            int x = Math.round(this.width * line / 3.0F);
            int y = Math.round(this.height * line / 3.0F);
            graphics.fill(x, 0, x + 1, this.height, GRID_LINE_COLOR);
            graphics.fill(0, y, this.width, y + 1, GRID_LINE_COLOR);
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

    private void updateColorPickerPosition() {
        if (this.colorPicker == null) {
            return;
        }
        int x = Math.max(SCREEN_MARGIN, this.panelX - PhotoModeColorPicker.WIDTH - COLOR_PICKER_GAP);
        int y = Math.max(SCREEN_MARGIN, Math.min(this.panelY, this.height - SCREEN_MARGIN - PhotoModeColorPicker.HEIGHT));
        this.colorPicker.setPosition(x, y);
    }

    private int actionY() {
        return this.panelY + this.panelHeight + ACTION_ROW_GAP;
    }

    private int actionX() {
        return Math.max(SCREEN_MARGIN, this.panelX + PANEL_WIDTH - this.actionRowWidth());
    }

    private int actionRowWidth() {
        int availableWidth = Math.max(PANEL_WIDTH, this.width - SCREEN_MARGIN * 2);
        return Math.min(this.preferredActionRowWidth(), availableWidth);
    }

    private int preferredActionRowWidth() {
        return this.actionButtonWidth(Component.translatable("panoramica.photo_mode.take_photo"), ACTION_TAKE_PHOTO_MIN_WIDTH)
                + this.actionButtonWidth(Component.translatable("panoramica.photo_mode.return_to_player"), ACTION_RETURN_TO_PLAYER_MIN_WIDTH)
                + this.actionButtonWidth(Component.translatable("panoramica.photo_mode.hide_gui"), ACTION_HIDE_GUI_MIN_WIDTH)
                + this.actionButtonWidth(Component.translatable("panoramica.photo_mode.reset"), ACTION_RESET_MIN_WIDTH)
                + this.actionButtonWidth(Component.translatable("panoramica.photo_mode.leave_short"), ACTION_LEAVE_MIN_WIDTH)
                + ACTION_GAP * 4;
    }

    private int actionButtonWidth(@NotNull Component message, int minWidth) {
        return Math.max(minWidth, this.font.width(message) + ACTION_BUTTON_TEXT_PADDING);
    }

    private int controlWidth() {
        return PANEL_WIDTH - PANEL_PADDING * 2;
    }

    private boolean isInsidePhotoModeUi(double mouseX, double mouseY) {
        if (PhotoModeManager.isPhotoModeUiHidden()) {
            return false;
        }
        boolean insidePanel = mouseX >= this.panelX && mouseX <= this.panelX + PANEL_WIDTH && mouseY >= this.panelY && mouseY <= this.panelY + this.panelHeight;
        if (insidePanel || this.confirmation != null) {
            return insidePanel;
        }
        if (this.colorPicker != null && this.colorPicker.contains(mouseX, mouseY)) {
            return true;
        }
        int actionY = this.actionY();
        int actionX = this.actionX();
        return mouseX >= actionX && mouseX <= actionX + this.actionRowWidth() && mouseY >= actionY && mouseY <= actionY + CONTROL_HEIGHT;
    }

    private boolean isConfiguredCameraControlKey(@NotNull KeyEvent event) {
        if (this.minecraft == null) {
            return false;
        }
        return this.minecraft.options.keyUp.matches(event)
                || this.minecraft.options.keyDown.matches(event)
                || this.minecraft.options.keyLeft.matches(event)
                || this.minecraft.options.keyRight.matches(event)
                || this.minecraft.options.keyJump.matches(event)
                || this.minecraft.options.keyShift.matches(event)
                || this.minecraft.options.keySprint.matches(event);
    }

    private static boolean isHideGuiKey(int key) {
        return key == GLFW.GLFW_KEY_H;
    }

    private static boolean isGridKey(int key) {
        return key == GLFW.GLFW_KEY_G;
    }

    private void setPhotoModeUiHidden(boolean hidden) {
        this.confirmation = null;
        this.rotatingView = false;
        if (hidden) {
            this.fogColorPickerOpen = false;
        }
        PhotoModeManager.setPhotoModeUiHidden(hidden);
        this.rebuildPhotoWidgets();
    }

    private void toggleGrid() {
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }
        active.setGridEnabled(!active.gridEnabled());
        this.updateButtonMessages();
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
    private static Component enabledValue(boolean enabled) {
        return Component.translatable(enabled ? "panoramica.photo_mode.enabled" : "panoramica.photo_mode.disabled")
                .withStyle(Style.EMPTY.withColor(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @NotNull
    private Component poseValue(@Nullable Identifier poseId) {
        if (poseId == null) {
            return Component.translatable("panoramica.photo_mode.pose.none").withStyle(Style.EMPTY.withColor(VALUE_COLOR));
        }
        PhotoPose pose = PhotoPoseManager.pose(poseId);
        return Component.translatable(pose == null ? "panoramica.photo_mode.pose.none" : pose.nameKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR));
    }

    @NotNull
    private Component blockValue(double value) {
        return Component.translatable("panoramica.photo_mode.blocks", String.format(Locale.ROOT, "%.2f", value))
                .withStyle(Style.EMPTY.withColor(VALUE_COLOR));
    }

    @NotNull
    private Component degreeValue(double value) {
        return Component.translatable("panoramica.photo_mode.degrees", String.format(Locale.ROOT, "%.0f", value))
                .withStyle(Style.EMPTY.withColor(VALUE_COLOR));
    }

    private enum Tab {
        GENERAL(GENERAL_ICON, "panoramica.photo_mode.tab.general", 113),
        PLAYER(PLAYER_ICON, "panoramica.photo_mode.tab.player", 263),
        EFFECTS(LENS_ICON, "panoramica.photo_mode.tab.effects", 63),
        ENVIRONMENT(GLOBE_ICON, "panoramica.photo_mode.tab.environment", 188);

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
        HIDE_GUI("panoramica.photo_mode.confirm.hide_gui.title", "panoramica.photo_mode.confirm.hide_gui.message", "panoramica.photo_mode.confirm.hide_gui.confirm"),
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
