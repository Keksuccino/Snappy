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

    /** @reason Sodium writes tint values into vertexColors after ColorProvider#getColors; photo-season tinting must adjust that cached array in place. */
    @Inject(method = "tintQuad", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/model/color/ColorProvider;getColors(Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos$MutableBlockPos;Ljava/lang/Object;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;[IZ)V", shift = At.Shift.AFTER), remap = false)
    private void after_tintQuadGetColors_Snappy(CallbackInfo ci) {
        // BlockRenderer inherits the render context fields from Sodium's AbstractBlockRenderContext; keep this accessor in sync with that class.
        AccessorMixinSodiumAbstractBlockRenderContext context = (AccessorMixinSodiumAbstractBlockRenderContext) (Object) this;
        BlockState state = context.getState_Snappy();
        BlockPos pos = context.getPos_Snappy();
        if (state != null && pos != null) {
            PhotoSeasonManager.overrideTintColors(state, pos, this.vertexColors);
        }
    }

}
