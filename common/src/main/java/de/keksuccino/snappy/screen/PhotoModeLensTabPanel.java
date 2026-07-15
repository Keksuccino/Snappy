package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.photo.PhotoModeManager;
import de.keksuccino.snappy.util.rendering.gui.widget.SnappyButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

final class PhotoModeLensTabPanel implements PhotoModeTabPanel {

    @Override
    public void addControls(@NotNull PhotoModeScreen screen) {
        int width = screen.tabControlWidth();
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }

        SnappyButton depthOfFieldButton = new SnappyButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setDepthOfFieldEnabled(!active.depthOfFieldEnabled());
            screen.updateButtonMessages();
        });
        depthOfFieldButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.depth_of_field.desc")));
        screen.depthOfFieldButton = screen.addTabControl(depthOfFieldButton);

        screen.depthOfFieldFocalLengthSlider = screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                PhotoModeScreen.CONTROL_HEIGHT,
                PhotoModeManager.DEPTH_OF_FIELD_FOCAL_LENGTH_MIN,
                PhotoModeManager.DEPTH_OF_FIELD_FOCAL_LENGTH_MAX,
                active.depthOfFieldFocalLength(),
                PhotoModeManager.DEPTH_OF_FIELD_FOCAL_LENGTH_DEFAULT,
                PhotoModeScreen.DEPTH_OF_FIELD_FOCAL_LENGTH_SNAP_RADIUS,
                PhotoModeScreen.DEPTH_OF_FIELD_FOCAL_LENGTH_STEP,
                value -> active.setDepthOfFieldFocalLength((float) value),
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.dof_focal_length", Component.translatable("snappy.photo_mode.millimeters", String.format(Locale.ROOT, "%.0f", value)).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR)))
        ));
        screen.depthOfFieldFocalLengthSlider.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.dof_focal_length.desc")));

        screen.depthOfFieldApertureSlider = screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                PhotoModeScreen.CONTROL_HEIGHT,
                PhotoModeManager.DEPTH_OF_FIELD_APERTURE_MIN,
                PhotoModeManager.DEPTH_OF_FIELD_APERTURE_MAX,
                active.depthOfFieldAperture(),
                PhotoModeManager.DEPTH_OF_FIELD_APERTURE_DEFAULT,
                PhotoModeScreen.DEPTH_OF_FIELD_APERTURE_SNAP_RADIUS,
                PhotoModeScreen.DEPTH_OF_FIELD_APERTURE_STEP,
                value -> active.setDepthOfFieldAperture((float) value),
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.dof_aperture", Component.translatable("snappy.photo_mode.aperture", String.format(Locale.ROOT, "%.1f", value)).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR)))
        ));
        screen.depthOfFieldApertureSlider.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.dof_aperture.desc")));

        screen.depthOfFieldFocusDistanceSlider = screen.addTabControl(new PhotoModeSlider(
                0,
                0,
                width,
                PhotoModeScreen.CONTROL_HEIGHT,
                PhotoModeManager.DEPTH_OF_FIELD_FOCUS_DISTANCE_MIN,
                PhotoModeManager.DEPTH_OF_FIELD_FOCUS_DISTANCE_MAX,
                active.depthOfFieldFocusDistance(),
                PhotoModeManager.DEPTH_OF_FIELD_FOCUS_DISTANCE_DEFAULT,
                PhotoModeScreen.DEPTH_OF_FIELD_FOCUS_DISTANCE_SNAP_RADIUS,
                PhotoModeScreen.DEPTH_OF_FIELD_FOCUS_DISTANCE_STEP,
                value -> active.setDepthOfFieldFocusDistance((float) value),
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.dof_focus_distance", Component.translatable("snappy.photo_mode.blocks", String.format(Locale.ROOT, "%.2f", value)).withStyle(Style.EMPTY.withColor(PhotoModeScreen.VALUE_COLOR)))
        ));
        screen.depthOfFieldFocusDistanceSlider.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.dof_focus_distance.desc")));
    }

}
