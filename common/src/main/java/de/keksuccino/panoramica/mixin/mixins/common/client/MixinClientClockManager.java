package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.keksuccino.panoramica.photo.PhotoModeManager;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientClockManager.class)
public class MixinClientClockManager {

    @ModifyReturnValue(method = "getTotalTicks", at = @At("RETURN"))
    private long modify_getTotalTicksReturn_Panoramica(long original) {
        return PhotoModeManager.overrideClockTicks(original);
    }

}
