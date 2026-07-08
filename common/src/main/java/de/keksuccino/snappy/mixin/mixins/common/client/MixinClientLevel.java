package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.keksuccino.snappy.client.render.PhotoSeasonManager;
import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
public class MixinClientLevel {

    @ModifyReturnValue(method = "getPrecipitationAt", at = @At("RETURN"))
    private Biome.Precipitation modify_getPrecipitationAtReturn_Snappy(Biome.Precipitation original, BlockPos pos) {
        return PhotoModeManager.overridePrecipitation(original, (ClientLevel) (Object) this, pos);
    }

    @ModifyReturnValue(method = "getClientLeafTintColor", at = @At("RETURN"))
    private int modify_getClientLeafTintColorReturn_Snappy(int original, BlockPos pos) {
        BlockState state = ((ClientLevel) (Object) this).getBlockState(pos);
        return PhotoSeasonManager.overrideTintColor(state, pos, original);
    }

}
