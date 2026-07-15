package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.photo.PhotoModeManager;
import de.keksuccino.snappy.util.rendering.gui.widget.SnappyButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

final class PhotoModeActorAppearancePanel implements PhotoModeTabPanel {

    @Override
    public void addControls(@NotNull PhotoModeScreen screen) {
        int width = screen.tabControlWidth();
        PhotoModeManager.Session active = PhotoModeManager.session();
        if (active == null) {
            return;
        }

        screen.hideSelfButton = screen.addTabControl(new SnappyButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setHideSelfPlayer(!active.hideSelfPlayer());
            screen.updateButtonMessages();
        }));

        screen.hideOthersButton = screen.addTabControl(new SnappyButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setHideOtherPlayers(!active.hideOtherPlayers());
            screen.updateButtonMessages();
        }));

        SnappyButton poseButton = new SnappyButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.cyclePose();
            screen.updateButtonMessages();
        });
        poseButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.pose.desc")));
        screen.poseButton = screen.addTabControl(poseButton);

        screen.addPlayerTransformSlider(
                width,
                PhotoModeScreen.PLAYER_POSITION_OFFSET_MIN,
                PhotoModeScreen.PLAYER_POSITION_OFFSET_MAX,
                active.selfPlayerPositionOffsetX(),
                PhotoModeScreen.PLAYER_POSITION_OFFSET_SNAP_RADIUS,
                PhotoModeScreen.PLAYER_POSITION_OFFSET_STEP,
                active::setSelfPlayerPositionOffsetX,
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.player_offset_x", screen.blockValue(value))
        );

        screen.addPlayerTransformSlider(
                width,
                PhotoModeScreen.PLAYER_POSITION_OFFSET_MIN,
                PhotoModeScreen.PLAYER_POSITION_OFFSET_MAX,
                active.selfPlayerPositionOffsetY(),
                PhotoModeScreen.PLAYER_POSITION_OFFSET_SNAP_RADIUS,
                PhotoModeScreen.PLAYER_POSITION_OFFSET_STEP,
                active::setSelfPlayerPositionOffsetY,
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.player_offset_y", screen.blockValue(value))
        );

        screen.addPlayerTransformSlider(
                width,
                PhotoModeScreen.PLAYER_POSITION_OFFSET_MIN,
                PhotoModeScreen.PLAYER_POSITION_OFFSET_MAX,
                active.selfPlayerPositionOffsetZ(),
                PhotoModeScreen.PLAYER_POSITION_OFFSET_SNAP_RADIUS,
                PhotoModeScreen.PLAYER_POSITION_OFFSET_STEP,
                active::setSelfPlayerPositionOffsetZ,
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.player_offset_z", screen.blockValue(value))
        );

        screen.addPlayerTransformSlider(
                width,
                PhotoModeScreen.PLAYER_ROTATION_OFFSET_MIN,
                PhotoModeScreen.PLAYER_ROTATION_OFFSET_MAX,
                active.selfPlayerRotationOffsetX(),
                PhotoModeScreen.PLAYER_ROTATION_OFFSET_SNAP_RADIUS,
                0.0D,
                active::setSelfPlayerRotationOffsetX,
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.player_rotation_x", screen.degreeValue(value))
        );

        screen.addPlayerTransformSlider(
                width,
                PhotoModeScreen.PLAYER_ROTATION_OFFSET_MIN,
                PhotoModeScreen.PLAYER_ROTATION_OFFSET_MAX,
                active.selfPlayerRotationOffsetY(),
                PhotoModeScreen.PLAYER_ROTATION_OFFSET_SNAP_RADIUS,
                0.0D,
                active::setSelfPlayerRotationOffsetY,
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.player_rotation_y", screen.degreeValue(value))
        );

        screen.addPlayerTransformSlider(
                width,
                PhotoModeScreen.PLAYER_ROTATION_OFFSET_MIN,
                PhotoModeScreen.PLAYER_ROTATION_OFFSET_MAX,
                active.selfPlayerRotationOffsetZ(),
                PhotoModeScreen.PLAYER_ROTATION_OFFSET_SNAP_RADIUS,
                0.0D,
                active::setSelfPlayerRotationOffsetZ,
                value -> PhotoModeScreen.optionMessage("snappy.photo_mode.player_rotation_z", screen.degreeValue(value))
        );

        SnappyButton armorButton = new SnappyButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setArmorMode(active.armorMode().next());
            screen.updateButtonMessages();
        });
        armorButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.armor.desc")));
        screen.armorButton = screen.addTabControl(armorButton);

        SnappyButton heldItemsButton = new SnappyButton(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT, Component.empty(), ignored -> {
            active.setHeldItemsMode(active.heldItemsMode().next());
            screen.updateButtonMessages();
        });
        heldItemsButton.setTooltip(Tooltip.create(Component.translatable("snappy.photo_mode.held_items.desc")));
        screen.heldItemsButton = screen.addTabControl(heldItemsButton);
    }

}
