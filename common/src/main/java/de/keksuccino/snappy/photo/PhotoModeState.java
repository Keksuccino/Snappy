package de.keksuccino.snappy.photo;

import de.keksuccino.snappy.client.render.config.BloomConfig;
import de.keksuccino.snappy.client.render.config.ColorAdjustmentConfig;
import de.keksuccino.snappy.client.render.config.DepthOfFieldConfig;
import de.keksuccino.snappy.client.render.config.StylizeConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PhotoModeState(
        @NotNull PhotoModeManager.CameraState camera,
        float fieldOfView,
        float vignette,
        @NotNull PhotoModeColorizePreset colorizePreset,
        @NotNull ColorAdjustmentConfig colorAdjustment,
        @NotNull BloomConfig bloom,
        @NotNull StylizeConfig stylize,
        @NotNull DepthOfFieldConfig depthOfField,
        @NotNull Vec3 selfPlayerPositionOffset,
        @NotNull Vec3 selfPlayerRotationOffset,
        boolean hideSelfPlayer,
        boolean hideOtherPlayers,
        @NotNull PhotoModeArmorMode armorMode,
        @NotNull PhotoModeHeldItemsMode heldItemsMode,
        boolean hideBeaconBeams,
        boolean paused,
        boolean photoModeUiHidden,
        boolean gridEnabled,
        @NotNull PhotoModeTimePreset timePreset,
        @NotNull PhotoModeWeatherPreset weatherPreset,
        float fogIntensity,
        float fogDistance,
        int fogColor,
        int skyColor,
        @Nullable Identifier poseId
) {
}
