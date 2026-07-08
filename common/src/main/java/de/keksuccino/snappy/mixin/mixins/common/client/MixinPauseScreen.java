package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.snappy.screen.SnappyButtons;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PauseScreen.class)
public class MixinPauseScreen {

    @Unique private static final String RETURN_TO_GAME_TRANSLATION_KEY_SNAPPY = "menu.returnToGame";

    @WrapOperation(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;ILnet/minecraft/client/gui/layouts/LayoutSettings;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private LayoutElement wrap_addPauseMenuChild_Snappy(GridLayout.RowHelper instance, @NotNull LayoutElement widget, int columnWidth, LayoutSettings layoutSettings, Operation<LayoutElement> original) {
        LayoutElement addedWidget = original.call(instance, widget, columnWidth, layoutSettings);
        if (this.isReturnToGameButton_Snappy(widget, columnWidth) && SnappyButtons.shouldShowPhotoModeButton()) {
            instance.addChild(SnappyButtons.pauseMenuPhotoMode(), columnWidth);
        }
        if (widget instanceof LinearLayout iconButtonRow) {
            if (SnappyButtons.shouldShowScreenshotBrowserButton()) {
                iconButtonRow.addChild(SnappyButtons.screenshotBrowser((Screen) (Object) this));
            }
        }
        return addedWidget;
    }

    @Unique
    private boolean isReturnToGameButton_Snappy(@NotNull LayoutElement widget, int columnWidth) {
        if (columnWidth != 2 || !(widget instanceof Button button)) {
            return false;
        }

        ComponentContents contents = button.getMessage().getContents();
        return contents instanceof TranslatableContents translatableContents && RETURN_TO_GAME_TRANSLATION_KEY_SNAPPY.equals(translatableContents.getKey());
    }

}
