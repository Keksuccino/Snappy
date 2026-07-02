package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.panoramica.screen.PauseScreenshotBrowserButton;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PauseScreen.class)
public class MixinPauseScreen {

    @WrapOperation(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;ILnet/minecraft/client/gui/layouts/LayoutSettings;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private LayoutElement wrap_addIconButtonRow_Panoramica(
            GridLayout.RowHelper instance,
            @NotNull LayoutElement widget,
            int columnWidth,
            LayoutSettings layoutSettings,
            Operation<LayoutElement> original
    ) {
        if (widget instanceof LinearLayout iconButtonRow) {
            iconButtonRow.addChild(new PauseScreenshotBrowserButton((Screen) (Object) this));
        }
        return original.call(instance, widget, columnWidth, layoutSettings);
    }

}
