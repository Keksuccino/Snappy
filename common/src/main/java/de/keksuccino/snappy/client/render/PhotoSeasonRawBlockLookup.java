package de.keksuccino.snappy.client.render;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PhotoSeasonRawBlockLookup {

    private static final BlockState AIR_STATE = Blocks.AIR.defaultBlockState();

    private PhotoSeasonRawBlockLookup() {
    }

    @NotNull
    public static BlockState air() {
        return AIR_STATE;
    }

    @NotNull
    public static BlockState stateOrAir(@Nullable BlockState state) {
        return state == null ? AIR_STATE : state;
    }

    public static int sodiumBlockIndex(int localX, int localY, int localZ) {
        return localY << 8 | localZ << 4 | localX;
    }

    public static int sodiumSectionIndex(int sectionX, int sectionY, int sectionZ, int sectionArrayLength) {
        return sectionY * sectionArrayLength * sectionArrayLength + sectionZ * sectionArrayLength + sectionX;
    }

    @NotNull
    public static BlockState sodiumSectionStateOrAir(@NotNull BlockState[][] blockArrays, int sectionIndex, int blockIndex) {
        if (sectionIndex < 0 || sectionIndex >= blockArrays.length) {
            return AIR_STATE;
        }
        BlockState[] sectionStates = blockArrays[sectionIndex];
        if (sectionStates == null || blockIndex < 0 || blockIndex >= sectionStates.length) {
            return AIR_STATE;
        }
        return stateOrAir(sectionStates[blockIndex]);
    }

}
