package de.keksuccino.panoramica.mixin.mixins.common.client;

import de.keksuccino.panoramica.screen.PanoramicaButtons;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(value = TitleScreen.class, priority = 900)
public abstract class MixinTitleScreen extends Screen {

    @Unique
    private static final int ICON_BUTTON_HEIGHT_PANORAMICA = 20;
    @Unique
    private static final int MIN_ICON_BUTTON_WIDTH_PANORAMICA = 16;
    @Unique
    private static final int MAX_ICON_BUTTON_WIDTH_PANORAMICA = 24;
    @Unique
    private static final int ICON_BUTTON_SPACING_PANORAMICA = 4;

    @Shadow
    @Nullable
    private FriendsButton friends;

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void after_init_Panoramica(CallbackInfo ci) {
        if (this.friends == null) {
            return;
        }

        int rowY = this.friends.getY();
        AbstractWidget screenshotBrowserButton = PanoramicaButtons.screenshotBrowser((Screen) (Object) this);
        screenshotBrowserButton.setPosition(this.width, rowY);
        this.addRenderableWidget(screenshotBrowserButton);
        this.reflowTitleIconRow_Panoramica(rowY);
    }

    @Unique
    private void reflowTitleIconRow_Panoramica(int rowY) {
        List<AbstractWidget> iconButtons = new ArrayList<>();
        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget
                    && widget.getY() == rowY
                    && widget.getHeight() == ICON_BUTTON_HEIGHT_PANORAMICA
                    && widget.getWidth() >= MIN_ICON_BUTTON_WIDTH_PANORAMICA
                    && widget.getWidth() <= MAX_ICON_BUTTON_WIDTH_PANORAMICA) {
                iconButtons.add(widget);
            }
        }
        if (iconButtons.isEmpty()) {
            return;
        }

        iconButtons.sort(Comparator.comparingInt(AbstractWidget::getX));
        int totalWidth = -ICON_BUTTON_SPACING_PANORAMICA;
        for (AbstractWidget widget : iconButtons) {
            totalWidth += widget.getWidth() + ICON_BUTTON_SPACING_PANORAMICA;
        }

        int x = this.width / 2 - totalWidth / 2;
        for (AbstractWidget widget : iconButtons) {
            widget.setPosition(x, rowY);
            x += widget.getWidth() + ICON_BUTTON_SPACING_PANORAMICA;
        }
    }

}
