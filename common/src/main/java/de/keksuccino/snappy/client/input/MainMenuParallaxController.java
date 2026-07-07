package de.keksuccino.snappy.client.input;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.snappy.Snappy;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

public final class MainMenuParallaxController {

    private static final float X_ROTATION_RANGE = 3.0F;
    private static final float Y_ROTATION_RANGE = 5.0F;
    private static final float SMOOTHING = 0.12F;
    private static final long SPIN_RESUME_DELAY_MS = 1_000L;
    private static final double MOUSE_MOVEMENT_EPSILON = 0.001D;

    private static float xRotationOffset;
    private static float yRotationOffset;
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;
    private static long lastMouseMoveMillis;

    private MainMenuParallaxController() {
    }

    public static void update() {
        if (!Snappy.getOptions().isMenuPanoramaParallaxEnabled()) {
            reset();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        int width = Math.max(1, window.getScreenWidth());
        int height = Math.max(1, window.getScreenHeight());

        double rawMouseX = minecraft.mouseHandler.xpos();
        double rawMouseY = minecraft.mouseHandler.ypos();
        if (!isMouseInsideWindow(window, rawMouseX, rawMouseY)) {
            settle();
            resetMouseTracking();
            return;
        }

        updateMouseMovement(rawMouseX, rawMouseY);

        float mouseX = normalizeMousePosition(rawMouseX, width);
        float mouseY = normalizeMousePosition(rawMouseY, height);
        xRotationOffset = Mth.lerp(SMOOTHING, xRotationOffset, mouseY * X_ROTATION_RANGE);
        yRotationOffset = Mth.lerp(SMOOTHING, yRotationOffset, mouseX * Y_ROTATION_RANGE);
    }

    public static boolean shouldSpin() {
        if (!Snappy.getOptions().isMenuPanoramaParallaxEnabled()) {
            reset();
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        double rawMouseX = minecraft.mouseHandler.xpos();
        double rawMouseY = minecraft.mouseHandler.ypos();
        if (!isMouseInsideWindow(window, rawMouseX, rawMouseY)) {
            resetMouseTracking();
            return true;
        }

        updateMouseMovement(rawMouseX, rawMouseY);
        return Util.getMillis() - lastMouseMoveMillis >= SPIN_RESUME_DELAY_MS;
    }

    public static float applyXRotation(float rotXInDegrees) {
        return rotXInDegrees + xRotationOffset;
    }

    public static float applyYRotation(float rotYInDegrees) {
        return rotYInDegrees + yRotationOffset;
    }

    public static void reset() {
        xRotationOffset = 0.0F;
        yRotationOffset = 0.0F;
        resetMouseTracking();
    }

    private static float normalizeMousePosition(double position, int size) {
        return Mth.clamp((float) ((position / size) * 2.0D - 1.0D), -1.0F, 1.0F);
    }

    private static boolean isMouseInsideWindow(@NotNull Window window, double mouseX, double mouseY) {
        return mouseX >= 0.0D && mouseY >= 0.0D && mouseX < window.getScreenWidth() && mouseY < window.getScreenHeight();
    }

    private static void updateMouseMovement(double mouseX, double mouseY) {
        if (!Double.isNaN(lastMouseX) && !Double.isNaN(lastMouseY)
                && (Math.abs(mouseX - lastMouseX) > MOUSE_MOVEMENT_EPSILON
                || Math.abs(mouseY - lastMouseY) > MOUSE_MOVEMENT_EPSILON)) {
            lastMouseMoveMillis = Util.getMillis();
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    private static void settle() {
        xRotationOffset = Mth.lerp(SMOOTHING, xRotationOffset, 0.0F);
        yRotationOffset = Mth.lerp(SMOOTHING, yRotationOffset, 0.0F);
    }

    private static void resetMouseTracking() {
        lastMouseX = Double.NaN;
        lastMouseY = Double.NaN;
        lastMouseMoveMillis = 0L;
    }

}
