package de.keksuccino.snappy.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PhotoModeColorSwatch {

    static final int SIZE = 10;
    static final int BORDER_COLOR = ARGB.color(255, 20, 20, 20);
    static final int INNER_BORDER_COLOR = ARGB.color(255, 235, 235, 235);
    static final int EMPTY_BACKGROUND_COLOR = ARGB.color(82, 24, 28, 34);
    static final int EMPTY_LINE_COLOR = ARGB.color(255, 255, 209, 102);

    private PhotoModeColorSwatch() {
    }

    static void render(@NotNull GuiGraphicsExtractor graphics, int x, int y, @Nullable Integer color) {
        graphics.fill(x - 1, y - 1, x + SIZE + 1, y + SIZE + 1, BORDER_COLOR);
        if (color == null) {
            graphics.fill(x, y, x + SIZE, y + SIZE, EMPTY_BACKGROUND_COLOR);
            for (int offset = 0; offset < SIZE; offset++) {
                graphics.fill(x + offset, y + SIZE - 1 - offset, x + offset + 1, y + SIZE - offset, EMPTY_LINE_COLOR);
            }
        } else {
            graphics.fill(x, y, x + SIZE, y + SIZE, ARGB.opaque(color));
        }
        graphics.outline(x, y, SIZE, SIZE, INNER_BORDER_COLOR);
    }

}
