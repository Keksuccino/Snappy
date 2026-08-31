package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.photo.PhotoPose;
import de.keksuccino.snappy.photo.PhotoPoseExporter;
import de.keksuccino.snappy.util.rendering.gui.widget.SnappyButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

final class PhotoModePoseMakerPanel {

    void addWidgets(@NotNull PhotoModeScreen screen) {
        int contentX = screen.poseMakerPanelX + PhotoModeScreen.PANEL_PADDING;
        int contentWidth = screen.poseMakerControlWidth();
        int y = screen.poseMakerNameBoxY();

        screen.poseMakerNameKeyBox = screen.addPhotoWidget(new EditBox(
                Minecraft.getInstance().font,
                contentX,
                y,
                contentWidth,
                PhotoModeScreen.CONTROL_HEIGHT,
                Component.translatable("snappy.photo_mode.pose_maker.name_key")
        ));
        screen.poseMakerNameKeyBox.setMaxLength(256);
        screen.poseMakerNameKeyBox.setHint(Component.translatable("snappy.photo_mode.pose_maker.name_key_hint"));
        screen.poseMakerNameKeyBox.setValue(screen.poseMakerNameKey);
        screen.poseMakerNameKeyBox.setResponder(value -> {
            screen.poseMakerNameKey = value;
            screen.syncPoseMakerPreview();
        });

        int columnWidth = screen.poseMakerColumnWidth();
        int sliderY = screen.poseMakerSliderStartY();
        int index = 0;
        for (PhotoModeScreen.PoseMakerAxis axis : PhotoModeScreen.PoseMakerAxis.values()) {
            index = screen.addPoseMakerRotationSlider(
                    index,
                    sliderY,
                    columnWidth,
                    "snappy.photo_mode.pose_maker.part.model",
                    screen.poseMakerModelRotation,
                    axis
            );
        }
        for (PhotoPose.BodyPart part : PhotoPose.BodyPart.values()) {
            PhotoModeScreen.PoseMakerRotation rotation = screen.poseMakerPartRotations.get(part);
            if (rotation == null) {
                continue;
            }
            for (PhotoModeScreen.PoseMakerAxis axis : PhotoModeScreen.PoseMakerAxis.values()) {
                index = screen.addPoseMakerRotationSlider(index, sliderY, columnWidth, part.labelKey(), rotation, axis);
            }
        }
        screen.addPoseMakerModelYOffsetSlider(
                contentX,
                sliderY + screen.poseMakerRotationSliderRows(screen.poseMakerColumns) * (PhotoModeScreen.SLIDER_HEIGHT + PhotoModeScreen.CONTROL_GAP),
                contentWidth
        );

        int buttonY = screen.poseMakerPanelY + screen.poseMakerPanelHeight - PhotoModeScreen.PANEL_PADDING - PhotoModeScreen.CONTROL_HEIGHT;
        int secondaryButtonY = buttonY - PhotoModeScreen.POSE_MAKER_BUTTON_GAP - PhotoModeScreen.CONTROL_HEIGHT;
        int buttonWidth = (contentWidth - PhotoModeScreen.POSE_MAKER_BUTTON_GAP) / 2;
        screen.addPhotoWidget(new SnappyButton(contentX, secondaryButtonY, buttonWidth, Component.translatable("snappy.photo_mode.pose_maker.reset"), ignored -> screen.resetPoseMakerSliders()));
        screen.addPhotoWidget(new SnappyButton(contentX + buttonWidth + PhotoModeScreen.POSE_MAKER_BUTTON_GAP, secondaryButtonY, contentWidth - buttonWidth - PhotoModeScreen.POSE_MAKER_BUTTON_GAP, Component.translatable("snappy.photo_mode.pose_maker.load"), ignored -> screen.loadPoseMakerPose()));
        screen.addPhotoWidget(new SnappyButton(contentX, buttonY, buttonWidth, Component.translatable("snappy.photo_mode.pose_maker.save"), ignored -> PhotoPoseExporter.saveWithNativeDialog(Minecraft.getInstance(), screen.createPoseMakerPose())));
        screen.addPhotoWidget(new SnappyButton(contentX + buttonWidth + PhotoModeScreen.POSE_MAKER_BUTTON_GAP, buttonY, contentWidth - buttonWidth - PhotoModeScreen.POSE_MAKER_BUTTON_GAP, Component.translatable("snappy.photo_mode.pose_maker.close"), ignored -> screen.closePoseMaker()));
    }

}
