package de.keksuccino.snappy.client.render;

import de.keksuccino.snappy.photo.PhotoModeWeatherPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class VisualLightningStormManager {

    private static final int VISUAL_LIGHTNING_ENTITY_ID_START = Integer.MIN_VALUE + 4096;
    private static final int VISUAL_LIGHTNING_ENTITY_ID_END = -1_800_000_000;
    private static final int LIVE_LIGHTNING_MIN_DELAY_TICKS = 20;
    private static final int LIVE_LIGHTNING_MAX_DELAY_TICKS = 52;
    private static final int LIVE_LIGHTNING_BURST_CHANCE = 4;
    private static final int MAX_VISUAL_LIGHTNING_ENTITIES = 12;
    private static final int PAUSED_STATIC_LIGHTNING_COUNT = 5;
    private static final int LIGHTNING_PLACEMENT_ATTEMPTS = 64;
    private static final int VERY_FAR_LIGHTNING_INTERVAL = 3;
    private static final double LIGHTNING_RENDER_DISTANCE_MARGIN = 24.0D;
    private static final double NEAR_LIGHTNING_MIN_DISTANCE = 28.0D;
    private static final double NEAR_LIGHTNING_MAX_DISTANCE = 64.0D;
    private static final double FAR_LIGHTNING_MIN_DISTANCE = 96.0D;
    private static final double FAR_LIGHTNING_MAX_DISTANCE = 176.0D;
    private static final double VERY_FAR_LIGHTNING_MIN_DISTANCE = 176.0D;
    private static final double VERY_FAR_LIGHTNING_MAX_DISTANCE = 360.0D;

    private static int nextVisualLightningEntityId = VISUAL_LIGHTNING_ENTITY_ID_START;

    private final RandomSource random = RandomSource.create();
    private final List<Integer> entityIds = new ArrayList<>();
    private int nextLiveStrikeTicks;
    private int farStrikeSequence;
    private boolean nextLiveStrikeFar;
    private boolean pausedStaticMode;

    public void tick(@NotNull Minecraft minecraft, @NotNull StormState active) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || active.weatherPreset() != PhotoModeWeatherPreset.THUNDERING) {
            this.clear(level);
            return;
        }

        this.prune(level);
        boolean paused = active.paused() && canPause(minecraft);
        if (paused) {
            if (!this.pausedStaticMode) {
                this.clear(level);
                this.pausedStaticMode = true;
            }
            this.fillPausedStaticBolts(minecraft, active);
            return;
        }

        if (this.pausedStaticMode) {
            this.clear(level);
            this.pausedStaticMode = false;
            this.nextLiveStrikeTicks = 0;
        }

        if (this.nextLiveStrikeTicks > 0) {
            this.nextLiveStrikeTicks--;
            return;
        }

        int strikeCount = this.random.nextInt(LIVE_LIGHTNING_BURST_CHANCE) == 0 ? 2 : 1;
        for (int strike = 0; strike < strikeCount; strike++) {
            this.spawnVisualLightning(minecraft, active, this.nextLiveStrikeDistance());
        }
        this.nextLiveStrikeTicks = this.random.nextIntBetweenInclusive(LIVE_LIGHTNING_MIN_DELAY_TICKS, LIVE_LIGHTNING_MAX_DELAY_TICKS);
    }

    public void clear(@Nullable ClientLevel level) {
        if (level != null) {
            for (Integer entityId : this.entityIds) {
                level.removeEntity(entityId, Entity.RemovalReason.DISCARDED);
            }
        }
        this.entityIds.clear();
        this.farStrikeSequence = 0;
        this.nextLiveStrikeFar = false;
        this.pausedStaticMode = false;
        this.nextLiveStrikeTicks = 0;
    }

    private void fillPausedStaticBolts(@NotNull Minecraft minecraft, @NotNull StormState active) {
        int missingStrikes = PAUSED_STATIC_LIGHTNING_COUNT - this.entityIds.size();
        int startSlot = this.entityIds.size();
        for (int strike = 0; strike < missingStrikes; strike++) {
            this.spawnVisualLightning(minecraft, active, this.staticStrikeDistance(startSlot + strike));
        }
    }

    @NotNull
    private StrikeDistance nextLiveStrikeDistance() {
        this.nextLiveStrikeFar = !this.nextLiveStrikeFar;
        if (!this.nextLiveStrikeFar) {
            return StrikeDistance.NEAR;
        }

        this.farStrikeSequence++;
        return this.farStrikeSequence % VERY_FAR_LIGHTNING_INTERVAL == 0 ? StrikeDistance.VERY_FAR : StrikeDistance.FAR;
    }

    @NotNull
    private StrikeDistance staticStrikeDistance(int slot) {
        if (slot % 2 == 0) {
            return StrikeDistance.NEAR;
        }
        return slot % (VERY_FAR_LIGHTNING_INTERVAL * 2) == VERY_FAR_LIGHTNING_INTERVAL ? StrikeDistance.VERY_FAR : StrikeDistance.FAR;
    }

    private boolean spawnVisualLightning(@NotNull Minecraft minecraft, @NotNull StormState active, @NotNull StrikeDistance distance) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            return false;
        }

        Vec3 position = this.chooseStrikePosition(minecraft, active, distance);
        if (position == null) {
            return false;
        }

        int entityId = nextVisualLightningEntityId(level);
        if (entityId == 0) {
            return false;
        }

        LightningBolt lightning = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
        lightning.setId(entityId);
        lightning.setVisualOnly(true);
        lightning.snapTo(position);
        level.addEntity(lightning);
        this.entityIds.add(entityId);
        this.enforceEntityLimit(level);
        return true;
    }

    @Nullable
    private Vec3 chooseStrikePosition(@NotNull Minecraft minecraft, @NotNull StormState active, @NotNull StrikeDistance distance) {
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return null;
        }

        Vec3 fallback = null;
        Vec3[] origins = new Vec3[] { active.position(), player.position() };
        for (Vec3 origin : origins) {
            for (int attempt = 0; attempt < LIGHTNING_PLACEMENT_ATTEMPTS; attempt++) {
                Vec3 candidate = this.createStrikeCandidate(minecraft, level, origin, distance);
                if (candidate == null) {
                    continue;
                }
                if (minecraft.levelRenderer.isSectionCompiledAndVisible(BlockPos.containing(candidate))) {
                    return candidate;
                }
                if (fallback == null) {
                    fallback = candidate;
                }
            }
        }

        return fallback;
    }

    @Nullable
    private Vec3 createStrikeCandidate(@NotNull Minecraft minecraft, @NotNull ClientLevel level, @NotNull Vec3 origin, @NotNull StrikeDistance distance) {
        double minDistance = distance.minDistance();
        double maxDistance = Math.min(distance.maxDistance(), maxLoadedStrikeDistance(minecraft));
        if (maxDistance < minDistance) {
            minDistance = Math.max(NEAR_LIGHTNING_MIN_DISTANCE, maxDistance * 0.65D);
        }
        double strikeDistance = Mth.lerp(this.random.nextDouble(), minDistance, maxDistance);
        double angle = this.random.nextDouble() * Mth.TWO_PI;
        int blockX = Mth.floor(origin.x + Math.cos(angle) * strikeDistance);
        int blockZ = Mth.floor(origin.z + Math.sin(angle) * strikeDistance);
        int chunkX = SectionPos.blockToSectionCoord(blockX);
        int chunkZ = SectionPos.blockToSectionCoord(blockZ);
        if (level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) == null) {
            return null;
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ);
        BlockPos strikePos = new BlockPos(blockX, surfaceY, blockZ);
        if (level.isOutsideBuildHeight(strikePos) || !level.getWorldBorder().isWithinBounds(strikePos)) {
            return null;
        }

        return Vec3.atBottomCenterOf(strikePos);
    }

    private static double maxLoadedStrikeDistance(@NotNull Minecraft minecraft) {
        return Math.max(NEAR_LIGHTNING_MAX_DISTANCE, minecraft.options.getEffectiveRenderDistance() * 16.0D - LIGHTNING_RENDER_DISTANCE_MARGIN);
    }

    private void prune(@NotNull ClientLevel level) {
        this.entityIds.removeIf(entityId -> {
            Entity entity = level.getEntity(entityId);
            return entity == null || entity.isRemoved();
        });
    }

    private void enforceEntityLimit(@NotNull ClientLevel level) {
        while (this.entityIds.size() > MAX_VISUAL_LIGHTNING_ENTITIES) {
            Integer entityId = this.entityIds.remove(0);
            level.removeEntity(entityId, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int nextVisualLightningEntityId(@NotNull ClientLevel level) {
        for (int attempt = 0; attempt < 4096; attempt++) {
            int candidate = nextVisualLightningEntityId++;
            if (nextVisualLightningEntityId >= VISUAL_LIGHTNING_ENTITY_ID_END) {
                nextVisualLightningEntityId = VISUAL_LIGHTNING_ENTITY_ID_START;
            }
            if (candidate != 0 && level.getEntity(candidate) == null) {
                return candidate;
            }
        }
        return 0;
    }

    private static boolean canPause(@NotNull Minecraft minecraft) {
        return minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished();
    }

    public interface StormState {

        @NotNull
        Vec3 position();

        @NotNull
        PhotoModeWeatherPreset weatherPreset();

        boolean paused();

    }

    private enum StrikeDistance {
        NEAR(NEAR_LIGHTNING_MIN_DISTANCE, NEAR_LIGHTNING_MAX_DISTANCE),
        FAR(FAR_LIGHTNING_MIN_DISTANCE, FAR_LIGHTNING_MAX_DISTANCE),
        VERY_FAR(VERY_FAR_LIGHTNING_MIN_DISTANCE, VERY_FAR_LIGHTNING_MAX_DISTANCE);

        private final double minDistance;
        private final double maxDistance;

        StrikeDistance(double minDistance, double maxDistance) {
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
        }

        private double minDistance() {
            return this.minDistance;
        }

        private double maxDistance() {
            return this.maxDistance;
        }
    }

}
