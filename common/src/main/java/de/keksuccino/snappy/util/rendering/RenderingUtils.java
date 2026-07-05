package de.keksuccino.snappy.util.rendering;

import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;

public class RenderingUtils {

    public static boolean isVulkanActive() {
        return RenderUtils.isVulkanActive();
    }

    public static void renderBorder(@NotNull GuiGraphicsExtractor graphics, int x, int y, int width, int height, int thickness, int color) {
        if (width <= 0 || height <= 0 || thickness <= 0) {
            return;
        }

        int borderThickness = Math.min(thickness, Math.min(width, height));
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, y + borderThickness, color);
        graphics.fill(x, bottom - borderThickness, right, bottom, color);
        if (height <= borderThickness * 2) {
            return;
        }

        graphics.fill(x, y + borderThickness, x + borderThickness, bottom - borderThickness, color);
        graphics.fill(right - borderThickness, y + borderThickness, right, bottom - borderThickness, color);
    }

}
