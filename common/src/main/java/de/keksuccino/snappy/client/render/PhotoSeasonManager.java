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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PhotoSeasonManager {

    private static final BlockState AIR_STATE = Blocks.AIR.defaultBlockState();
    private static final BlockState SNOW_LAYER_STATE = Blocks.SNOW.defaultBlockState();
    private static final BlockState GRASS_BLOCK_STATE = Blocks.GRASS_BLOCK.defaultBlockState().setValue(SnowyBlock.SNOWY, false);
    private static final int[][] AUTUMN_LEAF_COLOR_FAMILIES = {
            {0xFF9E3324, 0xFFB6462A, 0xFF862A1E},
            {0xFFC05A1C, 0xFFD07120, 0xFFA84A18},
            {0xFFD19B2E, 0xFFE0B140, 0xFFB98125},
            {0xFF7A4824, 0xFF8D582B, 0xFF60351F},
            {0xFFB26724, 0xFF9A4F20, 0xFFC17B2A}
    };
    private static final int AUTUMN_LEAF_BLOB_SIZE_XZ = 9;
    private static final int AUTUMN_LEAF_BLOB_SIZE_Y = 6;
    private static final int AUTUMN_LEAF_BLOB_VERTICAL_WEIGHT = 2;
    private static final float AUTUMN_LEAF_TINT_WEIGHT = 0.97F;
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
        return overrideRenderBlockState(level, null, rawStates, pos, original);
    }

    @NotNull
    public static BlockState overrideRenderBlockState(
            @NotNull BlockAndTintGetter level,
            @Nullable LevelReader heightLevel,
            @NotNull RawBlockStateGetter rawStates,
            @NotNull BlockPos pos,
            @NotNull BlockState original
    ) {
        PhotoModeSeason season = PhotoModeManager.season();
        return overrideRenderBlockStateAt(level, heightLevel, rawStates, pos.getX(), pos.getY(), pos.getZ(), original, season);
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
        return overrideRenderBlockStateAt(level, null, rawStates, x, y, z, original);
    }

    @NotNull
    public static BlockState overrideRenderBlockStateAt(
            @NotNull BlockAndTintGetter level,
            @Nullable LevelReader heightLevel,
            @NotNull RawBlockStateGetter rawStates,
            int x,
            int y,
            int z,
            @NotNull BlockState original
    ) {
        return overrideRenderBlockStateAt(level, heightLevel, rawStates, x, y, z, original, PhotoModeManager.season());
    }

    @NotNull
    private static BlockState overrideRenderBlockStateAt(
            @NotNull BlockAndTintGetter level,
            @Nullable LevelReader heightLevel,
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
        if (shouldAddSnowLayer(level, heightLevel, rawStates, x, y, z, original)) {
            return SNOW_LAYER_STATE;
        }
        return withWinterSnowyProperty(level, heightLevel, rawStates, x, y, z, original);
    }

    public static int overrideTintColor(@NotNull BlockState state, @NotNull BlockPos pos, int originalColor) {
        if (originalColor == -1) {
            return originalColor;
        }

        PhotoModeSeason season = PhotoModeManager.season();
        if (season.hasAutumnFoliage() && isLeafBlock(state) && isAutumnLeafTintCandidate(originalColor)) {
            return autumnLeafColor(pos, originalColor);
        }
        if (season.hasWinterTint() && isGreenHeavy(originalColor)) {
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
            @Nullable LevelReader heightLevel,
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
                || shouldAddSnowLayer(level, heightLevel, rawStates, x, y + 1, z, aboveState);
        return original.setValue(SnowyBlock.SNOWY, snowy);
    }

    private static boolean shouldAddSnowLayer(
            @NotNull BlockAndTintGetter level,
            @Nullable LevelReader heightLevel,
            @NotNull RawBlockStateGetter rawStates,
            int x,
            int y,
            int z,
            @NotNull BlockState original
    ) {
        if (!canUseVisualSnowPosition(original) || y <= level.getMinY() || y >= level.getMinY() + level.getHeight()) {
            return false;
        }
        if (!isOpenToSnowfall(level, heightLevel, rawStates, x, y, z)) {
            return false;
        }

        BlockPos belowPos = new BlockPos(x, y - 1, z);
        BlockState belowState = rawStates.getRawBlockState(x, y - 1, z);
        return canSupportVisualSnowLayer(level, belowPos, belowState);
    }

    private static boolean isOpenToSnowfall(
            @NotNull BlockAndTintGetter level,
            @Nullable LevelReader heightLevel,
            @NotNull RawBlockStateGetter rawStates,
            int x,
            int y,
            int z
    ) {
        if (heightLevel != null) {
            return heightLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) == y;
        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(x, y, z);
        if (!level.canSeeSky(mutablePos)) {
            return false;
        }

        int maxY = level.getMinY() + level.getHeight();
        for (int scanY = y + 1; scanY < maxY; scanY++) {
            mutablePos.setY(scanY);
            BlockState state = rawStates.getRawBlockState(x, scanY, z);
            if (isMotionBlockingForSnowfall(level, mutablePos, state)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMotionBlockingForSnowfall(
            @NotNull BlockGetter level,
            @NotNull BlockPos pos,
            @NotNull BlockState state
    ) {
        return state.isCollisionShapeFullBlock(level, pos) || !state.getFluidState().isEmpty();
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
        if (belowState.is(BlockTags.CANNOT_SUPPORT_SNOW_LAYER)) {
            return false;
        }
        if (belowState.is(BlockTags.SUPPORT_OVERRIDE_SNOW_LAYER)) {
            return true;
        }
        return Block.isFaceFull(belowState.getCollisionShape(level, belowPos), Direction.UP)
                || belowState.is(Blocks.SNOW) && belowState.getValue(SnowLayerBlock.LAYERS) == SnowLayerBlock.MAX_HEIGHT;
    }

    private static int autumnLeafColor(@NotNull BlockPos pos, int originalColor) {
        long blobSeed = autumnLeafBlobSeed(pos);
        int[] family = AUTUMN_LEAF_COLOR_FAMILIES[seedIndex(blobSeed, AUTUMN_LEAF_COLOR_FAMILIES.length)];
        int target = family[seedIndex(blobSeed >>> 17, family.length)];
        return ARGB.srgbLerp(AUTUMN_LEAF_TINT_WEIGHT, ARGB.opaque(originalColor), target);
    }

    private static long autumnLeafBlobSeed(@NotNull BlockPos pos) {
        int baseCellX = Math.floorDiv(pos.getX(), AUTUMN_LEAF_BLOB_SIZE_XZ);
        int baseCellY = Math.floorDiv(pos.getY(), AUTUMN_LEAF_BLOB_SIZE_Y);
        int baseCellZ = Math.floorDiv(pos.getZ(), AUTUMN_LEAF_BLOB_SIZE_XZ);
        long bestSeed = 0L;
        long bestDistance = Long.MAX_VALUE;

        for (int cellY = baseCellY - 1; cellY <= baseCellY + 1; cellY++) {
            for (int cellZ = baseCellZ - 1; cellZ <= baseCellZ + 1; cellZ++) {
                for (int cellX = baseCellX - 1; cellX <= baseCellX + 1; cellX++) {
                    long seed = mixSeed(cellX, cellY, cellZ);
                    int anchorX = cellX * AUTUMN_LEAF_BLOB_SIZE_XZ + seedOffset(seed, AUTUMN_LEAF_BLOB_SIZE_XZ);
                    int anchorY = cellY * AUTUMN_LEAF_BLOB_SIZE_Y + seedOffset(seed >>> 21, AUTUMN_LEAF_BLOB_SIZE_Y);
                    int anchorZ = cellZ * AUTUMN_LEAF_BLOB_SIZE_XZ + seedOffset(seed >>> 42, AUTUMN_LEAF_BLOB_SIZE_XZ);
                    long dx = pos.getX() - anchorX;
                    long dy = (long) (pos.getY() - anchorY) * AUTUMN_LEAF_BLOB_VERTICAL_WEIGHT;
                    long dz = pos.getZ() - anchorZ;
                    long distance = dx * dx + dy * dy + dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestSeed = seed;
                    }
                }
            }
        }

        return bestSeed;
    }

    private static long mixSeed(int x, int y, int z) {
        long seed = 0x9E3779B97F4A7C15L;
        seed ^= (long) x * 0xBF58476D1CE4E5B9L;
        seed = Long.rotateLeft(seed, 27);
        seed ^= (long) y * 0x94D049BB133111EBL;
        seed = Long.rotateLeft(seed, 31);
        seed ^= (long) z * 0xD6E8FEB86659FD93L;
        seed ^= seed >>> 30;
        seed *= 0xBF58476D1CE4E5B9L;
        seed ^= seed >>> 27;
        seed *= 0x94D049BB133111EBL;
        return seed ^ seed >>> 31;
    }

    private static int seedOffset(long seed, int size) {
        return Math.floorMod((int) (seed ^ seed >>> 32), size);
    }

    private static int seedIndex(long seed, int size) {
        return seedOffset(seed, size);
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

    private static boolean isAutumnLeafTintCandidate(int color) {
        int red = ARGB.red(color);
        int green = ARGB.green(color);
        int blue = ARGB.blue(color);
        return green >= 58 && green >= red * 0.92F && green > blue * 1.08F && red <= green * 1.18F;
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
