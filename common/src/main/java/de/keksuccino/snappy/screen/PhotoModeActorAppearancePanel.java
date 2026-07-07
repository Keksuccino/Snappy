package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.client.gui.components.Button;
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

        screen.hideSelfButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setHideSelfPlayer(!active.hideSelfPlayer());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).build());

        screen.hideOthersButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setHideOtherPlayers(!active.hideOtherPlayers());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).build());

        screen.poseButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.cyclePose();
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.pose.desc"))).build());

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

        screen.armorButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setArmorMode(active.armorMode().next());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.armor.desc"))).build());

        screen.heldItemsButton = screen.addTabControl(Button.builder(Component.empty(), button -> {
            active.setHeldItemsMode(active.heldItemsMode().next());
            screen.updateButtonMessages();
        }).bounds(0, 0, width, PhotoModeScreen.CONTROL_HEIGHT).tooltip(Tooltip.create(Component.translatable("snappy.photo_mode.held_items.desc"))).build());
    }

}
