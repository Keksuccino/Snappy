package de.keksuccino.snappy.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.keksuccino.snappy.client.render.PhotoSeasonRawBlockLookup;
import de.keksuccino.snappy.client.render.PhotoSeasonManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSectionRegion.class)
public class MixinRenderSectionRegion {

    @Shadow @Final private int minSectionX;
    @Shadow @Final private int minSectionY;
    @Shadow @Final private int minSectionZ;
    @Shadow @Final private ClientLevel level;

    @Shadow
    private SectionCopy getSection(int sectionX, int sectionY, int sectionZ) {
        throw new AssertionError();
    }

    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState modify_getBlockStateReturn_Snappy(BlockState original, BlockPos pos) {
        return PhotoSeasonManager.overrideRenderBlockState((BlockAndTintGetter) (Object) this, this.level, this::getRawBlockState_Snappy, pos, original);
    }

    @NotNull
    @Unique
    private BlockState getRawBlockState_Snappy(int x, int y, int z) {
        int sectionX = SectionPos.blockToSectionCoord(x);
        int sectionY = SectionPos.blockToSectionCoord(y);
        int sectionZ = SectionPos.blockToSectionCoord(z);
        // Season overlays query neighboring blocks while getBlockState is being modified, so this must read the unmodified section copy.
        if (sectionX < this.minSectionX || sectionX >= this.minSectionX + RenderSectionRegion.SIZE
                || sectionY < this.minSectionY || sectionY >= this.minSectionY + RenderSectionRegion.SIZE
                || sectionZ < this.minSectionZ || sectionZ >= this.minSectionZ + RenderSectionRegion.SIZE) {
            return PhotoSeasonRawBlockLookup.air();
        }
        return this.getSection(sectionX, sectionY, sectionZ).getBlockState(new BlockPos(x, y, z));
    }

}
