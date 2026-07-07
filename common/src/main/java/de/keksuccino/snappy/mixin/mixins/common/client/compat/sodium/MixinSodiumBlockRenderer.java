package de.keksuccino.snappy.mixin.mixins.common.client.compat.sodium;

import de.keksuccino.snappy.client.render.PhotoSeasonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public class MixinSodiumBlockRenderer {

    @Shadow @Final private int[] vertexColors;
    @Shadow protected BlockState state;
    @Shadow protected BlockPos pos;

    @Inject(
            method = "tintQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/model/color/ColorProvider;getColors(Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos$MutableBlockPos;Ljava/lang/Object;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;[IZ)V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void after_tintQuadGetColors_Snappy(CallbackInfo ci) {
        PhotoSeasonManager.overrideTintColors(this.state, this.pos, this.vertexColors);
    }

}
