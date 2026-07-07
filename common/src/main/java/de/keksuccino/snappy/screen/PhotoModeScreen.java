package de.keksuccino.snappy.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.snappy.KeyMappings;
import de.keksuccino.snappy.OptionsScreen;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.client.gui.UIFormatting;
import de.keksuccino.snappy.photo.PhotoModeArmorMode;
import de.keksuccino.snappy.photo.PhotoModeColorizePreset;
import de.keksuccino.snappy.photo.PhotoModeHeldItemsMode;
import de.keksuccino.snappy.photo.PhotoModeManager;
import de.keksuccino.snappy.photo.PhotoModeSeason;
import de.keksuccino.snappy.photo.PhotoModeStylizePreset;
import de.keksuccino.snappy.photo.PhotoModeTimePreset;
import de.keksuccino.snappy.photo.PhotoModeWeatherPreset;
import de.keksuccino.snappy.photo.PhotoPose;
import de.keksuccino.snappy.photo.PhotoPoseExporter;
import de.keksuccino.snappy.photo.PhotoPoseManager;
import de.keksuccino.snappy.util.rendering.RenderingUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class PhotoModeScreen extends Screen {

    private static final Identifier GENERAL_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/photo_mode/tabs/tab_general_icon_15x15.png");
    private static final Identifier PLAYER_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/photo_mode/tabs/tab_player_icon_15x15.png");
    private static final Identifier LENS_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/photo_mode/tabs/tab_lens_icon_15x15.png");
    private static final Identifier WORLD_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/photo_mode/tabs/tab_world_icon_15x15.png");
    private static final Identifier SETTINGS_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/browser/settings_icon_15x15.png");
    private static final int PANEL_WIDTH = 236;
    static final int PANEL_PADDING = 8;
    static final int CONTROL_HEIGHT = 20;
    static final int CONTROL_GAP = 5;
    private static final int TAB_GAP = 4;
    private static final int TAB_SCROLLBAR_SPACING = 2;
    private static final int TAB_SCROLLBAR_RESERVE = AbstractScrollArea.SCROLLBAR_WIDTH + TAB_SCROLLBAR_SPACING;
    private static final int TAB_BODY_TOP_OFFSET = PANEL_PADDING + TexturedIconButton.DEFAULT_BUTTON_SIZE + CONTROL_GAP + 2;
    private static final int MIN_TAB_BODY_HEIGHT = CONTROL_HEIGHT;
    private static final int MIN_TAB_PANEL_HEIGHT = TAB_BODY_TOP_OFFSET + MIN_TAB_BODY_HEIGHT + PANEL_PADDING;
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
    static final int POSE_MAKER_COLUMN_GAP = 6;
    private static final int POSE_MAKER_MIN_COLUMNS = 2;
    private static final int POSE_MAKER_MAX_COLUMNS = 4;
    private static final int POSE_MAKER_HEADER_HEIGHT = 14;
    private static final int POSE_MAKER_NAME_LABEL_HEIGHT = 10;
    static final int POSE_MAKER_BUTTON_GAP = 5;
    private static final int POSE_MAKER_SPACE_PRESS_COUNT = 15;
    private static final int PANEL_BACKGROUND_COLOR = ARGB.color(174, 0, 0, 0);
    private static final int PANEL_ACCENT_COLOR = ARGB.color(255, 255, 209, 102);
    private static final int PANEL_BORDER_COLOR = ARGB.color(210, 116, 128, 142);
    private static final int SECTION_BACKGROUND_COLOR = ARGB.color(82, 24, 28, 34);
    private static final int GRID_LINE_COLOR = ARGB.color(112, 255, 255, 255);
    static final int VALUE_COLOR = 0xFFFFAA00;
    private static final int NO_HOVER_MOUSE_POSITION = -1;
    static final double FOV_SNAP_RADIUS = 2.0D;
    static final double ROLL_SNAP_RADIUS = 5.0D;
    static final double VIGNETTE_SNAP_RADIUS = 0.05D;
    static final double BLOOM_SNAP_RADIUS = 0.03D;
    static final double BLOOM_STEP = 0.01D;
    static final double FILM_GRAIN_SNAP_RADIUS = 0.03D;
    static final double FILM_GRAIN_STEP = 0.01D;
    static final double CHROMATIC_ABERRATION_SNAP_RADIUS = 0.03D;
    static final double CHROMATIC_ABERRATION_STEP = 0.01D;
    static final double COLOR_ADJUSTMENT_SNAP_RADIUS = 0.03D;
    static final double COLOR_ADJUSTMENT_STEP = 0.01D;
    static final double PLAYER_POSITION_OFFSET_MIN = -5.0D;
    static final double PLAYER_POSITION_OFFSET_MAX = 5.0D;
    static final double PLAYER_POSITION_OFFSET_SNAP_RADIUS = 0.08D;
    static final double PLAYER_POSITION_OFFSET_STEP = 0.01D;
    static final double PLAYER_ROTATION_OFFSET_MIN = -180.0D;
    static final double PLAYER_ROTATION_OFFSET_MAX = 180.0D;
    static final double PLAYER_ROTATION_OFFSET_SNAP_RADIUS = 5.0D;
    static final double FOG_INTENSITY_SNAP_RADIUS = 0.05D;
    static final double FOG_DISTANCE_SNAP_RADIUS = 4.0D;
    static final double FOG_DISTANCE_STEP = 1.0D;
    static final double DEPTH_OF_FIELD_FOCUS_DISTANCE_SNAP_RADIUS = 0.05D;
    static final double DEPTH_OF_FIELD_FOCUS_DISTANCE_STEP = 0.01D;
    static final double DEPTH_OF_FIELD_FOCAL_LENGTH_SNAP_RADIUS = 1.0D;
    static final double DEPTH_OF_FIELD_FOCAL_LENGTH_STEP = 1.0D;
    static final double DEPTH_OF_FIELD_APERTURE_SNAP_RADIUS = 0.1D;
    static final double DEPTH_OF_FIELD_APERTURE_STEP = 0.1D;
    private static final double POSE_MAKER_ROTATION_MIN = -180.0D;
    private static final double POSE_MAKER_ROTATION_MAX = 180.0D;
    private static final double POSE_MAKER_ROTATION_SNAP_RADIUS = 5.0D;
    private static final double POSE_MAKER_ROTATION_STEP = 1.0D;
    private static final double POSE_MAKER_MODEL_Y_OFFSET_SNAP_RADIUS = 0.08D;
    private static final double POSE_MAKER_MODEL_Y_OFFSET_STEP = 0.01D;
    private static final long POSE_MAKER_SPACE_SEQUENCE_MILLIS = 2700L;
    private static final String DEFAULT_POSE_MAKER_NAME_KEY = "snappy.photo_mode.pose.custom";

    private Tab selectedTab = Tab.GENERAL;
    @Nullable
    private ConfirmationDialog confirmationDialog;
    private boolean keepPhotoModeOpenAfterRemoval;
    private boolean rotatingView;
    private boolean cameraCursorGrabbed;
    private int panelX;
    private int panelY;
    private int panelHeight;
    private boolean poseMakerOpen;
    int poseMakerPanelX;
    int poseMakerPanelY;
    int poseMakerPanelWidth;
    int poseMakerPanelHeight;
    int poseMakerColumns;
    private boolean poseMakerSpaceDown;
    private int poseMakerSpacePresses;
    private long poseMakerFirstSpacePressMillis;
    String poseMakerNameKey = DEFAULT_POSE_MAKER_NAME_KEY;
    final PoseMakerRotation poseMakerModelRotation = new PoseMakerRotation();
    double poseMakerModelYOffset;
    final Map<PhotoPose.BodyPart, PoseMakerRotation> poseMakerPartRotations = new EnumMap<>(PhotoPose.BodyPart.class);
    private final PhotoModePoseMakerPanel poseMakerPanel = new PhotoModePoseMakerPanel();
    @Nullable
    private LinearLayout tabControlLayout;
    @Nullable
    Button pauseButton;
    @Nullable
    Button gridButton;
    @Nullable
    Button hideSelfButton;
    @Nullable
    Button hideOthersButton;
    @Nullable
    Button poseButton;
    @Nullable
    Button armorButton;
    @Nullable
    Button heldItemsButton;
    @Nullable
    Button timeButton;
    @Nullable
    Button weatherButton;
    @Nullable
    Button seasonButton;
    @Nullable
    Button beaconBeamsButton;
    @Nullable
    Button colorizeButton;
    @Nullable
    Button stylizeButton;
    @Nullable
    Button depthOfFieldButton;
    @Nullable
    PhotoModeSlider depthOfFieldFocalLengthSlider;
    @Nullable
    PhotoModeSlider depthOfFieldApertureSlider;
    @Nullable
    PhotoModeSlider depthOfFieldFocusDistanceSlider;
    @Nullable
    PhotoModeColorButton skyColorButton;
    @Nullable
    PhotoModeColorButton fogColorButton;
    @Nullable
    private PhotoModeColorPicker colorPicker;
    @Nullable
    private ColorPickerTarget colorPickerTarget;
    @Nullable
    EditBox poseMakerNameKeyBox;

    public PhotoModeScreen() {
        super(Component.translatable("snappy.photo_mode.title"));
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
        if (this.handleConfirmationDialogKeyPressed(event)) {
            return true;
        }

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
            this.openConfirmationDialog(Confirmation.LEAVE);
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
        if (this.keepPhotoModeOpenAfterRemoval) {
            this.keepPhotoModeOpenAfterRemoval = false;
            return;
        }
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
            this.poseMakerPanel.addWidgets(this);
        }
        if (this.confirmationDialog != null) {
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
        this.addSettingsButton(y);

        this.tabControlLayout = LinearLayout.vertical().spacing(CONTROL_GAP);
        this.selectedTab.panel().addControls(this);
        this.addTabControlScrollArea();
        this.addActionButtons();
        this.updateButtonMessages();
    }

    private void clearControlReferences() {
        this.tabControlLayout = null;
        this.pauseButton = null;
        this.gridButton = null;
        this.hideSelfButton = null;
        this.hideOthersButton = null;
        this.poseButton = null;
        this.armorButton = null;
        this.heldItemsButton = null;
        this.timeButton = null;
        this.weatherButton = null;
        this.seasonButton = null;
        this.beaconBeamsButton = null;
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

    private void addTabControlScrollArea() {
        LinearLayout controls = this.tabControlLayout;
        if (controls == null) {
            return;
        }

        int bodyHeight = this.tabBodyHeight();
        ScrollableLayout scrollableLayout = new ScrollableLayout(
                this.minecraft,
                controls,
                bodyHeight,
                ScrollableLayout.ReserveStrategy.RIGHT
        );
        scrollableLayout.setScrollbarSpacing(TAB_SCROLLBAR_SPACING);
        scrollableLayout.setMinWidth(this.tabControlWidth());
        scrollableLayout.arrangeElements();
        scrollableLayout.setMaxHeight(bodyHeight);
        scrollableLayout.arrangeElements();
        scrollableLayout.setPosition(this.panelX + PANEL_PADDING, this.tabBodyY());
        scrollableLayout.visitWidgets(this::addRenderableWidget);
    }

    private void addSettingsButton(int y) {
        TexturedIconButton button = this.addRenderableWidget(new TexturedIconButton(
                Component.translatable("snappy.browser.settings"),
                ignored -> this.openOptionsScreen(),
                SETTINGS_ICON
        ));
        button.setPosition(this.panelX + PANEL_WIDTH - PANEL_PADDING - TexturedIconButton.DEFAULT_BUTTON_SIZE, y);
    }

    private void openOptionsScreen() {
        this.keepPhotoModeOpenAfterRemoval = true;
        Minecraft.getInstance().gui.setScreen(new OptionsScreen(this));
    }

    @NotNull
    <T extends AbstractWidget> T addTabControl(@NotNull T widget) {
        LinearLayout controls = this.tabControlLayout;
        if (controls == null) {
            throw new IllegalStateException("Tab controls can only be added while rebuilding the photo mode tab body.");
        }
        controls.addChild(widget);
        return widget;
    }

    @NotNull
    <T extends AbstractWidget> T addPhotoWidget(@NotNull T widget) {
        return this.addRenderableWidget(widget);
    }

    private void addActionButtons() {
        Component takePhotoMessage = this.takePhotoMessage();
        Component returnToPlayerMessage = Component.translatable("snappy.photo_mode.return_to_player");
        Component hideGuiMessage = this.hideGuiMessage();
        Component resetMessage = Component.translatable("snappy.photo_mode.reset");
        Component leaveMessage = Component.translatable("snappy.photo_mode.leave_short");
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
        this.addActionButton(returnToPlayerMessage, x, y, returnToPlayerWidth, button -> PhotoModeManager.returnCameraToPlayer(Minecraft.getInstance()), Component.translatable("snappy.photo_mode.return_to_player.desc"));
        x += returnToPlayerWidth + ACTION_GAP;
        this.addActionButton(hideGuiMessage, x, y, hideGuiWidth, button -> {
            this.openConfirmationDialog(Confirmation.HIDE_GUI);
        }, this.hideGuiDescription());
        x += hideGuiWidth + ACTION_GAP;
        this.addActionButton(resetMessage, x, y, resetWidth, button -> {
            this.openConfirmationDialog(Confirmation.RESET);
        }, resetMessage);
        x += resetWidth + ACTION_GAP;
        this.addActionButton(leaveMessage, x, y, leaveWidth, button -> {
            this.openConfirmationDialog(Confirmation.LEAVE);
        }, Component.translatable("snappy.photo_mode.leave"));
    }

    @NotNull
    PhotoModeColorButton addPhotoColorButton(
            int width,
            @NotNull ColorPickerTarget target,
            @NotNull Supplier<@Nullable Integer> colorSupplier,
            @NotNull IntSupplier editColorSupplier,
            @NotNull Supplier<@Nullable Integer> defaultColorSupplier,
            @NotNull Consumer<@Nullable Integer> colorConsumer
    ) {
        PhotoModeColorButton button = this.addTabControl(new PhotoModeColorButton(
                0,
                0,
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
        this.addRenderableWidget(Button.builder(Component.translatable("snappy.photo_mode.color_picker.reset_default"), button -> {
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

    void addPlayerTransformSlider(
            int width,
            double minValue,
            double maxValue,
            double currentValue,
            double snapRadius,
            double actualStep,
            @NotNull DoubleConsumer valueConsumer,
            @NotNull DoubleFunction<Component> messageFactory
    ) {
        this.addTabControl(new PhotoModeSlider(
                0,
                0,
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

    int addPoseMakerRotationSlider(
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
                        "snappy.photo_mode.pose_maker.rotation",
                        Component.translatable(labelKey),
                        Component.literal(axis.label()),
                        this.degreeValue(value)
                )
        ));
        return index + 1;
    }

    void addPoseMakerModelYOffsetSlider(int x, int y, int width) {
        PhotoModeSlider slider = this.addRenderableWidget(new PhotoModeSlider(
                x,
                y,
                width,
                CONTROL_HEIGHT,
                PhotoPose.MODEL_Y_OFFSET_MIN,
                PhotoPose.MODEL_Y_OFFSET_MAX,
                this.poseMakerModelYOffset,
                0.0D,
                POSE_MAKER_MODEL_Y_OFFSET_SNAP_RADIUS,
                POSE_MAKER_MODEL_Y_OFFSET_STEP,
                value -> {
                    this.setPoseMakerModelYOffset(value);
                    this.syncPoseMakerPreview();
                },
                value -> optionMessage("snappy.photo_mode.pose_maker.model_y_offset", this.poseMakerOffsetValue(value))
        ));
        slider.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.pose_maker.model_y_offset.desc")));
    }

    private void addActionButton(@NotNull Component message, int x, int y, int width, @NotNull Button.OnPress onPress, @NotNull Component tooltip) {
        this.addRenderableWidget(Button.builder(message, onPress)
                .bounds(x, y, width, CONTROL_HEIGHT)
                .tooltip(Tooltip.create(tooltip))
                .build());
    }

    private void addConfirmationWidgets() {
        ConfirmationDialog dialog = this.confirmationDialog;
        if (dialog == null) {
            return;
        }
        int buttonWidth = (this.controlWidth() - CONTROL_GAP) / 2;
        int y = this.panelY + this.panelHeight - PANEL_PADDING - CONTROL_HEIGHT;
        int x = this.panelX + PANEL_PADDING;
        this.addRenderableWidget(Button.builder(dialog.confirmMessage(), button -> this.confirmConfirmationDialog())
                .bounds(x, y, buttonWidth, CONTROL_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.cancelConfirmationDialog())
                .bounds(x + buttonWidth + CONTROL_GAP, y, this.controlWidth() - buttonWidth - CONTROL_GAP, CONTROL_HEIGHT)
                .build());
    }

    private boolean handleConfirmationDialogKeyPressed(@NotNull KeyEvent event) {
        ConfirmationDialog dialog = this.confirmationDialog;
        return dialog != null && dialog.keyPressed(this, event);
    }

    private void openConfirmationDialog(@NotNull Confirmation confirmation) {
        this.closeColorPicker();
        this.confirmationDialog = new ConfirmationDialog(confirmation);
        this.rebuildPhotoWidgets();
    }

    private void confirmConfirmationDialog() {
        ConfirmationDialog dialog = this.confirmationDialog;
        if (dialog == null) {
            return;
        }
        this.confirmationDialog = null;
        dialog.confirm(this);
    }

    private void cancelConfirmationDialog() {
        this.confirmationDialog = null;
        this.rebuildPhotoWidgets();
    }

    void updateButtonMessages() {
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }
        if (this.pauseButton != null) {
            boolean canPause = PhotoModeManager.canPause(Minecraft.getInstance());
            Component value = Component.translatable(active.paused() && canPause
                    ? "snappy.photo_mode.pause.paused"
                    : "snappy.photo_mode.pause.live").withStyle(active.paused() && canPause ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
            this.pauseButton.active = canPause;
            this.pauseButton.setMessage(optionMessage("snappy.photo_mode.pause", value));
            this.pauseButton.setTooltip(Tooltip.create(Component.translatable(canPause
                    ? "snappy.photo_mode.pause.desc"
                    : "snappy.photo_mode.pause.unavailable_server")));
        }
        if (this.gridButton != null) {
            this.gridButton.setMessage(this.gridMessage(active.gridEnabled()));
        }
        if (this.hideSelfButton != null) {
            this.hideSelfButton.setMessage(optionMessage("snappy.photo_mode.hide_self", visibilityValue(!active.hideSelfPlayer())));
        }
        if (this.hideOthersButton != null) {
            this.hideOthersButton.setMessage(optionMessage("snappy.photo_mode.hide_others", visibilityValue(!active.hideOtherPlayers())));
        }
        if (this.poseButton != null) {
            this.poseButton.setMessage(optionMessage("snappy.photo_mode.pose", this.poseValue(active.poseId())));
        }
        if (this.armorButton != null) {
            PhotoModeArmorMode armorMode = active.armorMode();
            this.armorButton.setMessage(optionMessage("snappy.photo_mode.armor", Component.translatable(armorMode.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.heldItemsButton != null) {
            PhotoModeHeldItemsMode heldItemsMode = active.heldItemsMode();
            this.heldItemsButton.setMessage(optionMessage("snappy.photo_mode.held_items", Component.translatable(heldItemsMode.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.timeButton != null) {
            PhotoModeTimePreset preset = active.timePreset();
            this.timeButton.setMessage(optionMessage("snappy.photo_mode.time", Component.translatable(preset.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.weatherButton != null) {
            PhotoModeWeatherPreset preset = active.weatherPreset();
            this.weatherButton.setMessage(optionMessage("snappy.photo_mode.weather", Component.translatable(preset.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.seasonButton != null) {
            PhotoModeSeason season = active.season();
            this.seasonButton.setMessage(optionMessage("snappy.photo_mode.season", Component.translatable(season.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.beaconBeamsButton != null) {
            this.beaconBeamsButton.setMessage(optionMessage("snappy.photo_mode.beacon_beams", enabledValue(active.beaconBeamsEnabled())));
        }
        if (this.colorizeButton != null) {
            PhotoModeColorizePreset preset = active.colorizePreset();
            this.colorizeButton.setMessage(optionMessage("snappy.photo_mode.colorize", Component.translatable(preset.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.stylizeButton != null) {
            PhotoModeStylizePreset preset = active.stylizePreset();
            this.stylizeButton.setMessage(optionMessage("snappy.photo_mode.stylize", Component.translatable(preset.labelKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.depthOfFieldButton != null) {
            this.depthOfFieldButton.setMessage(optionMessage("snappy.photo_mode.depth_of_field", enabledValue(active.depthOfFieldEnabled())));
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
            this.skyColorButton.setMessage(optionMessage("snappy.photo_mode.sky_color", Component.literal(PhotoModeColorPicker.formatHexColor(active.skyColorOverride())).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
        }
        if (this.fogColorButton != null) {
            this.fogColorButton.setMessage(optionMessage("snappy.photo_mode.fog_color", Component.literal(PhotoModeColorPicker.formatHexColor(active.fogColorOverride())).withStyle(Style.EMPTY.withColor(VALUE_COLOR))));
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

        ConfirmationDialog dialog = this.confirmationDialog;
        if (dialog != null) {
            int contentX = this.panelX + PANEL_PADDING;
            int contentY = this.panelY + PANEL_PADDING;
            int width = this.controlWidth();
            graphics.fill(contentX, contentY, contentX + width, this.panelY + this.panelHeight - PANEL_PADDING - CONTROL_HEIGHT - CONTROL_GAP, SECTION_BACKGROUND_COLOR);
            graphics.outline(contentX, contentY, width, this.panelHeight - PANEL_PADDING * 2 - CONTROL_HEIGHT - CONTROL_GAP, PANEL_BORDER_COLOR);
            graphics.centeredText(this.font, dialog.title(), contentX + width / 2, contentY + 14, PANEL_ACCENT_COLOR);
            graphics.textWithWordWrap(this.font, dialog.message(this), contentX + 10, contentY + 36, width - 20, 0xFFFFFFFF);
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
                Component.translatable("snappy.photo_mode.pose_maker.title"),
                this.poseMakerPanelX + this.poseMakerPanelWidth / 2,
                this.poseMakerPanelY + PANEL_PADDING + 3,
                PANEL_ACCENT_COLOR
        );
        graphics.text(
                this.font,
                Component.translatable("snappy.photo_mode.pose_maker.name_key"),
                this.poseMakerPanelX + PANEL_PADDING,
                this.poseMakerNameLabelY(),
                0xFFFFFFFF
        );
    }

    private void updatePanelBounds() {
        int actionRowReserve = this.confirmationDialog == null ? CONTROL_HEIGHT + ACTION_ROW_GAP : 0;
        int availablePanelHeight = Math.max(MIN_TAB_PANEL_HEIGHT, this.height - SCREEN_MARGIN * 2 - actionRowReserve);
        int desiredPanelHeight = this.confirmationDialog != null ? 118 : Math.min(this.selectedTab.panelHeight(), this.maxTabPanelHeight());
        this.panelHeight = Math.min(availablePanelHeight, desiredPanelHeight);
        this.panelX = Math.max(SCREEN_MARGIN, this.width - PANEL_WIDTH - SCREEN_MARGIN);
        this.panelY = Math.max(SCREEN_MARGIN, this.height - this.panelHeight - SCREEN_MARGIN - actionRowReserve);
    }

    private int maxTabPanelHeight() {
        return Math.max(MIN_TAB_PANEL_HEIGHT, this.height / 3);
    }

    private int tabBodyY() {
        return this.panelY + TAB_BODY_TOP_OFFSET;
    }

    private int tabBodyHeight() {
        return Math.max(MIN_TAB_BODY_HEIGHT, this.panelHeight - TAB_BODY_TOP_OFFSET - PANEL_PADDING);
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
        int rows = this.poseMakerSliderRows(columns);
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

    private int poseMakerSliderRows(int columns) {
        return this.poseMakerRotationSliderRows(columns) + 1;
    }

    int poseMakerRotationSliderRows(int columns) {
        return (this.poseMakerRotationSliderCount() + columns - 1) / columns;
    }

    private int poseMakerRotationSliderCount() {
        return (PhotoPose.BodyPart.values().length + 1) * PoseMakerAxis.values().length;
    }

    int poseMakerControlWidth() {
        return this.poseMakerPanelWidth - PANEL_PADDING * 2;
    }

    int poseMakerColumnWidth() {
        return (this.poseMakerControlWidth() - (this.poseMakerColumns - 1) * POSE_MAKER_COLUMN_GAP) / this.poseMakerColumns;
    }

    private int poseMakerNameLabelY() {
        return this.poseMakerPanelY + PANEL_PADDING + POSE_MAKER_HEADER_HEIGHT + CONTROL_GAP;
    }

    int poseMakerNameBoxY() {
        return this.poseMakerNameLabelY() + POSE_MAKER_NAME_LABEL_HEIGHT;
    }

    int poseMakerSliderStartY() {
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
                + this.actionButtonWidth(Component.translatable("snappy.photo_mode.return_to_player"), ACTION_RETURN_TO_PLAYER_MIN_WIDTH)
                + this.actionButtonWidth(this.hideGuiMessage(), ACTION_HIDE_GUI_MIN_WIDTH)
                + this.actionButtonWidth(Component.translatable("snappy.photo_mode.reset"), ACTION_RESET_MIN_WIDTH)
                + this.actionButtonWidth(Component.translatable("snappy.photo_mode.leave_short"), ACTION_LEAVE_MIN_WIDTH)
                + ACTION_GAP * 4;
    }

    @NotNull
    private Component takePhotoMessage() {
        Minecraft minecraft = this.minecraft == null ? Minecraft.getInstance() : this.minecraft;
        return Component.translatable("snappy.photo_mode.take_photo", minecraft.options.keyScreenshot.getTranslatedKeyMessage());
    }

    @NotNull
    private Component gridMessage(boolean enabled) {
        return optionMessage("snappy.photo_mode.grid", KeyMappings.KEY_PHOTO_MODE_TOGGLE_GRID.getTranslatedKeyMessage(), enabledValue(enabled));
    }

    @NotNull
    private Component hideGuiMessage() {
        return Component.translatable("snappy.photo_mode.hide_gui", this.hideGuiKeyMessage());
    }

    @NotNull
    private Component hideGuiDescription() {
        return Component.translatable("snappy.photo_mode.hide_gui.desc", this.hideGuiKeyMessage());
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

    int tabControlWidth() {
        return Math.max(1, this.controlWidth() - TAB_SCROLLBAR_RESERVE);
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
        if (this.confirmationDialog != null) {
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
    static Integer emptyColor() {
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

    void closePoseMaker() {
        this.poseMakerOpen = false;
        this.clearPoseMakerPreview();
        this.rebuildPhotoWidgets();
    }

    void syncPoseMakerPreview() {
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

    void resetPoseMakerSliders() {
        this.poseMakerModelRotation.reset();
        this.poseMakerModelYOffset = 0.0D;
        for (PoseMakerRotation rotation : this.poseMakerPartRotations.values()) {
            rotation.reset();
        }
        this.syncPoseMakerPreview();
        this.rebuildPhotoWidgets();
    }

    void loadPoseMakerPose() {
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
        this.setPoseMakerModelYOffset(pose.modelYOffset());
        for (PhotoPose.BodyPart part : PhotoPose.BodyPart.values()) {
            PoseMakerRotation rotation = this.poseMakerPartRotations.get(part);
            if (rotation != null) {
                rotation.set(pose.rotations().getOrDefault(part, PhotoPose.PartRotation.ZERO));
            }
        }
    }

    @NotNull
    PhotoPose createPoseMakerPose() {
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
                this.poseMakerModelYOffset,
                Map.copyOf(rotations)
        );
    }

    private void setPoseMakerModelYOffset(double value) {
        this.poseMakerModelYOffset = PhotoPose.clampModelYOffset(value);
    }

    @NotNull
    private String poseMakerNameKey() {
        String key = this.poseMakerNameKey.trim();
        return key.isEmpty() ? DEFAULT_POSE_MAKER_NAME_KEY : key;
    }

    private void setPhotoModeUiHidden(boolean hidden) {
        this.confirmationDialog = null;
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
    static Component optionMessage(@NotNull String key, @NotNull Component value) {
        return UIFormatting.optionMessage(key, value);
    }

    @NotNull
    static Component optionMessage(@NotNull String key, @NotNull Object... args) {
        return UIFormatting.optionMessage(key, args);
    }

    @NotNull
    static Component signedPercentValue(double value) {
        return UIFormatting.signedPercentValue("snappy.photo_mode.percent", value, VALUE_COLOR);
    }

    @NotNull
    private static Component visibilityValue(boolean visible) {
        return UIFormatting.visibleHiddenValue(visible, "snappy.photo_mode.visible", "snappy.photo_mode.hidden");
    }

    @NotNull
    private static Component enabledValue(boolean enabled) {
        return UIFormatting.enabledDisabledValue(enabled, "snappy.photo_mode.enabled", "snappy.photo_mode.disabled");
    }

    @NotNull
    private Component poseValue(@Nullable Identifier poseId) {
        if (poseId == null) {
            return Component.translatable("snappy.photo_mode.pose.none").withStyle(Style.EMPTY.withColor(VALUE_COLOR));
        }
        PhotoPose pose = PhotoPoseManager.pose(poseId);
        return Component.translatable(pose == null ? "snappy.photo_mode.pose.none" : pose.nameKey()).withStyle(Style.EMPTY.withColor(VALUE_COLOR));
    }

    @NotNull
    Component blockValue(double value) {
        return UIFormatting.fixedTranslatable("snappy.photo_mode.blocks", value, "%.2f", VALUE_COLOR);
    }

    @NotNull
    Component poseMakerOffsetValue(double value) {
        return UIFormatting.fixedLiteral(value, "%+.2f", VALUE_COLOR);
    }

    @NotNull
    Component degreeValue(double value) {
        return UIFormatting.fixedTranslatable("snappy.photo_mode.degrees", value, "%.0f", VALUE_COLOR);
    }

    private enum Tab {
        GENERAL(GENERAL_ICON, "snappy.photo_mode.tab.general", 363, new PhotoModeGeneralTabPanel()),
        LENS(LENS_ICON, "snappy.photo_mode.tab.lens", 138, new PhotoModeLensTabPanel()),
        PLAYER(PLAYER_ICON, "snappy.photo_mode.tab.player", 313, new PhotoModeActorAppearancePanel()),
        WORLD(WORLD_ICON, "snappy.photo_mode.tab.world", 238, new PhotoModeEnvironmentPanel());

        private final Identifier icon;
        private final String labelKey;
        private final int panelHeight;
        private final PhotoModeTabPanel panel;

        Tab(@NotNull Identifier icon, @NotNull String labelKey, int panelHeight, @NotNull PhotoModeTabPanel panel) {
            this.icon = icon;
            this.labelKey = labelKey;
            this.panelHeight = panelHeight;
            this.panel = panel;
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

        @NotNull
        private PhotoModeTabPanel panel() {
            return this.panel;
        }
    }

    enum PoseMakerAxis {
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

    static final class PoseMakerRotation {

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

    private static final class ConfirmationDialog {

        private final Confirmation confirmation;

        private ConfirmationDialog(@NotNull Confirmation confirmation) {
            this.confirmation = confirmation;
        }

        private boolean keyPressed(@NotNull PhotoModeScreen screen, @NotNull KeyEvent event) {
            if (event.isEscape()) {
                screen.cancelConfirmationDialog();
                return true;
            }
            if (event.isConfirmation()) {
                screen.confirmConfirmationDialog();
                return true;
            }
            return false;
        }

        @NotNull
        private Component title() {
            return this.confirmation.title();
        }

        @NotNull
        private Component message(@NotNull PhotoModeScreen screen) {
            return this.confirmation.message(screen);
        }

        @NotNull
        private Component confirmMessage() {
            return this.confirmation.confirmMessage();
        }

        private void confirm(@NotNull PhotoModeScreen screen) {
            this.confirmation.confirm(screen);
        }
    }

    private enum Confirmation {
        HIDE_GUI("snappy.photo_mode.confirm.hide_gui.title", "snappy.photo_mode.confirm.hide_gui.message", "snappy.photo_mode.confirm.hide_gui.confirm") {
            @Override
            @NotNull
            Component message(@NotNull PhotoModeScreen screen) {
                return Component.translatable(this.messageKey, screen.hideGuiKeyMessage());
            }

            @Override
            void confirm(@NotNull PhotoModeScreen screen) {
                screen.setPhotoModeUiHidden(true);
            }
        },
        RESET("snappy.photo_mode.confirm.reset.title", "snappy.photo_mode.confirm.reset.message", "snappy.photo_mode.confirm.reset.confirm") {
            @Override
            void confirm(@NotNull PhotoModeScreen screen) {
                PhotoModeManager.resetToDefaults(Minecraft.getInstance());
                screen.rebuildPhotoWidgets();
            }
        },
        LEAVE("snappy.photo_mode.confirm.leave.title", "snappy.photo_mode.confirm.leave.message", "snappy.photo_mode.confirm.leave.confirm") {
            @Override
            void confirm(@NotNull PhotoModeScreen screen) {
                PhotoModeManager.close();
                Minecraft.getInstance().gui.setScreen(null);
            }
        };

        private final String titleKey;
        protected final String messageKey;
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
        Component message(@NotNull PhotoModeScreen screen) {
            return Component.translatable(this.messageKey);
        }

        @NotNull
        private Component confirmMessage() {
            return Component.translatable(this.confirmKey);
        }

        abstract void confirm(@NotNull PhotoModeScreen screen);
    }

    enum ColorPickerTarget {
        SKY("snappy.photo_mode.sky_color_picker"),
        FOG("snappy.photo_mode.fog_color_picker");

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
