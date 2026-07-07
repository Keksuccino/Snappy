package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.keksuccino.snappy.client.render.PhotoSeasonManager;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSectionRegion.class)
public class MixinRenderSectionRegion {

    @Unique private static final BlockState AIR_STATE_SNAPPY = Blocks.AIR.defaultBlockState();

    @NotNull
    @Unique
    private BlockState getRawBlockState_Snappy(int x, int y, int z) {
        int sectionX = SectionPos.blockToSectionCoord(x);
        int sectionY = SectionPos.blockToSectionCoord(y);
        int sectionZ = SectionPos.blockToSectionCoord(z);
        if (sectionX < this.minSectionX || sectionX >= this.minSectionX + RenderSectionRegion.SIZE
                || sectionY < this.minSectionY || sectionY >= this.minSectionY + RenderSectionRegion.SIZE
                || sectionZ < this.minSectionZ || sectionZ >= this.minSectionZ + RenderSectionRegion.SIZE) {
            return AIR_STATE_SNAPPY;
        }
        return this.getSection(sectionX, sectionY, sectionZ).getBlockState(new BlockPos(x, y, z));
    }

    @Shadow @Final private int minSectionX;
    @Shadow @Final private int minSectionY;
    @Shadow @Final private int minSectionZ;

    @Shadow
    private SectionCopy getSection(int sectionX, int sectionY, int sectionZ) {
        throw new AssertionError();
    }

    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState modify_getBlockStateReturn_Snappy(BlockState original, BlockPos pos) {
        return PhotoSeasonManager.overrideRenderBlockState(
                (BlockAndTintGetter) (Object) this,
                this::getRawBlockState_Snappy,
                pos,
                original
        );
    }

}
