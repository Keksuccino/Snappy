package de.keksuccino.snappy.mixin.mixins.common.client.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import de.keksuccino.snappy.client.render.PhotoSeasonRawBlockLookup;
import de.keksuccino.snappy.client.render.PhotoSeasonManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public class MixinSodiumLevelSlice {

    @Shadow @Final private static int SECTION_ARRAY_LENGTH;

    @Shadow @Final private ClientLevel level;
    @Shadow @Final private BlockState[][] blockArrays;
    @Shadow private int originBlockX;
    @Shadow private int originBlockY;
    @Shadow private int originBlockZ;
    @Shadow private BoundingBox volume;

    /** @reason Winter photo mode can synthesize snow in sections Sodium would otherwise skip as empty. */
    @ModifyExpressionValue(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;hasOnlyAir()Z", remap = true), remap = false)
    private static boolean modify_prepareHasOnlyAir_Snappy(boolean original, @Local(argsOnly = true) Level level, @Local(argsOnly = true) SectionPos sectionPos) {
        return original && !PhotoSeasonManager.shouldBuildVisualWinterSection(level, sectionPos);
    }

    @ModifyReturnValue(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"), remap = false)
    private BlockState modify_getBlockStateReturn_Snappy(BlockState original, int x, int y, int z) {
        return PhotoSeasonManager.overrideRenderBlockStateAt((BlockAndTintGetter) (Object) this, this.level, this::getRawBlockState_Snappy, x, y, z, original);
    }

    @NotNull
    @Unique
    private BlockState getRawBlockState_Snappy(int x, int y, int z) {
        if (this.volume == null || !this.volume.isInside(x, y, z)) {
            return PhotoSeasonRawBlockLookup.air();
        }

        int localX = x - this.originBlockX;
        int localY = y - this.originBlockY;
        int localZ = z - this.originBlockZ;
        int sectionIndex = PhotoSeasonRawBlockLookup.sodiumSectionIndex(localX >> 4, localY >> 4, localZ >> 4, SECTION_ARRAY_LENGTH);
        int blockIndex = PhotoSeasonRawBlockLookup.sodiumBlockIndex(localX & 15, localY & 15, localZ & 15);
        // This mirrors Sodium's cached LevelSlice layout; use the copied arrays directly to avoid re-entering the season override path.
        return PhotoSeasonRawBlockLookup.sodiumSectionStateOrAir(this.blockArrays, sectionIndex, blockIndex);
    }

}
