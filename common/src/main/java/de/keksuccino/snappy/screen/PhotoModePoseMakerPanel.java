package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.photo.PhotoPose;
import de.keksuccino.snappy.photo.PhotoPoseExporter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
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
                sliderY + screen.poseMakerRotationSliderRows(screen.poseMakerColumns) * (PhotoModeScreen.CONTROL_HEIGHT + PhotoModeScreen.CONTROL_GAP),
                contentWidth
        );

        int buttonY = screen.poseMakerPanelY + screen.poseMakerPanelHeight - PhotoModeScreen.PANEL_PADDING - PhotoModeScreen.CONTROL_HEIGHT;
        int secondaryButtonY = buttonY - PhotoModeScreen.POSE_MAKER_BUTTON_GAP - PhotoModeScreen.CONTROL_HEIGHT;
        int buttonWidth = (contentWidth - PhotoModeScreen.POSE_MAKER_BUTTON_GAP) / 2;
        screen.addPhotoWidget(Button.builder(Component.translatable("snappy.photo_mode.pose_maker.reset"), button -> screen.resetPoseMakerSliders())
                .bounds(contentX, secondaryButtonY, buttonWidth, PhotoModeScreen.CONTROL_HEIGHT)
                .build());
        screen.addPhotoWidget(Button.builder(Component.translatable("snappy.photo_mode.pose_maker.load"), button -> screen.loadPoseMakerPose())
                .bounds(contentX + buttonWidth + PhotoModeScreen.POSE_MAKER_BUTTON_GAP, secondaryButtonY, contentWidth - buttonWidth - PhotoModeScreen.POSE_MAKER_BUTTON_GAP, PhotoModeScreen.CONTROL_HEIGHT)
                .build());
        screen.addPhotoWidget(Button.builder(Component.translatable("snappy.photo_mode.pose_maker.save"), button -> PhotoPoseExporter.saveWithNativeDialog(Minecraft.getInstance(), screen.createPoseMakerPose()))
                .bounds(contentX, buttonY, buttonWidth, PhotoModeScreen.CONTROL_HEIGHT)
                .build());
        screen.addPhotoWidget(Button.builder(Component.translatable("snappy.photo_mode.pose_maker.close"), button -> screen.closePoseMaker())
                .bounds(contentX + buttonWidth + PhotoModeScreen.POSE_MAKER_BUTTON_GAP, buttonY, contentWidth - buttonWidth - PhotoModeScreen.POSE_MAKER_BUTTON_GAP, PhotoModeScreen.CONTROL_HEIGHT)
                .build());
    }

}
