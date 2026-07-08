package de.keksuccino.snappy.mixin.mixins.common.client;

import de.keksuccino.snappy.screen.SnappyButtons;
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

    @Shadow @Nullable private FriendsButton friends;

    @Unique private static final int ICON_BUTTON_HEIGHT_SNAPPY = 20;
    @Unique private static final int MIN_ICON_BUTTON_WIDTH_SNAPPY = 16;
    @Unique private static final int MAX_ICON_BUTTON_WIDTH_SNAPPY = 24;
    @Unique private static final int ICON_BUTTON_SPACING_SNAPPY = 4;

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void after_init_Snappy(CallbackInfo ci) {
        if (!SnappyButtons.shouldShowScreenshotBrowserButton()) {
            return;
        }
        if (this.friends == null) {
            return;
        }

        int rowY = this.friends.getY();
        AbstractWidget screenshotBrowserButton = SnappyButtons.screenshotBrowser((Screen) (Object) this);
        screenshotBrowserButton.setPosition(this.width, rowY);
        this.addRenderableWidget(screenshotBrowserButton);
        this.reflowTitleIconRow_Snappy(rowY);
    }

    @Unique
    private void reflowTitleIconRow_Snappy(int rowY) {
        List<AbstractWidget> iconButtons = new ArrayList<>();
        // The title screen lays out the Realms/Friends icon row before this mixin adds Snappy's button, so the row must be centered again.
        for (GuiEventListener child : this.children()) {
            if (child instanceof AbstractWidget widget
                    && widget.getY() == rowY
                    && widget.getHeight() == ICON_BUTTON_HEIGHT_SNAPPY
                    && widget.getWidth() >= MIN_ICON_BUTTON_WIDTH_SNAPPY
                    && widget.getWidth() <= MAX_ICON_BUTTON_WIDTH_SNAPPY) {
                iconButtons.add(widget);
            }
        }
        if (iconButtons.isEmpty()) {
            return;
        }

        iconButtons.sort(Comparator.comparingInt(AbstractWidget::getX));
        int totalWidth = -ICON_BUTTON_SPACING_SNAPPY;
        for (AbstractWidget widget : iconButtons) {
            totalWidth += widget.getWidth() + ICON_BUTTON_SPACING_SNAPPY;
        }

        int x = this.width / 2 - totalWidth / 2;
        for (AbstractWidget widget : iconButtons) {
            widget.setPosition(x, rowY);
            x += widget.getWidth() + ICON_BUTTON_SPACING_SNAPPY;
        }
    }

}
