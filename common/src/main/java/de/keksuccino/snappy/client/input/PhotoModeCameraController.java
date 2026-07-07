package de.keksuccino.snappy.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.snappy.KeyMappings;
import de.keksuccino.snappy.platform.Services;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class PhotoModeCameraController {

    private static final float CAMERA_SPEED_BLOCKS_PER_SECOND = 9.0F;
    private static final float CAMERA_FAST_SPEED_MULTIPLIER = 3.0F;
    private static final float CAMERA_SLOW_SPEED_MULTIPLIER = 0.35F;
    private static final float MOUSE_ROTATION_SENSITIVITY = 0.16F;
    private static final double INITIAL_CAMERA_DISTANCE = 3.0D;
    private static final double SCROLL_ZOOM_SECONDS_PER_NOTCH = 0.08D;
    private static final double MAX_SCROLL_ZOOM_NOTCHES = 4.0D;
    private static final double MAX_FRAME_SECONDS = 0.1D;

    private final Set<InputConstants.Key> activeBoundInputs = new HashSet<>();
    private long lastMovementMillis = Util.getMillis();

    public void updateMovement(@NotNull Minecraft minecraft, @NotNull MutableCamera camera) {
        long now = Util.getMillis();
        double deltaSeconds = Math.min(MAX_FRAME_SECONDS, Math.max(0.0D, (now - this.lastMovementMillis) / 1_000.0D));
        this.lastMovementMillis = now;
        if (deltaSeconds <= 0.0D) {
            return;
        }

        float forwardAxis = this.axis(minecraft, minecraft.options.keyUp, minecraft.options.keyDown);
        float strafeAxis = this.axis(minecraft, minecraft.options.keyRight, minecraft.options.keyLeft);
        float verticalAxis = 0.0F;
        if (this.isBoundKeyDown(minecraft, minecraft.options.keyJump)) {
            verticalAxis += 1.0F;
        }
        if (this.isBoundKeyDown(minecraft, minecraft.options.keyShift)) {
            verticalAxis -= 1.0F;
        }

        if (forwardAxis == 0.0F && strafeAxis == 0.0F && verticalAxis == 0.0F) {
            return;
        }

        Vec3 forward = this.forwardVector(camera);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        if (right.lengthSqr() < 1.0E-7D) {
            right = Vec3.directionFromRotation(0.0F, camera.yaw() + 90.0F);
        } else {
            right = right.normalize();
        }
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 movement = forward.scale(forwardAxis).add(right.scale(strafeAxis)).add(up.scale(verticalAxis));
        if (movement.lengthSqr() > 1.0D) {
            movement = movement.normalize();
        }
        camera.setPosition(camera.position().add(movement.scale(this.movementSpeed(minecraft) * deltaSeconds)));
    }

    public void zoomFromScroll(@NotNull Minecraft minecraft, @NotNull MutableCamera camera, double scrollY) {
        if (!Double.isFinite(scrollY) || scrollY == 0.0D) {
            return;
        }
        double notches = Mth.clamp(scrollY, -MAX_SCROLL_ZOOM_NOTCHES, MAX_SCROLL_ZOOM_NOTCHES);
        camera.setPosition(camera.position().add(this.forwardVector(camera).scale(this.movementSpeed(minecraft) * SCROLL_ZOOM_SECONDS_PER_NOTCH * notches)));
        this.markMoved();
    }

    public void setBoundInputState(@NotNull InputConstants.Key key, boolean down) {
        if (down) {
            this.activeBoundInputs.add(key);
        } else {
            this.activeBoundInputs.remove(key);
        }
    }

    public void rotate(@NotNull MutableCamera camera, double dx, double dy) {
        camera.setYaw(Mth.wrapDegrees(camera.yaw() + (float) dx * MOUSE_ROTATION_SENSITIVITY));
        camera.setPitch(Mth.clamp(camera.pitch() + (float) dy * MOUSE_ROTATION_SENSITIVITY, -89.5F, 89.5F));
    }

    @NotNull
    public Vec3 forwardVector(@NotNull MutableCamera camera) {
        return Vec3.directionFromRotation(camera.pitch(), camera.yaw()).normalize();
    }

    public void resetInputState() {
        this.activeBoundInputs.clear();
        this.markMoved();
    }

    public void markMoved() {
        this.lastMovementMillis = Util.getMillis();
    }

    @NotNull
    public static CameraStart facingPlayer(@NotNull Player player) {
        Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.78D, 0.0D);
        Vec3 playerForward = Vec3.directionFromRotation(0.0F, player.getVisualRotationYInDegrees()).normalize();
        Vec3 position = player.position()
                .add(0.0D, player.getEyeHeight(), 0.0D)
                .add(playerForward.scale(INITIAL_CAMERA_DISTANCE));
        Vec2 rotation = target.subtract(position).rotation();
        return new CameraStart(position, rotation.y, rotation.x);
    }

    private float axis(@NotNull Minecraft minecraft, @NotNull KeyMapping positiveKey, @NotNull KeyMapping negativeKey) {
        float axis = 0.0F;
        if (this.isBoundKeyDown(minecraft, positiveKey)) {
            axis += 1.0F;
        }
        if (this.isBoundKeyDown(minecraft, negativeKey)) {
            axis -= 1.0F;
        }
        return axis;
    }

    private float movementSpeed(@NotNull Minecraft minecraft) {
        float speed = CAMERA_SPEED_BLOCKS_PER_SECOND;
        if (this.isBoundKeyDown(minecraft, minecraft.options.keySprint)) {
            speed *= CAMERA_FAST_SPEED_MULTIPLIER;
        }
        if (this.isBoundKeyDown(minecraft, KeyMappings.KEY_PHOTO_MODE_SLOW_CAMERA)) {
            speed *= CAMERA_SLOW_SPEED_MULTIPLIER;
        }
        return speed;
    }

    private boolean isBoundKeyDown(@NotNull Minecraft minecraft, @NotNull KeyMapping keyMapping) {
        InputConstants.Key key = Services.PLATFORM.getKeyMappingKey(keyMapping);
        if (key.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(minecraft.getWindow(), key.getValue());
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
        }
        return this.activeBoundInputs.contains(key) || keyMapping.isDown();
    }

    public interface MutableCamera {

        @NotNull
        Vec3 position();

        void setPosition(@NotNull Vec3 position);

        float yaw();

        void setYaw(float yaw);

        float pitch();

        void setPitch(float pitch);

    }

    public record CameraStart(@NotNull Vec3 position, float yaw, float pitch) {
    }

}
