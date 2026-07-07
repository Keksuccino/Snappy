package de.keksuccino.snappy.client.render;

import de.keksuccino.snappy.photo.PhotoModeTimePreset;
import de.keksuccino.snappy.photo.PhotoModeWeatherPreset;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PhotoEnvironmentManager {

    public static final double PHOTO_FOG_MIN_DISTANCE = 8.0D;
    public static final double PHOTO_FOG_MAX_DISTANCE = 512.0D;
    public static final double PHOTO_FOG_DEFAULT_DISTANCE = 64.0D;
    public static final int PHOTO_FOG_FALLBACK_COLOR = ARGB.color(168, 184, 196);
    public static final int PHOTO_SKY_FALLBACK_COLOR = ARGB.color(120, 169, 255);

    private static final int WORLD_FAST_FORWARD_TICKS = 40;
    private static final int WORLD_FOLLOWUP_TICKS = 5;

    private static boolean worldOverrideScope;
    private static boolean suppressWorldRefreshSounds;
    private static boolean suppressSkyColorOverride;

    private PhotoEnvironmentManager() {
    }

    public static void reset(@NotNull Minecraft minecraft) {
        worldOverrideScope = false;
        suppressWorldRefreshSounds = false;
        suppressSkyColorOverride = false;
        refreshWorldCaches(minecraft, null, true);
    }

    public static void beginWorldOverrideScope(@Nullable EnvironmentState active) {
        worldOverrideScope = active != null;
    }

    public static void endWorldOverrideScope() {
        worldOverrideScope = false;
    }

    public static long overrideClockTicks(@Nullable EnvironmentState active, long original) {
        return active != null && worldOverrideScope ? active.timePreset().clockTicks() : original;
    }

    public static float overrideRainLevel(@Nullable EnvironmentState active, float original) {
        return active != null && worldOverrideScope ? active.weatherPreset().rainLevel() : original;
    }

    public static float overrideThunderLevel(@Nullable EnvironmentState active, float original) {
        return active != null && worldOverrideScope ? active.weatherPreset().thunderLevel() : original;
    }

    public static long overrideGameTime(@Nullable EnvironmentState active, long original) {
        return active != null && worldOverrideScope ? original + active.visualGameTimeOffsetTicks() : original;
    }

    public static void applyFogOverrides(@Nullable EnvironmentState active, @NotNull FogData fog, int renderDistanceInChunks) {
        if (active == null) {
            return;
        }

        active.sampleFogColor(fog);
        float intensity = active.fogIntensity();
        @Nullable Integer color = active.fogColorOverride();
        if (intensity > 0.0F && color != null) {
            fog.color.set(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), 1.0F);
        }

        if (intensity <= 0.0F) {
            return;
        }

        float renderDistanceBlocks = Math.max(1.0F, renderDistanceInChunks * 16.0F);
        double maxDistance = Math.max(PHOTO_FOG_MIN_DISTANCE, Math.min(PHOTO_FOG_MAX_DISTANCE, renderDistanceBlocks));
        float end = (float) Mth.clamp(active.fogDistance(), PHOTO_FOG_MIN_DISTANCE, maxDistance);
        float start = Math.max(-8.0F, end * (1.0F - intensity));
        if (end <= start) {
            end = start + 1.0F;
        }

        fog.environmentalStart = start;
        fog.environmentalEnd = end;
        fog.skyEnd = Math.min(fog.skyEnd, end);
        fog.cloudEnd = Math.min(fog.cloudEnd, end);
    }

    public static boolean shouldSuppressWorldRefreshSound(@NotNull SoundSource source) {
        return suppressWorldRefreshSounds && source == SoundSource.WEATHER;
    }

    public static void afterExtractRenderState(@Nullable EnvironmentState active, @NotNull GameRenderState gameRenderState) {
        if (active == null) {
            return;
        }

        LevelRenderState levelRenderState = gameRenderState.levelRenderState;
        levelRenderState.weatherRenderState.intensity = active.weatherPreset().rainLevel();
        levelRenderState.skyRenderState.rainBrightness = 1.0F - active.weatherPreset().rainLevel();
        gameRenderState.lightmapRenderState.needsUpdate = true;
    }

    public static int overrideSkyColor(@Nullable EnvironmentState active, int sampledSkyColor) {
        return active == null || suppressSkyColorOverride ? sampledSkyColor : active.overrideSkyColor(sampledSkyColor);
    }

    public static void requestWorldVisualRefresh(@NotNull Minecraft minecraft, @Nullable EnvironmentState active) {
        if (active == null) {
            return;
        }

        active.setWorldVisualTicksRemaining(WORLD_FOLLOWUP_TICKS);
        runWithWorldOverrideScope(() -> refreshWorldCaches(minecraft, active, true));
        if (shouldRunPausedWorldVisualRefresh(minecraft, active)) {
            runPausedWorldVisualTicks(minecraft, active, WORLD_FAST_FORWARD_TICKS);
        }
    }

    public static void tickWorldVisualRefresh(@NotNull Minecraft minecraft, @Nullable EnvironmentState active) {
        if (active != null && active.worldVisualTicksRemaining() > 0 && shouldRunPausedWorldVisualRefresh(minecraft, active)) {
            runPausedWorldVisualTicks(minecraft, active, 1);
            active.setWorldVisualTicksRemaining(active.worldVisualTicksRemaining() - 1);
        }
    }

    @NotNull
    public static PhotoModeTimePreset defaultTimePreset(@NotNull Minecraft minecraft) {
        if (minecraft.level == null) {
            return PhotoModeTimePreset.NOON;
        }

        long clock = Math.floorMod(minecraft.level.getDefaultClockTime(), 24_000L);
        PhotoModeTimePreset best = PhotoModeTimePreset.NOON;
        long bestDistance = Long.MAX_VALUE;
        for (PhotoModeTimePreset preset : PhotoModeTimePreset.values()) {
            long distance = Math.abs(clock - preset.clockTicks());
            distance = Math.min(distance, 24_000L - distance);
            if (distance < bestDistance) {
                best = preset;
                bestDistance = distance;
            }
        }
        return best;
    }

    @NotNull
    public static PhotoModeWeatherPreset defaultWeatherPreset(@NotNull Minecraft minecraft) {
        if (minecraft.level == null) {
            return PhotoModeWeatherPreset.SUNNY;
        }
        if (minecraft.level.getThunderLevel(1.0F) >= 0.5F) {
            return PhotoModeWeatherPreset.THUNDERING;
        }
        if (minecraft.level.getRainLevel(1.0F) >= 0.5F) {
            return PhotoModeWeatherPreset.RAINY;
        }
        return PhotoModeWeatherPreset.SUNNY;
    }

    public static int sampleSkyColor(@NotNull Minecraft minecraft, @NotNull Vec3 position) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            return PHOTO_SKY_FALLBACK_COLOR;
        }

        boolean previousSuppressSkyColorOverride = suppressSkyColorOverride;
        suppressSkyColorOverride = true;
        try {
            return ARGB.opaque(level.environmentAttributes().getValue(EnvironmentAttributes.SKY_COLOR, position, null));
        } finally {
            suppressSkyColorOverride = previousSuppressSkyColorOverride;
        }
    }

    private static boolean shouldRunPausedWorldVisualRefresh(@NotNull Minecraft minecraft, @NotNull EnvironmentState active) {
        return active.paused()
                && canPause(minecraft)
                && minecraft.level != null
                && minecraft.player != null
                && minecraft.level.tickRateManager().runsNormally();
    }

    private static void runPausedWorldVisualTicks(@NotNull Minecraft minecraft, @NotNull EnvironmentState active, int ticks) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || ticks <= 0 || !shouldRunPausedWorldVisualRefresh(minecraft, active)) {
            return;
        }

        // Visual-only catch-up: do not call ClientLevel.tick() or touch the integrated server.
        boolean previousWorldOverrideScope = worldOverrideScope;
        boolean previousSuppressWorldRefreshSounds = suppressWorldRefreshSounds;
        worldOverrideScope = true;
        suppressWorldRefreshSounds = true;
        try {
            for (int tick = 0; tick < ticks; tick++) {
                active.advanceVisualGameTime(1L);
                refreshWorldCaches(minecraft, active, false);
                level.tickWeatherEffects();
                minecraft.particleEngine.tick();
            }
        } finally {
            worldOverrideScope = previousWorldOverrideScope;
            suppressWorldRefreshSounds = previousSuppressWorldRefreshSounds;
        }
    }

    private static void runWithWorldOverrideScope(@NotNull Runnable runnable) {
        boolean previousWorldOverrideScope = worldOverrideScope;
        worldOverrideScope = true;
        try {
            runnable.run();
        } finally {
            worldOverrideScope = previousWorldOverrideScope;
        }
    }

    private static void refreshWorldCaches(
            @NotNull Minecraft minecraft,
            @Nullable EnvironmentState active,
            boolean resetProbe
    ) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        level.environmentAttributes().invalidateTickCache();
        level.updateSkyBrightness();

        Camera camera = minecraft.gameRenderer.mainCamera();
        if (resetProbe) {
            camera.attributeProbe().reset();
        }

        camera.attributeProbe().tick(level, active == null ? camera.position() : active.position());
    }

    private static boolean canPause(@NotNull Minecraft minecraft) {
        return minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished();
    }

    public interface EnvironmentState {

        @NotNull
        Vec3 position();

        @NotNull
        PhotoModeTimePreset timePreset();

        @NotNull
        PhotoModeWeatherPreset weatherPreset();

        float fogIntensity();

        float fogDistance();

        @Nullable
        Integer fogColorOverride();

        boolean paused();

        long visualGameTimeOffsetTicks();

        int worldVisualTicksRemaining();

        void setWorldVisualTicksRemaining(int ticks);

        void advanceVisualGameTime(long ticks);

        void sampleFogColor(@NotNull FogData fog);

        int overrideSkyColor(int sampledSkyColor);

    }

}
