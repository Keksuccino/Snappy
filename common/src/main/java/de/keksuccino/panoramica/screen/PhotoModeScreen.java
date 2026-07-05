package de.keksuccino.panoramica.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.panoramica.KeyMappings;
import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.photo.PhotoModeColorizePreset;
import de.keksuccino.panoramica.photo.PhotoModeManager;
import de.keksuccino.panoramica.photo.PhotoModeStylizePreset;
import de.keksuccino.panoramica.photo.PhotoModeTimePreset;
import de.keksuccino.panoramica.photo.PhotoModeWeatherPreset;
import de.keksuccino.panoramica.photo.PhotoPose;
import de.keksuccino.panoramica.photo.PhotoPoseExporter;
import de.keksuccino.panoramica.photo.PhotoPoseManager;
import de.keksuccino.panoramica.util.rendering.RenderingUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class PhotoModeScreen extends Screen {

    private static final Identifier GENERAL_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/general_camera_icon_15x15.png");
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
    private static final int POSE_MAKER_MIN_COLUMN_WIDTH = 108;
    private static final int POSE_MAKER_MAX_COLUMN_WIDTH = 162;
    private static final int POSE_MAKER_COLUMN_GAP = 6;
    private static final int POSE_MAKER_MIN_COLUMNS = 2;
    private static final int POSE_MAKER_MAX_COLUMNS = 4;
    private static final int POSE_MAKER_HEADER_HEIGHT = 14;
    private static final int POSE_MAKER_NAME_LABEL_HEIGHT = 10;
    private static final int POSE_MAKER_BUTTON_GAP = 5;
    private static final int POSE_MAKER_SPACE_PRESS_COUNT = 5;
    private static final int PANEL_BACKGROUND_COLOR = ARGB.color(174, 0, 0, 0);
    private static final int PANEL_ACCENT_COLOR = ARGB.color(255, 255, 209, 102);
    private static final int PANEL_BORDER_COLOR = ARGB.color(210, 116, 128, 142);
    private static final int SECTION_BACKGROUND_COLOR = ARGB.color(82, 24, 28, 34);
    private static final int GRID_LINE_COLOR = ARGB.color(112, 255, 255, 255);
    private static final int VALUE_COLOR = 0xFFFFAA00;
    private static final int NO_HOVER_MOUSE_POSITION = -1;
    private static final double FOV_SNAP_RADIUS = 2.0D;
    private static final double ROLL_SNAP_RADIUS = 5.0D;
    private static final double VIGNETTE_SNAP_RADIUS = 0.05D;
    private static final double BLOOM_SNAP_RADIUS = 0.03D;
    private static final double BLOOM_STEP = 0.01D;
    private static final double COLOR_ADJUSTMENT_SNAP_RADIUS = 0.03D;
    private static final double COLOR_ADJUSTMENT_STEP = 0.01D;
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
    private static final double DEPTH_OF_FIELD_FOCUS_DISTANCE_SNAP_RADIUS = 0.05D;
    private static final double DEPTH_OF_FIELD_FOCUS_DISTANCE_STEP = 0.01D;
    private static final double DEPTH_OF_FIELD_FOCAL_LENGTH_SNAP_RADIUS = 1.0D;
    private static final double DEPTH_OF_FIELD_FOCAL_LENGTH_STEP = 1.0D;
    private static final double DEPTH_OF_FIELD_APERTURE_SNAP_RADIUS = 0.1D;
    private static final double DEPTH_OF_FIELD_APERTURE_STEP = 0.1D;
    private static final double POSE_MAKER_ROTATION_MIN = -180.0D;
    private static final double POSE_MAKER_ROTATION_MAX = 180.0D;
    private static final double POSE_MAKER_ROTATION_SNAP_RADIUS = 5.0D;
    private static final double POSE_MAKER_ROTATION_STEP = 1.0D;
    private static final long POSE_MAKER_SPACE_SEQUENCE_MILLIS = 900L;
    private static final String DEFAULT_POSE_MAKER_NAME_KEY = "panoramica.photo_mode.pose.custom";

    private Tab selectedTab = Tab.GENERAL;
    @Nullable
    private Confirmation confirmation;
    private boolean rotatingView;
    private boolean cameraCursorGrabbed;
    private int panelX;
    private int panelY;
    private int panelHeight;
    private boolean poseMakerOpen;
    private int poseMakerPanelX;
    private int poseMakerPanelY;
    private int poseMakerPanelWidth;
    private int poseMakerPanelHeight;
    private int poseMakerColumns;
    private boolean poseMakerSpaceDown;
    private int poseMakerSpacePresses;
    private long poseMakerFirstSpacePressMillis;
    private String poseMakerNameKey = DEFAULT_POSE_MAKER_NAME_KEY;
    private final PoseMakerRotation poseMakerModelRotation = new PoseMakerRotation();
    private final Map<PhotoPose.BodyPart, PoseMakerRotation> poseMakerPartRotations = new EnumMap<>(PhotoPose.BodyPart.class);
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
    private Button colorizeButton;
    @Nullable
    private Button stylizeButton;
    @Nullable
    private Button depthOfFieldButton;
    @Nullable
    private PhotoModeSlider depthOfFieldFocalLengthSlider;
    @Nullable
    private PhotoModeSlider depthOfFieldApertureSlider;
    @Nullable
    private PhotoModeSlider depthOfFieldFocusDistanceSlider;
    @Nullable
    private PhotoModeColorButton skyColorButton;
    @Nullable
    private PhotoModeColorButton fogColorButton;
    @Nullable
    private PhotoModeColorPicker colorPicker;
    @Nullable
    private ColorPickerTarget colorPickerTarget;
    @Nullable
    private EditBox poseMakerNameKeyBox;

    public PhotoModeScreen() {
        super(Component.translatable("panoramica.photo_mode.title"));
        for (PhotoPose.BodyPart part : PhotoPose.BodyPart.values()) {
            this.poseMakerPartRotations.put(part, new PoseMakerRotation());
        }
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

        this.updateButtonMessages();
        this.renderPanel(graphics);
        if (this.poseMakerOpen) {
            this.renderPoseMakerPanel(graphics);
        }
        int hoverMouseX = this.hoverMouseX(mouseX);
        int hoverMouseY = this.hoverMouseY(mouseY);
        if (this.colorPicker != null) {
            this.colorPicker.extractRenderState(graphics, this.font, hoverMouseX, hoverMouseY);
        }
        super.extractRenderState(graphics, hoverMouseX, hoverMouseY, a);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        this.stopRotatingView();
        if (PhotoModeManager.isPhotoModeUiHidden()) {
            if (this.handlePhotoModeActionMouse(event)) {
                return true;
            }
            if (event.button() == 0) {
                this.clearFocus();
                this.startRotatingView();
            }
            return true;
        }
        if (!this.isInsidePhotoModeUi(event.x(), event.y()) && this.handlePhotoModeActionMouse(event)) {
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
            this.startRotatingView();
            return true;
        }
        return true;
    }

    private boolean handlePhotoModeActionMouse(@NotNull MouseButtonEvent event) {
        if (KeyMappings.KEY_PHOTO_MODE_HIDE_UI.matchesMouse(event)) {
            this.setPhotoModeUiHidden(!PhotoModeManager.isPhotoModeUiHidden());
            return true;
        }
        if (KeyMappings.KEY_PHOTO_MODE_TOGGLE_GRID.matchesMouse(event)) {
            this.toggleGrid();
            return true;
        }
        return false;
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
            this.stopRotatingView();
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
        boolean poseMakerSpacePress = this.markPoseMakerSpaceDown(event);
        if (this.isPoseMakerNameKeyBoxFocused()) {
            if (event.isEscape()) {
                this.clearFocus();
            } else {
                super.keyPressed(event);
            }
            return true;
        }

        boolean configuredCameraControlKey = this.isConfiguredCameraControlKey(event);
        if (configuredCameraControlKey) {
            PhotoModeManager.setConfiguredCameraControlKeyState(event, true);
        }
        if (poseMakerSpacePress && this.registerPoseMakerSpacePress()) {
            this.togglePoseMaker();
            return true;
        }
        if (isHideGuiKey(event)) {
            this.setPhotoModeUiHidden(!PhotoModeManager.isPhotoModeUiHidden());
            return true;
        }
        if (isGridKey(event)) {
            this.toggleGrid();
            return true;
        }
        if (PhotoModeManager.isPhotoModeUiHidden()) {
            if (event.isEscape()) {
                this.setPhotoModeUiHidden(false);
            }
            return true;
        }
        if (event.isEscape() && this.colorPickerTarget != null) {
            this.closeColorPicker();
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
        if (event.key() == GLFW.GLFW_KEY_SPACE) {
            this.poseMakerSpaceDown = false;
        }
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
        this.stopRotatingView();
        this.clearPoseMakerPreview();
        PhotoModeManager.close();
    }

    private void rebuildPhotoWidgets() {
        this.clearWidgets();
        this.clearControlReferences();
        this.updatePanelBounds();
        this.updatePoseMakerPanelBounds();
        if (PhotoModeManager.isPhotoModeUiHidden()) {
            return;
        }
        if (this.poseMakerOpen) {
            this.addPoseMakerWidgets();
        }
        if (this.confirmation != null) {
            this.closeColorPicker();
            this.addConfirmationWidgets();
            return;
        }

        int x = this.panelX + PANEL_PADDING;
        int y = this.panelY + PANEL_PADDING;
        for (Tab tab : Tab.values()) {
            TexturedIconButton button = this.addRenderableWidget(new TexturedIconButton(tab.message(), ignored -> {
                this.selectedTab = tab;
                this.closeColorPicker();
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
            case LENS -> this.addLensControls(y);
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
        this.colorizeButton = null;
        this.stylizeButton = null;
        this.depthOfFieldButton = null;
        this.depthOfFieldFocalLengthSlider = null;
        this.depthOfFieldApertureSlider = null;
        this.depthOfFieldFocusDistanceSlider = null;
        this.skyColorButton = null;
        this.fogColorButton = null;
        this.colorPicker = null;
        this.poseMakerNameKeyBox = null;
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

        y = this.addColorEffectControls(x, y, width, active);

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

    private void addLensControls(int y) {
        int x = this.panelX + PANEL_PADDING;
        int width = this.controlWidth();
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }

        this.depthOfFieldButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.setDepthOfFieldEnabled(!active.depthOfFieldEnabled());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.depth_of_field.desc"))).build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.depthOfFieldFocalLengthSlider = this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                PhotoModeManager.DEPTH_OF_FIELD_FOCAL_LENGTH_MIN,
                PhotoModeManager.DEPTH_OF_FIELD_FOCAL_LENGTH_MAX,
                active.depthOfFieldFocalLength(),
                PhotoModeManager.DEPTH_OF_FIELD_FOCAL_LENGTH_DEFAULT,
                DEPTH_OF_FIELD_FOCAL_LENGTH_SNAP_RADIUS,
                DEPTH_OF_FIELD_FOCAL_LENGTH_STEP,
                value -> active.setDepthOfFieldFocalLength((float) value),
                value -> optionMessage("panoramica.photo_mode.dof_focal_length", Component.translatable("panoramica.photo_mode.millimeters", String.format(Locale.ROOT, "%.0f", value)).withStyle(Style.EMPTY.withColor(VALUE_COLOR)))
        ));
        this.depthOfFieldFocalLengthSlider.setTooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.dof_focal_length.desc")));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.depthOfFieldApertureSlider = this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                PhotoModeManager.DEPTH_OF_FIELD_APERTURE_MIN,
                PhotoModeManager.DEPTH_OF_FIELD_APERTURE_MAX,
                active.depthOfFieldAperture(),
                PhotoModeManager.DEPTH_OF_FIELD_APERTURE_DEFAULT,
                DEPTH_OF_FIELD_APERTURE_SNAP_RADIUS,
                DEPTH_OF_FIELD_APERTURE_STEP,
                value -> active.setDepthOfFieldAperture((float) value),
                value -> optionMessage("panoramica.photo_mode.dof_aperture", Component.translatable("panoramica.photo_mode.aperture", String.format(Locale.ROOT, "%.1f", value)).withStyle(Style.EMPTY.withColor(VALUE_COLOR)))
        ));
        this.depthOfFieldApertureSlider.setTooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.dof_aperture.desc")));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.depthOfFieldFocusDistanceSlider = this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                PhotoModeManager.DEPTH_OF_FIELD_FOCUS_DISTANCE_MIN,
                PhotoModeManager.DEPTH_OF_FIELD_FOCUS_DISTANCE_MAX,
                active.depthOfFieldFocusDistance(),
                PhotoModeManager.DEPTH_OF_FIELD_FOCUS_DISTANCE_DEFAULT,
                DEPTH_OF_FIELD_FOCUS_DISTANCE_SNAP_RADIUS,
                DEPTH_OF_FIELD_FOCUS_DISTANCE_STEP,
                value -> active.setDepthOfFieldFocusDistance((float) value),
                value -> optionMessage("panoramica.photo_mode.dof_focus_distance", Component.translatable("panoramica.photo_mode.blocks", String.format(Locale.ROOT, "%.2f", value)).withStyle(Style.EMPTY.withColor(VALUE_COLOR)))
        ));
        this.depthOfFieldFocusDistanceSlider.setTooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.dof_focus_distance.desc")));
    }

    private int addColorEffectControls(int x, int y, int width, @NotNull PhotoModeManager.Session active) {
        PhotoModeSlider vignetteSlider = this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                0.0D,
                1.0D,
                active.vignette(),
                0.0D,
                VIGNETTE_SNAP_RADIUS,
                value -> active.setVignette((float) value),
                value -> optionMessage("panoramica.photo_mode.vignette", Component.translatable("panoramica.photo_mode.percent", Math.round(value * 100.0D)).withStyle(Style.EMPTY.withColor(VALUE_COLOR)))
        ));
        vignetteSlider.setTooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.vignette.desc")));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        y = this.addColorAdjustmentSlider(
                x,
                y,
                width,
                "panoramica.photo_mode.gamma",
                "panoramica.photo_mode.gamma.desc",
                PhotoModeManager.GAMMA_MIN,
                PhotoModeManager.GAMMA_MAX,
                active.gamma(),
                PhotoModeManager.GAMMA_DEFAULT,
                value -> active.setGamma((float) value)
        );

        y = this.addColorAdjustmentSlider(
                x,
                y,
                width,
                "panoramica.photo_mode.saturation",
                "panoramica.photo_mode.saturation.desc",
                PhotoModeManager.SATURATION_MIN,
                PhotoModeManager.SATURATION_MAX,
                active.saturation(),
                PhotoModeManager.SATURATION_DEFAULT,
                value -> active.setSaturation((float) value)
        );

        y = this.addColorAdjustmentSlider(
                x,
                y,
                width,
                "panoramica.photo_mode.contrast",
                "panoramica.photo_mode.contrast.desc",
                PhotoModeManager.CONTRAST_MIN,
                PhotoModeManager.CONTRAST_MAX,
                active.contrast(),
                PhotoModeManager.CONTRAST_DEFAULT,
                value -> active.setContrast((float) value)
        );

        y = this.addColorAdjustmentSlider(
                x,
                y,
                width,
                "panoramica.photo_mode.overexposure",
                "panoramica.photo_mode.overexposure.desc",
                PhotoModeManager.OVEREXPOSURE_MIN,
                PhotoModeManager.OVEREXPOSURE_MAX,
                active.overexposure(),
                PhotoModeManager.OVEREXPOSURE_DEFAULT,
                value -> active.setOverexposure((float) value)
        );

        PhotoModeSlider bloomSlider = this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                PhotoModeManager.BLOOM_MIN,
                PhotoModeManager.BLOOM_MAX,
                active.bloom(),
                PhotoModeManager.BLOOM_DEFAULT,
                BLOOM_SNAP_RADIUS,
                BLOOM_STEP,
                value -> active.setBloom((float) value),
                value -> optionMessage("panoramica.photo_mode.bloom", Component.translatable("panoramica.photo_mode.percent", Math.round(value * 100.0D)).withStyle(Style.EMPTY.withColor(VALUE_COLOR)))
        ));
        bloomSlider.setTooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.bloom.desc")));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.colorizeButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.setColorizePreset(active.colorizePreset().next());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.colorize.desc"))).build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        this.stylizeButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            active.setStylizePreset(active.stylizePreset().next());
            this.updateButtonMessages();
        }).bounds(x, y, width, CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("panoramica.photo_mode.stylize.desc"))).build());
        return y + CONTROL_HEIGHT + CONTROL_GAP;
    }

    private int addColorAdjustmentSlider(
            int x,
            int y,
            int width,
            @NotNull String labelKey,
            @NotNull String tooltipKey,
            float minValue,
            float maxValue,
            float currentValue,
            float defaultValue,
            @NotNull DoubleConsumer valueConsumer
    ) {
        PhotoModeSlider slider = this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                minValue,
                maxValue,
                currentValue,
                defaultValue,
                COLOR_ADJUSTMENT_SNAP_RADIUS,
                COLOR_ADJUSTMENT_STEP,
                valueConsumer,
                value -> optionMessage(labelKey, signedPercentValue(value))
        ));
        slider.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        return y + CONTROL_HEIGHT + CONTROL_GAP;
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

        this.skyColorButton = this.addPhotoColorButton(
                x,
                y,
                width,
                ColorPickerTarget.SKY,
                active::skyColorOverride,
                active::skyColor,
                PhotoModeScreen::emptyColor,
                active::setSkyColor
        );
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

        this.fogColorButton = this.addPhotoColorButton(
                x,
                y,
                width,
                ColorPickerTarget.FOG,
                active::fogColorOverride,
                active::fogColor,
                PhotoModeScreen::emptyColor,
                active::setFogColor
        );
    }

    private void addActionButtons() {
        Component takePhotoMessage = this.takePhotoMessage();
        Component returnToPlayerMessage = Component.translatable("panoramica.photo_mode.return_to_player");
        Component hideGuiMessage = this.hideGuiMessage();
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
            this.closeColorPicker();
            this.confirmation = Confirmation.HIDE_GUI;
            this.rebuildPhotoWidgets();
        }, this.hideGuiDescription());
        x += hideGuiWidth + ACTION_GAP;
        this.addActionButton(resetMessage, x, y, resetWidth, button -> {
            this.closeColorPicker();
            this.confirmation = Confirmation.RESET;
            this.rebuildPhotoWidgets();
        }, resetMessage);
        x += resetWidth + ACTION_GAP;
        this.addActionButton(leaveMessage, x, y, leaveWidth, button -> {
            this.closeColorPicker();
            this.confirmation = Confirmation.LEAVE;
            this.rebuildPhotoWidgets();
        }, Component.translatable("panoramica.photo_mode.leave"));
    }

    @NotNull
    private PhotoModeColorButton addPhotoColorButton(
            int x,
            int y,
            int width,
            @NotNull ColorPickerTarget target,
            @NotNull Supplier<@Nullable Integer> colorSupplier,
            @NotNull IntSupplier editColorSupplier,
            @NotNull Supplier<@Nullable Integer> defaultColorSupplier,
            @NotNull Consumer<@Nullable Integer> colorConsumer
    ) {
        PhotoModeColorButton button = this.addRenderableWidget(new PhotoModeColorButton(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                Component.empty(),
                ignored -> {
                    this.colorPickerTarget = this.colorPickerTarget == target ? null : target;
                    this.rebuildPhotoWidgets();
                },
                colorSupplier
        ));
        if (this.colorPickerTarget == target) {
            this.addColorPicker(target, colorSupplier, editColorSupplier, defaultColorSupplier, colorConsumer);
        }
        return button;
    }

    private void addColorPicker(
            @NotNull ColorPickerTarget target,
            @NotNull Supplier<@Nullable Integer> colorSupplier,
            @NotNull IntSupplier editColorSupplier,
            @NotNull Supplier<@Nullable Integer> defaultColorSupplier,
            @NotNull Consumer<@Nullable Integer> colorConsumer
    ) {
        this.colorPicker = new PhotoModeColorPicker(target.title(), colorSupplier, editColorSupplier, defaultColorSupplier, colorConsumer);
        this.updateColorPickerPosition();
        this.addRenderableWidget(Button.builder(Component.translatable("panoramica.photo_mode.color_picker.reset_default"), button -> {
            if (this.colorPicker != null) {
                this.colorPicker.resetToDefault();
                this.updateButtonMessages();
            }
        }).bounds(
                this.colorPicker.resetButtonX(),
                this.colorPicker.resetButtonY(),
                this.colorPicker.resetButtonWidth(),
                this.colorPicker.resetButtonHeight()
        ).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            this.closeColorPicker();
            this.rebuildPhotoWidgets();
        }).bounds(
                this.colorPicker.doneButtonX(),
                this.colorPicker.doneButtonY(),
                this.colorPicker.doneButtonWidth(),
                this.colorPicker.doneButtonHeight()
        ).build());
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

    private void addPoseMakerWidgets() {
        int contentX = this.poseMakerPanelX + PANEL_PADDING;
        int contentWidth = this.poseMakerControlWidth();
        int y = this.poseMakerNameBoxY();

        this.poseMakerNameKeyBox = this.addRenderableWidget(new EditBox(
                this.font,
                contentX,
                y,
                contentWidth,
                CONTROL_HEIGHT,
                Component.translatable("panoramica.photo_mode.pose_maker.name_key")
        ));
        this.poseMakerNameKeyBox.setMaxLength(256);
        this.poseMakerNameKeyBox.setHint(Component.translatable("panoramica.photo_mode.pose_maker.name_key_hint"));
        this.poseMakerNameKeyBox.setValue(this.poseMakerNameKey);
        this.poseMakerNameKeyBox.setResponder(value -> {
            this.poseMakerNameKey = value;
            this.syncPoseMakerPreview();
        });

        int columnWidth = this.poseMakerColumnWidth();
        int sliderY = this.poseMakerSliderStartY();
        int index = 0;
        for (PoseMakerAxis axis : PoseMakerAxis.values()) {
            index = this.addPoseMakerRotationSlider(
                    index,
                    sliderY,
                    columnWidth,
                    "panoramica.photo_mode.pose_maker.part.model",
                    this.poseMakerModelRotation,
                    axis
            );
        }
        for (PhotoPose.BodyPart part : PhotoPose.BodyPart.values()) {
            PoseMakerRotation rotation = this.poseMakerPartRotations.get(part);
            if (rotation == null) {
                continue;
            }
            for (PoseMakerAxis axis : PoseMakerAxis.values()) {
                index = this.addPoseMakerRotationSlider(index, sliderY, columnWidth, part.labelKey(), rotation, axis);
            }
        }

        int buttonY = this.poseMakerPanelY + this.poseMakerPanelHeight - PANEL_PADDING - CONTROL_HEIGHT;
        int secondaryButtonY = buttonY - POSE_MAKER_BUTTON_GAP - CONTROL_HEIGHT;
        int buttonWidth = (contentWidth - POSE_MAKER_BUTTON_GAP) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("panoramica.photo_mode.pose_maker.reset"), button -> this.resetPoseMakerSliders())
                .bounds(contentX, secondaryButtonY, buttonWidth, CONTROL_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("panoramica.photo_mode.pose_maker.load"), button -> this.loadPoseMakerPose())
                .bounds(contentX + buttonWidth + POSE_MAKER_BUTTON_GAP, secondaryButtonY, contentWidth - buttonWidth - POSE_MAKER_BUTTON_GAP, CONTROL_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("panoramica.photo_mode.pose_maker.save"), button -> {
            if (this.minecraft != null) {
                PhotoPoseExporter.saveWithNativeDialog(this.minecraft, this.createPoseMakerPose());
            }
        }).bounds(contentX, buttonY, buttonWidth, CONTROL_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("panoramica.photo_mode.pose_maker.close"), button -> this.closePoseMaker())
                .bounds(contentX + buttonWidth + POSE_MAKER_BUTTON_GAP, buttonY, contentWidth - buttonWidth - POSE_MAKER_BUTTON_GAP, CONTROL_HEIGHT)
                .build());
    }

    private int addPoseMakerRotationSlider(
            int index,
            int sliderStartY,
            int columnWidth,
            @NotNull String labelKey,
            @NotNull PoseMakerRotation rotation,
            @NotNull PoseMakerAxis axis
    ) {
        int column = index % this.poseMakerColumns;
        int row = index / this.poseMakerColumns;
        int x = this.poseMakerPanelX + PANEL_PADDING + column * (columnWidth + POSE_MAKER_COLUMN_GAP);
        int y = sliderStartY + row * (CONTROL_HEIGHT + CONTROL_GAP);
        this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                columnWidth,
                CONTROL_HEIGHT,
                POSE_MAKER_ROTATION_MIN,
                POSE_MAKER_ROTATION_MAX,
                rotation.value(axis),
                0.0D,
                POSE_MAKER_ROTATION_SNAP_RADIUS,
                POSE_MAKER_ROTATION_STEP,
                value -> {
                    rotation.set(axis, value);
                    this.syncPoseMakerPreview();
                },
                value -> optionMessage(
                        "panoramica.photo_mode.pose_maker.rotation",
                        Component.translatable(labelKey),
                        Component.literal(axis.label()),
                        this.degreeValue(value)
                )
        ));
        return index + 1;
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
            this.gridButton.setMessage(this.gridMessage(active.gridEnabled()));
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
        if (this.colorizeButton != null) {
            PhotoModeColorizePreset preset = active.colorizePreset();
            this.colorizeButton.setMessage(optionMessage("panoramica.photo_mode.colorize", Component.translatable(preset.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.stylizeButton != null) {
            PhotoModeStylizePreset preset = active.stylizePreset();
            this.stylizeButton.setMessage(optionMessage("panoramica.photo_mode.stylize", Component.translatable(preset.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.depthOfFieldButton != null) {
            this.depthOfFieldButton.setMessage(optionMessage("panoramica.photo_mode.depth_of_field", enabledValue(active.depthOfFieldEnabled())));
        }
        if (this.depthOfFieldFocalLengthSlider != null) {
            this.depthOfFieldFocalLengthSlider.active = active.depthOfFieldEnabled();
        }
        if (this.depthOfFieldApertureSlider != null) {
            this.depthOfFieldApertureSlider.active = active.depthOfFieldEnabled();
        }
        if (this.depthOfFieldFocusDistanceSlider != null) {
            this.depthOfFieldFocusDistanceSlider.active = active.depthOfFieldEnabled();
        }
        if (this.skyColorButton != null) {
            this.skyColorButton.setMessage(optionMessage("panoramica.photo_mode.sky_color", Component.literal(PhotoModeColorPicker.formatHexColor(active.skyColorOverride())).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.fogColorButton != null) {
            this.fogColorButton.setMessage(optionMessage("panoramica.photo_mode.fog_color", Component.literal(PhotoModeColorPicker.formatHexColor(active.fogColorOverride())).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
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
            graphics.textWithWordWrap(this.font, this.confirmationMessage(this.confirmation), contentX + 10, contentY + 36, width - 20, 0xFFFFFFFF);
            return;
        }

        int tabY = this.panelY + PANEL_PADDING;
        int tabX = this.panelX + PANEL_PADDING + this.selectedTab.ordinal() * (TexturedIconButton.DEFAULT_BUTTON_SIZE + TAB_GAP);
        graphics.outline(tabX - 1, tabY - 1, TexturedIconButton.DEFAULT_BUTTON_SIZE + 2, TexturedIconButton.DEFAULT_BUTTON_SIZE + 2, PANEL_ACCENT_COLOR);
    }

    private void renderPoseMakerPanel(@NotNull GuiGraphicsExtractor graphics) {
        RenderingUtils.renderBorder(graphics, this.poseMakerPanelX - 1, this.poseMakerPanelY - 1, this.poseMakerPanelWidth + 2, this.poseMakerPanelHeight + 2, 1, PANEL_BORDER_COLOR);
        graphics.fill(this.poseMakerPanelX, this.poseMakerPanelY, this.poseMakerPanelX + this.poseMakerPanelWidth, this.poseMakerPanelY + this.poseMakerPanelHeight, PANEL_BACKGROUND_COLOR);
        graphics.centeredText(
                this.font,
                Component.translatable("panoramica.photo_mode.pose_maker.title"),
                this.poseMakerPanelX + this.poseMakerPanelWidth / 2,
                this.poseMakerPanelY + PANEL_PADDING + 3,
                PANEL_ACCENT_COLOR
        );
        graphics.text(
                this.font,
                Component.translatable("panoramica.photo_mode.pose_maker.name_key"),
                this.poseMakerPanelX + PANEL_PADDING,
                this.poseMakerNameLabelY(),
                0xFFFFFFFF
        );
    }

    private void updatePanelBounds() {
        int actionRowReserve = this.confirmation == null ? CONTROL_HEIGHT + ACTION_ROW_GAP : 0;
        int availablePanelHeight = Math.max(CONTROL_HEIGHT, this.height - SCREEN_MARGIN * 2 - actionRowReserve);
        this.panelHeight = Math.min(availablePanelHeight, this.confirmation != null ? 118 : this.selectedTab.panelHeight());
        this.panelX = Math.max(SCREEN_MARGIN, this.width - PANEL_WIDTH - SCREEN_MARGIN);
        this.panelY = Math.max(SCREEN_MARGIN, this.height - this.panelHeight - SCREEN_MARGIN - actionRowReserve);
    }

    private void updatePoseMakerPanelBounds() {
        int actionRowReserve = CONTROL_HEIGHT + ACTION_ROW_GAP;
        int availableHeight = Math.max(CONTROL_HEIGHT, this.height - SCREEN_MARGIN * 2 - actionRowReserve);
        int availableWidth = this.poseMakerAvailableWidth();
        this.poseMakerColumns = this.calculatePoseMakerColumns(availableWidth, availableHeight);
        this.poseMakerPanelWidth = this.calculatePoseMakerPanelWidth(availableWidth, this.poseMakerColumns);
        this.poseMakerPanelHeight = Math.min(availableHeight, this.poseMakerDesiredPanelHeight(this.poseMakerColumns));
        this.poseMakerPanelX = SCREEN_MARGIN;
        this.poseMakerPanelY = Math.max(SCREEN_MARGIN, this.height - this.poseMakerPanelHeight - SCREEN_MARGIN - actionRowReserve);
    }

    private int poseMakerAvailableWidth() {
        int leftRegionWidth = this.panelX - COLOR_PICKER_GAP - SCREEN_MARGIN;
        int minimumWidth = this.poseMakerMinimumPanelWidth(POSE_MAKER_MIN_COLUMNS);
        if (leftRegionWidth >= minimumWidth) {
            return leftRegionWidth;
        }
        return Math.max(minimumWidth, this.width - SCREEN_MARGIN * 2);
    }

    private int calculatePoseMakerColumns(int availableWidth, int availableHeight) {
        int maxColumns = Math.min(POSE_MAKER_MAX_COLUMNS, Math.max(POSE_MAKER_MIN_COLUMNS, (availableWidth - PANEL_PADDING * 2 + POSE_MAKER_COLUMN_GAP) / (POSE_MAKER_MIN_COLUMN_WIDTH + POSE_MAKER_COLUMN_GAP)));
        int firstColumns = Math.min(maxColumns, Math.max(POSE_MAKER_MIN_COLUMNS, 3));
        for (int columns = firstColumns; columns <= maxColumns; columns++) {
            if (this.poseMakerDesiredPanelHeight(columns) <= availableHeight) {
                return columns;
            }
        }
        return maxColumns;
    }

    private int calculatePoseMakerPanelWidth(int availableWidth, int columns) {
        int minimumWidth = this.poseMakerMinimumPanelWidth(columns);
        int preferredWidth = PANEL_PADDING * 2 + columns * POSE_MAKER_MAX_COLUMN_WIDTH + (columns - 1) * POSE_MAKER_COLUMN_GAP;
        return Math.max(minimumWidth, Math.min(preferredWidth, availableWidth));
    }

    private int poseMakerMinimumPanelWidth(int columns) {
        return PANEL_PADDING * 2 + columns * POSE_MAKER_MIN_COLUMN_WIDTH + (columns - 1) * POSE_MAKER_COLUMN_GAP;
    }

    private int poseMakerDesiredPanelHeight(int columns) {
        int rows = (this.poseMakerSliderCount() + columns - 1) / columns;
        int sliderHeight = rows * CONTROL_HEIGHT + Math.max(0, rows - 1) * CONTROL_GAP;
        return PANEL_PADDING * 2
                + POSE_MAKER_HEADER_HEIGHT
                + CONTROL_GAP
                + POSE_MAKER_NAME_LABEL_HEIGHT
                + CONTROL_HEIGHT
                + CONTROL_GAP
                + sliderHeight
                + CONTROL_GAP
                + CONTROL_HEIGHT
                + POSE_MAKER_BUTTON_GAP
                + CONTROL_HEIGHT;
    }

    private int poseMakerSliderCount() {
        return (PhotoPose.BodyPart.values().length + 1) * PoseMakerAxis.values().length;
    }

    private int poseMakerControlWidth() {
        return this.poseMakerPanelWidth - PANEL_PADDING * 2;
    }

    private int poseMakerColumnWidth() {
        return (this.poseMakerControlWidth() - (this.poseMakerColumns - 1) * POSE_MAKER_COLUMN_GAP) / this.poseMakerColumns;
    }

    private int poseMakerNameLabelY() {
        return this.poseMakerPanelY + PANEL_PADDING + POSE_MAKER_HEADER_HEIGHT + CONTROL_GAP;
    }

    private int poseMakerNameBoxY() {
        return this.poseMakerNameLabelY() + POSE_MAKER_NAME_LABEL_HEIGHT;
    }

    private int poseMakerSliderStartY() {
        return this.poseMakerNameBoxY() + CONTROL_HEIGHT + CONTROL_GAP;
    }

    private void updateColorPickerPosition() {
        if (this.colorPicker == null) {
            return;
        }
        int x = Math.max(SCREEN_MARGIN, this.panelX - PhotoModeColorPicker.WIDTH - COLOR_PICKER_GAP);
        int y = Math.max(SCREEN_MARGIN, Math.min(this.panelY, this.height - SCREEN_MARGIN - PhotoModeColorPicker.HEIGHT));
        this.colorPicker.setPosition(x, y);
    }

    @NotNull
    private Component confirmationMessage(@NotNull Confirmation confirmation) {
        if (confirmation == Confirmation.HIDE_GUI) {
            return Component.translatable(confirmation.messageKey, this.hideGuiKeyMessage());
        }
        return confirmation.message();
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
        return this.actionButtonWidth(this.takePhotoMessage(), ACTION_TAKE_PHOTO_MIN_WIDTH)
                + this.actionButtonWidth(Component.translatable("panoramica.photo_mode.return_to_player"), ACTION_RETURN_TO_PLAYER_MIN_WIDTH)
                + this.actionButtonWidth(this.hideGuiMessage(), ACTION_HIDE_GUI_MIN_WIDTH)
                + this.actionButtonWidth(Component.translatable("panoramica.photo_mode.reset"), ACTION_RESET_MIN_WIDTH)
                + this.actionButtonWidth(Component.translatable("panoramica.photo_mode.leave_short"), ACTION_LEAVE_MIN_WIDTH)
                + ACTION_GAP * 4;
    }

    @NotNull
    private Component takePhotoMessage() {
        Minecraft minecraft = this.minecraft == null ? Minecraft.getInstance() : this.minecraft;
        return Component.translatable("panoramica.photo_mode.take_photo", minecraft.options.keyScreenshot.getTranslatedKeyMessage());
    }

    @NotNull
    private Component gridMessage(boolean enabled) {
        return optionMessage("panoramica.photo_mode.grid", KeyMappings.KEY_PHOTO_MODE_TOGGLE_GRID.getTranslatedKeyMessage(), enabledValue(enabled));
    }

    @NotNull
    private Component hideGuiMessage() {
        return Component.translatable("panoramica.photo_mode.hide_gui", this.hideGuiKeyMessage());
    }

    @NotNull
    private Component hideGuiDescription() {
        return Component.translatable("panoramica.photo_mode.hide_gui.desc", this.hideGuiKeyMessage());
    }

    @NotNull
    private Component hideGuiKeyMessage() {
        return KeyMappings.KEY_PHOTO_MODE_HIDE_UI.getTranslatedKeyMessage();
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
        if (insidePanel) {
            return true;
        }
        if (this.poseMakerOpen && mouseX >= this.poseMakerPanelX && mouseX <= this.poseMakerPanelX + this.poseMakerPanelWidth && mouseY >= this.poseMakerPanelY && mouseY <= this.poseMakerPanelY + this.poseMakerPanelHeight) {
            return true;
        }
        if (this.confirmation != null) {
            return false;
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
                || this.minecraft.options.keySprint.matches(event)
                || KeyMappings.KEY_PHOTO_MODE_SLOW_CAMERA.matches(event);
    }

    private static boolean isHideGuiKey(@NotNull KeyEvent event) {
        return KeyMappings.KEY_PHOTO_MODE_HIDE_UI.matches(event);
    }

    private static boolean isGridKey(@NotNull KeyEvent event) {
        return KeyMappings.KEY_PHOTO_MODE_TOGGLE_GRID.matches(event);
    }

    private boolean isPoseMakerNameKeyBoxFocused() {
        return this.poseMakerNameKeyBox != null && this.poseMakerNameKeyBox.isFocused();
    }

    private boolean markPoseMakerSpaceDown(@NotNull KeyEvent event) {
        if (event.key() != GLFW.GLFW_KEY_SPACE) {
            return false;
        }
        if (this.poseMakerSpaceDown) {
            return false;
        }
        this.poseMakerSpaceDown = true;
        return true;
    }

    @Nullable
    private static Integer emptyColor() {
        return null;
    }

    private boolean registerPoseMakerSpacePress() {
        long now = Util.getMillis();
        if (this.poseMakerFirstSpacePressMillis == 0L || now - this.poseMakerFirstSpacePressMillis > POSE_MAKER_SPACE_SEQUENCE_MILLIS) {
            this.poseMakerFirstSpacePressMillis = now;
            this.poseMakerSpacePresses = 0;
        }

        this.poseMakerSpacePresses++;
        if (this.poseMakerSpacePresses < POSE_MAKER_SPACE_PRESS_COUNT) {
            return false;
        }

        this.poseMakerSpacePresses = 0;
        this.poseMakerFirstSpacePressMillis = 0L;
        return true;
    }

    private void togglePoseMaker() {
        if (this.poseMakerOpen) {
            this.closePoseMaker();
        } else {
            this.openPoseMaker();
        }
    }

    private void openPoseMaker() {
        this.poseMakerOpen = true;
        this.closeColorPicker();
        this.syncPoseMakerPreview();
        this.rebuildPhotoWidgets();
    }

    private void closePoseMaker() {
        this.poseMakerOpen = false;
        this.clearPoseMakerPreview();
        this.rebuildPhotoWidgets();
    }

    private void syncPoseMakerPreview() {
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active != null && this.poseMakerOpen) {
            active.setPoseMakerPose(this.createPoseMakerPose());
        }
    }

    private void clearPoseMakerPreview() {
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active != null) {
            active.setPoseMakerPose(null);
        }
    }

    private void resetPoseMakerSliders() {
        this.poseMakerModelRotation.reset();
        for (PoseMakerRotation rotation : this.poseMakerPartRotations.values()) {
            rotation.reset();
        }
        this.syncPoseMakerPreview();
        this.rebuildPhotoWidgets();
    }

    private void loadPoseMakerPose() {
        if (this.minecraft == null) {
            return;
        }
        PhotoPose pose = PhotoPoseExporter.loadWithNativeDialog(this.minecraft);
        if (pose == null) {
            return;
        }
        this.applyPoseToPoseMaker(pose);
        this.syncPoseMakerPreview();
        this.rebuildPhotoWidgets();
    }

    private void applyPoseToPoseMaker(@NotNull PhotoPose pose) {
        this.poseMakerNameKey = pose.nameKey();
        this.poseMakerModelRotation.set(pose.modelRotation());
        for (PhotoPose.BodyPart part : PhotoPose.BodyPart.values()) {
            PoseMakerRotation rotation = this.poseMakerPartRotations.get(part);
            if (rotation != null) {
                rotation.set(pose.rotations().getOrDefault(part, PhotoPose.PartRotation.ZERO));
            }
        }
    }

    @NotNull
    private PhotoPose createPoseMakerPose() {
        Map<PhotoPose.BodyPart, PhotoPose.PartRotation> rotations = PhotoPose.emptyRotationMap();
        for (PhotoPose.BodyPart part : PhotoPose.BodyPart.values()) {
            PoseMakerRotation rotation = this.poseMakerPartRotations.get(part);
            if (rotation == null) {
                continue;
            }
            PhotoPose.PartRotation partRotation = rotation.toPartRotation();
            if (!partRotation.isZero()) {
                rotations.put(part, partRotation);
            }
        }
        return new PhotoPose(
                this.poseMakerNameKey(),
                this.poseMakerModelRotation.toPartRotation(),
                Map.copyOf(rotations)
        );
    }

    @NotNull
    private String poseMakerNameKey() {
        String key = this.poseMakerNameKey.trim();
        return key.isEmpty() ? DEFAULT_POSE_MAKER_NAME_KEY : key;
    }

    private void setPhotoModeUiHidden(boolean hidden) {
        this.confirmation = null;
        this.stopRotatingView();
        if (hidden) {
            this.closeColorPicker();
        }
        PhotoModeManager.setPhotoModeUiHidden(hidden);
        this.rebuildPhotoWidgets();
    }

    private void startRotatingView() {
        this.rotatingView = true;
        this.setCameraCursorGrabbed(true);
    }

    private void stopRotatingView() {
        this.rotatingView = false;
        this.setCameraCursorGrabbed(false);
    }

    private void setCameraCursorGrabbed(boolean grabbed) {
        if (this.cameraCursorGrabbed == grabbed || this.minecraft == null) {
            return;
        }
        if (grabbed && !this.minecraft.isWindowActive()) {
            return;
        }

        this.cameraCursorGrabbed = grabbed;
        double centerX = this.minecraft.getWindow().getScreenWidth() / 2.0D;
        double centerY = this.minecraft.getWindow().getScreenHeight() / 2.0D;
        this.minecraft.mouseHandler.setIgnoreFirstMove();
        InputConstants.grabOrReleaseMouse(
                this.minecraft.getWindow(),
                grabbed ? InputConstants.CURSOR_DISABLED : InputConstants.CURSOR_NORMAL,
                centerX,
                centerY
        );
    }

    private int hoverMouseX(int mouseX) {
        return this.cameraCursorGrabbed ? NO_HOVER_MOUSE_POSITION : mouseX;
    }

    private int hoverMouseY(int mouseY) {
        return this.cameraCursorGrabbed ? NO_HOVER_MOUSE_POSITION : mouseY;
    }

    private void closeColorPicker() {
        this.colorPickerTarget = null;
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
    private static Component optionMessage(@NotNull String key, @NotNull Object... args) {
        return Component.translatable(key, args);
    }

    @NotNull
    private static Component signedPercentValue(double value) {
        int percent = (int) Math.round(value * 100.0D);
        String sign = percent > 0 ? "+" : "";
        return Component.translatable("panoramica.photo_mode.percent", sign + percent)
                .withStyle(Style.EMPTY.withColor(VALUE_COLOR));
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
        GENERAL(GENERAL_ICON, "panoramica.photo_mode.tab.general", 313),
        LENS(LENS_ICON, "panoramica.photo_mode.tab.lens", 138),
        PLAYER(PLAYER_ICON, "panoramica.photo_mode.tab.player", 263),
        ENVIRONMENT(GLOBE_ICON, "panoramica.photo_mode.tab.environment", 213);

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

    private enum PoseMakerAxis {
        X("X"),
        Y("Y"),
        Z("Z");

        private final String label;

        PoseMakerAxis(@NotNull String label) {
            this.label = label;
        }

        @NotNull
        private String label() {
            return this.label;
        }
    }

    private static final class PoseMakerRotation {

        private double x;
        private double y;
        private double z;

        private double value(@NotNull PoseMakerAxis axis) {
            return switch (axis) {
                case X -> this.x;
                case Y -> this.y;
                case Z -> this.z;
            };
        }

        private void set(@NotNull PoseMakerAxis axis, double value) {
            double clamped = Mth.clamp(value, POSE_MAKER_ROTATION_MIN, POSE_MAKER_ROTATION_MAX);
            switch (axis) {
                case X -> this.x = clamped;
                case Y -> this.y = clamped;
                case Z -> this.z = clamped;
            }
        }

        private void set(@NotNull PhotoPose.PartRotation rotation) {
            this.set(PoseMakerAxis.X, rotation.xDegrees());
            this.set(PoseMakerAxis.Y, rotation.yDegrees());
            this.set(PoseMakerAxis.Z, rotation.zDegrees());
        }

        private void reset() {
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
        }

        @NotNull
        private PhotoPose.PartRotation toPartRotation() {
            return PhotoPose.PartRotation.degrees((float) this.x, (float) this.y, (float) this.z);
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

    private enum ColorPickerTarget {
        SKY("panoramica.photo_mode.sky_color_picker"),
        FOG("panoramica.photo_mode.fog_color_picker");

        private final String titleKey;

        ColorPickerTarget(@NotNull String titleKey) {
            this.titleKey = titleKey;
        }

        @NotNull
        private Component title() {
            return Component.translatable(this.titleKey);
        }
    }

}
