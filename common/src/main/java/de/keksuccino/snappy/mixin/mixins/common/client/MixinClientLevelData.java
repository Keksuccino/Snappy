package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.ClientLevelData.class)
public class MixinClientLevelData {

    @ModifyReturnValue(method = "getGameTime", at = @At("RETURN"))
    private long modify_getGameTimeReturn_Snappy(long original) {
        return PhotoModeManager.overrideGameTime(original);
    }

}
