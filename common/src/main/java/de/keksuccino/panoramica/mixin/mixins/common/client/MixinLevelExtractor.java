package de.keksuccino.panoramica.mixin.mixins.common.client;

import de.keksuccino.panoramica.photo.PhotoModeManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public class MixinLevelExtractor {

    @Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
    private void before_isEntityVisible_Panoramica(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> info) {
        if (PhotoModeManager.shouldHidePlayerEntity(entity)) {
            info.setReturnValue(false);
        }
    }

}
