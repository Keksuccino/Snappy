package de.keksuccino.snappy.client.render;

import de.keksuccino.snappy.photo.PhotoModeManager;
import de.keksuccino.snappy.photo.PhotoModeSeason;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;

public final class PhotoSeasonManager {

    private static final BlockState AIR_STATE = Blocks.AIR.defaultBlockState();
    private static final BlockState SNOW_LAYER_STATE = Blocks.SNOW.defaultBlockState();
    private static final BlockState GRASS_BLOCK_STATE = Blocks.GRASS_BLOCK.defaultBlockState().setValue(SnowyBlock.SNOWY, false);
    private static final int[] AUTUMN_LEAF_COLORS = {
            0xFFD99A2B,
            0xFFC86422,
            0xFFB9472B,
            0xFFD4B640,
            0xFFA86B2A,
            0xFF9B3A24,
            0xFFC98229,
            0xFF8D5A2D
    };
    private static final int[] WINTER_LEAF_COLORS = {
            0xFF8EA89B,
            0xFF7F988D,
            0xFFA9B9AE,
            0xFF6F8B82,
            0xFF96AAA4
    };
    private static final int[] WINTER_GRASS_COLORS = {
            0xFF86A994,
            0xFF799988,
            0xFFA3B7A8,
            0xFF6F907F,
            0xFF91A79A
    };

    private PhotoSeasonManager() {
    }

    public static void requestWorldRenderRefresh(@NotNull Minecraft minecraft) {
        if (!canRefreshWorldRenderers(minecraft)) {
            return;
        }
        if (minecraft.isSameThread()) {
            refreshWorldRenderers(minecraft);
        } else {
            minecraft.execute(() -> refreshWorldRenderers(minecraft));
        }
    }

    private static void refreshWorldRenderers(@NotNull Minecraft minecraft) {
        if (!canRefreshWorldRenderers(minecraft)) {
            return;
        }
        minecraft.levelExtractor.allChanged();
        refreshSodiumRenderer();
    }

    private static boolean canRefreshWorldRenderers(@NotNull Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null && minecraft.levelExtractor != null;
    }

    private static void refreshSodiumRenderer() {
        try {
            Class<?> rendererClass = Class.forName(
                    "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer",
                    false,
                    PhotoSeasonManager.class.getClassLoader()
            );
            Object renderer = rendererClass.getMethod("instanceNullable").invoke(null);
            if (renderer != null) {
                rendererClass.getMethod("reload").invoke(renderer);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    @NotNull
    public static BlockState overrideRenderBlockState(
            @NotNull BlockAndTintGetter level,
            @NotNull RawBlockStateGetter rawStates,
            @NotNull BlockPos pos,
            @NotNull BlockState original
    ) {
        PhotoModeSeason season = PhotoModeManager.season();
        return overrideRenderBlockStateAt(level, rawStates, pos.getX(), pos.getY(), pos.getZ(), original, season);
    }

    @NotNull
    public static BlockState overrideRenderBlockStateAt(
            @NotNull BlockAndTintGetter level,
            @NotNull RawBlockStateGetter rawStates,
            int x,
            int y,
            int z,
            @NotNull BlockState original
    ) {
        return overrideRenderBlockStateAt(level, rawStates, x, y, z, original, PhotoModeManager.season());
    }

    @NotNull
    private static BlockState overrideRenderBlockStateAt(
            @NotNull BlockAndTintGetter level,
            @NotNull RawBlockStateGetter rawStates,
            int x,
            int y,
            int z,
            @NotNull BlockState original,
            @NotNull PhotoModeSeason season
    ) {
        if (!season.hasVisualOverrides()) {
            return original;
        }

        if (season.removesSnow()) {
            return withoutSnow(original);
        }
        if (!season.addsSnow()) {
            return original;
        }
        if (shouldAddSnowLayer(level, rawStates, x, y, z, original)) {
            return SNOW_LAYER_STATE;
        }
        return withWinterSnowyProperty(level, rawStates, x, y, z, original);
    }

    public static int overrideTintColor(@NotNull BlockState state, @NotNull BlockPos pos, int originalColor) {
        if (originalColor == -1 || !isGreenHeavy(originalColor)) {
            return originalColor;
        }

        PhotoModeSeason season = PhotoModeManager.season();
        if (season.hasAutumnFoliage() && isLeafBlock(state)) {
            return paletteColor(AUTUMN_LEAF_COLORS, state, pos, originalColor, 0.88F);
        }
        if (season.hasWinterTint()) {
            if (isLeafBlock(state)) {
                return paletteColor(WINTER_LEAF_COLORS, state, pos, originalColor, 0.72F);
            }
            if (isGrassBlock(state)) {
                return paletteColor(WINTER_GRASS_COLORS, state, pos, originalColor, 0.70F);
            }
        }
        return originalColor;
    }

    public static void overrideTintColors(@NotNull BlockState state, @NotNull BlockPos pos, int @NotNull [] colors) {
        PhotoModeSeason season = PhotoModeManager.season();
        if (!season.hasAutumnFoliage() && !season.hasWinterTint()) {
            return;
        }

        for (int i = 0; i < colors.length; i++) {
            colors[i] = overrideTintColor(state, pos, colors[i]);
        }
    }

    @NotNull
    public static Biome.Precipitation overridePrecipitation(@NotNull Biome.Precipitation original) {
        PhotoModeSeason season = PhotoModeManager.season();
        if (season.removesSnow() && original == Biome.Precipitation.SNOW) {
            return Biome.Precipitation.RAIN;
        }
        if (season.addsSnow() && original == Biome.Precipitation.RAIN) {
            return Biome.Precipitation.SNOW;
        }
        return original;
    }

    public static void reclassifyWinterEmptySections(
            @NotNull ClientLevel level,
            @NotNull LongOpenHashSet addedEmptySections,
            @NotNull LongOpenHashSet removedEmptySections
    ) {
        if (!PhotoModeManager.season().addsSnow() || addedEmptySections.isEmpty()) {
            return;
        }

        LongIterator iterator = addedEmptySections.longIterator();
        while (iterator.hasNext()) {
            long sectionNode = iterator.nextLong();
            if (canEmptySectionRenderWinterSnow(level, sectionNode)) {
                iterator.remove();
                removedEmptySections.add(sectionNode);
            }
        }
    }

    public static boolean shouldBuildVisualWinterSection(@NotNull Level level, @NotNull SectionPos sectionPos) {
        return PhotoModeManager.season().addsSnow() && canEmptySectionRenderWinterSnow(level, sectionPos);
    }

    @NotNull
    private static BlockState withoutSnow(@NotNull BlockState original) {
        if (original.is(Blocks.SNOW)) {
            return AIR_STATE;
        }
        if (original.is(Blocks.SNOW_BLOCK)) {
            return GRASS_BLOCK_STATE;
        }
        if (original.hasProperty(SnowyBlock.SNOWY)) {
            return original.setValue(SnowyBlock.SNOWY, false);
        }
        return original;
    }

    @NotNull
    private static BlockState withWinterSnowyProperty(
            @NotNull BlockAndTintGetter level,
            @NotNull RawBlockStateGetter rawStates,
            int x,
            int y,
            int z,
            @NotNull BlockState original
    ) {
        if (!original.hasProperty(SnowyBlock.SNOWY)) {
            return original;
        }

        BlockState aboveState = rawStates.getRawBlockState(x, y + 1, z);
        boolean snowy = aboveState.is(Blocks.SNOW)
                || aboveState.is(Blocks.SNOW_BLOCK)
                || shouldAddSnowLayer(level, rawStates, x, y + 1, z, aboveState);
        return original.setValue(SnowyBlock.SNOWY, snowy);
    }

    private static boolean shouldAddSnowLayer(
            @NotNull BlockAndTintGetter level,
            @NotNull RawBlockStateGetter rawStates,
            int x,
            int y,
            int z,
            @NotNull BlockState original
    ) {
        if (!canUseVisualSnowPosition(original) || y <= level.getMinY() || y >= level.getMinY() + level.getHeight()) {
            return false;
        }

        BlockPos belowPos = new BlockPos(x, y - 1, z);
        BlockState belowState = rawStates.getRawBlockState(x, y - 1, z);
        return canSupportVisualSnowLayer(level, belowPos, belowState);
    }

    private static boolean canUseVisualSnowPosition(@NotNull BlockState original) {
        if (original.isAir()) {
            return true;
        }
        if (original.is(Blocks.SNOW)
                || original.is(Blocks.SNOW_BLOCK)
                || !original.getFluidState().isEmpty()
                || original.hasBlockEntity()
                || original.is(BlockTags.LEAVES)) {
            return false;
        }
        return (original.canBeReplaced() || original.is(BlockTags.REPLACEABLE)) && isSoftVegetationBlock(original);
    }

    private static boolean canEmptySectionRenderWinterSnow(@NotNull ClientLevel level, long sectionNode) {
        return canEmptySectionRenderWinterSnow(level, SectionPos.of(sectionNode));
    }

    private static boolean canEmptySectionRenderWinterSnow(@NotNull Level level, @NotNull SectionPos sectionPos) {
        int snowY = sectionPos.minBlockY();
        if (snowY <= level.getMinY() || snowY >= level.getMinY() + level.getHeight()) {
            return false;
        }
        if (level.getChunk(sectionPos.getX(), sectionPos.getZ(), ChunkStatus.FULL, false) == null) {
            return false;
        }

        int minX = sectionPos.minBlockX();
        int minZ = sectionPos.minBlockZ();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int blockX = minX + localX;
                int blockZ = minZ + localZ;
                mutablePos.set(blockX, snowY, blockZ);
                if (!level.getBlockState(mutablePos).isAir()) {
                    continue;
                }

                mutablePos.set(blockX, snowY - 1, blockZ);
                if (canSupportVisualSnowLayer(level, mutablePos, level.getBlockState(mutablePos))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean canSupportVisualSnowLayer(
            @NotNull BlockGetter level,
            @NotNull BlockPos belowPos,
            @NotNull BlockState belowState
    ) {
        if (belowState.isAir()
                || belowState.is(Blocks.SNOW)
                || belowState.is(Blocks.SNOW_BLOCK)
                || !belowState.getFluidState().isEmpty()) {
            return false;
        }
        if (belowState.is(BlockTags.SUPPORT_OVERRIDE_SNOW_LAYER) || belowState.is(BlockTags.LEAVES)) {
            return true;
        }
        if (belowState.is(BlockTags.CANNOT_SUPPORT_SNOW_LAYER)) {
            return false;
        }
        return hasVisualTopSupport(belowState.getBlockSupportShape(level, belowPos))
                || Block.isFaceFull(belowState.getCollisionShape(level, belowPos), Direction.UP);
    }

    private static boolean hasVisualTopSupport(@NotNull VoxelShape shape) {
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 0.999D;
    }

    private static int paletteColor(@NotNull int[] palette, @NotNull BlockState state, @NotNull BlockPos pos, int originalColor, float weight) {
        int target = palette[paletteIndex(palette, state, pos)];
        return ARGB.srgbLerp(weight, ARGB.opaque(originalColor), target);
    }

    private static int paletteIndex(@NotNull int[] palette, @NotNull BlockState state, @NotNull BlockPos pos) {
        long seed = state.getSeed(pos);
        return Math.floorMod((int) (seed ^ seed >>> 32), palette.length);
    }

    private static boolean isGreenHeavy(int color) {
        int red = ARGB.red(color);
        int green = ARGB.green(color);
        int blue = ARGB.blue(color);
        int nextHighest = Math.max(red, blue);
        return green >= 64 && green - nextHighest >= 14 && green > red * 1.07F && green > blue * 1.07F;
    }

    private static boolean isLeafBlock(@NotNull BlockState state) {
        if (state.is(BlockTags.LEAVES)) {
            return true;
        }

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = blockId.getPath();
        if (path.contains("leaf_litter")) {
            return false;
        }
        if (path.contains("leaves") || path.endsWith("_leaf") || path.contains("_leaf_")) {
            return true;
        }

        String className = state.getBlock().getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        return (className.contains("leaves") || className.contains("leaf")) && !className.contains("litter");
    }

    private static boolean isGrassBlock(@NotNull BlockState state) {
        return state.is(BlockTags.GRASS_BLOCKS)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.BUSH)
                || blockPathContains(state, "grass")
                || blockPathContains(state, "fern");
    }

    private static boolean isSoftVegetationBlock(@NotNull BlockState state) {
        if (state.is(BlockTags.FLOWER_POTS)) {
            return false;
        }
        if (state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.REPLACEABLE_BY_MUSHROOMS)) {
            return true;
        }

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = blockId.getPath();
        return !path.startsWith("potted_")
                && (path.contains("grass")
                || path.contains("fern")
                || path.contains("bush")
                || path.contains("flower")
                || path.contains("petal")
                || path.contains("sapling")
                || path.contains("sprout")
                || path.contains("mushroom")
                || path.contains("roots")
                || path.contains("leaf_litter"));
    }

    private static boolean blockPathContains(@NotNull BlockState state, @NotNull String needle) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return blockId.getPath().contains(needle);
    }

    @FunctionalInterface
    public interface RawBlockStateGetter {

        @NotNull
        BlockState getRawBlockState(int x, int y, int z);

        @NotNull
        default BlockState getRawBlockState(@NotNull BlockPos pos) {
            return this.getRawBlockState(pos.getX(), pos.getY(), pos.getZ());
        }

    }

}
