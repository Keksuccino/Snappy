package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.keksuccino.snappy.client.render.PhotoSeasonManager;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelBlockRenderer.class)
public class MixinModelBlockRenderer {

    @ModifyReturnValue(method = "computeTintColor", at = @At("RETURN"))
    private int modify_computeTintColorReturn_Snappy(
            int original,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            int tintIndex
    ) {
        return PhotoSeasonManager.overrideTintColor(state, pos, original);
    }

}
