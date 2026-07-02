package de.keksuccino.panoramica.mixin.mixins.common.client;

import de.keksuccino.panoramica.capture.PanoramaCaptureManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_Panoramica(CallbackInfo info) {
        PanoramaCaptureManager.clientTick((Minecraft) (Object) this);
    }

}
