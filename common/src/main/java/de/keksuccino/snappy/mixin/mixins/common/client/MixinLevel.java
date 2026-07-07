package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public class MixinLevel {

    @ModifyReturnValue(method = "getRainLevel", at = @At("RETURN"))
    private float modify_getRainLevelReturn_Snappy(float original) {
        return PhotoModeManager.overrideRainLevel(original);
    }

    @ModifyReturnValue(method = "getThunderLevel", at = @At("RETURN"))
    private float modify_getThunderLevelReturn_Snappy(float original) {
        return PhotoModeManager.overrideThunderLevel(original);
    }

    @Inject(method = "playLocalSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V", at = @At("HEAD"), cancellable = true)
    private void cancel_playLocalSound_Snappy(BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay, CallbackInfo info) {
        if (PhotoModeManager.shouldSuppressWorldRefreshSound(source)) {
            info.cancel();
        }
    }

}
