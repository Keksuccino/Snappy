package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.snappy.menu.PanoramaMenuManager;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.CubeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CubeMap;render(FF)V"))
    private void wrap_renderCubeMap_Snappy(CubeMap instance, float rotXInDegrees, float rotYInDegrees, Operation<Void> original) {
        PanoramaMenuManager.updateMenuParallax();
        original.call(instance, PanoramaMenuManager.applyMenuParallaxXRotation(rotXInDegrees), PanoramaMenuManager.applyMenuParallaxYRotation(rotYInDegrees));
    }

}
