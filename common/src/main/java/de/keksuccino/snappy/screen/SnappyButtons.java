package de.keksuccino.snappy.screen;

import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.photo.PhotoModeManager;
import de.keksuccino.snappy.util.rendering.gui.widget.SnappyButton;
import de.keksuccino.snappy.util.rendering.gui.widget.IconButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public final class SnappyButtons {

    private static final int PAUSE_MENU_FULL_WIDTH_BUTTON_WIDTH = 204;

    public static final Identifier SCREENSHOT_BROWSER_ICON = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/screenshot_browser/buttons/open_browser_icon_15x15.png");

    private SnappyButtons() {
    }

    public static boolean shouldShowScreenshotBrowserButton() {
        return Snappy.getOptions().isScreenshotBrowserButtonEnabled();
    }

    public static boolean shouldShowPhotoModeButton() {
        return Snappy.getOptions().isPhotoModeButtonEnabled();
    }

    public static IconButton screenshotBrowser(@NotNull Screen parent) {
        IconButton button = new IconButton(Component.translatable("snappy.screenshot_browser.open"), ignored -> Minecraft.getInstance().gui.setScreen(new ScreenshotBrowserScreen(parent)), SCREENSHOT_BROWSER_ICON);
        button.useVanillaTextures(true);
        return button;
    }

    public static SnappyButton pauseMenuPhotoMode() {
        return new SnappyButton(0, 0, PAUSE_MENU_FULL_WIDTH_BUTTON_WIDTH, Component.translatable("snappy.photo_mode.open"), ignored -> PhotoModeManager.open(Minecraft.getInstance())).useVanillaTextures(true);
    }

}
