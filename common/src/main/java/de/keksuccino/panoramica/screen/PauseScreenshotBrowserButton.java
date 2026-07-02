package de.keksuccino.panoramica.screen;

import de.keksuccino.panoramica.Panoramica;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

public class PauseScreenshotBrowserButton extends Button {

    private static final Identifier ICON_TEXTURE = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/button_icon_100x100.png");
    private static final int BUTTON_SIZE = 20;
    private static final int ICON_SIZE = 15;
    private static final int TEXTURE_SIZE = 100;

    public PauseScreenshotBrowserButton(@NotNull Screen parent) {
        super(0, 0, BUTTON_SIZE, BUTTON_SIZE, Component.translatable("panoramica.pause.screenshot_browser"),
                button -> Minecraft.getInstance().gui.setScreen(new ScreenshotBrowserScreen(parent)), DEFAULT_NARRATION);
        this.setTooltip(Tooltip.create(this.getMessage()));
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);

        int iconX = this.getX() + (this.getWidth() - ICON_SIZE) / 2;
        int iconY = this.getY() + (this.getHeight() - ICON_SIZE) / 2;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ICON_TEXTURE,
                iconX,
                iconY,
                0.0F,
                0.0F,
                ICON_SIZE,
                ICON_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                ARGB.white(this.alpha)
        );
    }

}
