package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.panoramica.menu.PanoramaMenuManager;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CubeMap.class)
public class MixinCubeMap {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;"))
    private AbstractTexture wrap_getTexture_Panoramica(@NotNull TextureManager instance, @NotNull Identifier location, @NotNull Operation<AbstractTexture> original) {
        Identifier customPanoramaId = PanoramaMenuManager.prepareTextureForRender(location);
        if (customPanoramaId != null) {
            return instance.getTexture(customPanoramaId);
        }
        return original.call(instance, location);
    }

}
