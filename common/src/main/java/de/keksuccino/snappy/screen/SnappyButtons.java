package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public final class SnappyButtons {

    public static final Identifier SCREENSHOT_BROWSER_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser_icon_15x15.png");

    private SnappyButtons() {
    }

    public static boolean shouldShowScreenshotButtons() {
        return !Snappy.getOptions().areScreenshotButtonsHidden();
    }

    public static TexturedIconButton screenshotBrowser(@NotNull Screen parent) {
        return new TexturedIconButton(
                Component.translatable("snappy.screenshot_browser.open"),
                button -> Minecraft.getInstance().gui.setScreen(new ScreenshotBrowserScreen(parent)),
                SCREENSHOT_BROWSER_ICON
        );
    }

    public static TexturedIconButton photoMode() {
        return new TexturedIconButton(
                Component.translatable("snappy.photo_mode.open"),
                button -> PhotoModeManager.open(Minecraft.getInstance()),
                SCREENSHOT_BROWSER_ICON
        );
    }

}
