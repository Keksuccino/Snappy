package de.keksuccino.panoramica.screen;

import de.keksuccino.panoramica.Panoramica;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public final class PanoramicaButtons {

    private static final Identifier SCREENSHOT_BROWSER_ICON = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/screenshot_browser_icon_15x15.png");

    private PanoramicaButtons() {
    }

    public static boolean shouldShowScreenshotButtons() {
        return !Panoramica.getOptions().areScreenshotButtonsHidden();
    }

    public static TexturedIconButton screenshotBrowser(@NotNull Screen parent) {
        return new TexturedIconButton(
                Component.translatable("panoramica.screenshot_browser.open"),
                button -> Minecraft.getInstance().gui.setScreen(new ScreenshotBrowserScreen(parent)),
                SCREENSHOT_BROWSER_ICON
        );
    }

}
