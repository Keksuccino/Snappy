package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public final class SnappyButtons {

    public static final Identifier SCREENSHOT_BROWSER_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/buttons/open_browser_icon_15x15.png");
    public static final Identifier PHOTO_MODE_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/photo_mode/buttons/open_photo_mode_icon_15x15.png");

    private SnappyButtons() {
    }

    public static boolean shouldShowScreenshotBrowserButton() {
        return Snappy.getOptions().isScreenshotBrowserButtonEnabled();
    }

    public static boolean shouldShowPhotoModeButton() {
        return Snappy.getOptions().isPhotoModeButtonEnabled();
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
                PHOTO_MODE_ICON
        );
    }

}
