package de.keksuccino.panoramica.photo;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.capture.NormalScreenshotCaptureManager;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager;
import de.keksuccino.panoramica.photo.PhotoPoseManager.PoseEntry;
import de.keksuccino.panoramica.platform.Services;
import de.keksuccino.panoramica.preview.ScreenshotPreviewManager;
import de.keksuccino.panoramica.screen.PhotoModeScreen;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class PhotoModeManager {

    private static final Identifier VIGNETTE_TEXTURE = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "textures/photo_vignette.png");
    private static final float CAMERA_SPEED_BLOCKS_PER_SECOND = 9.0F;
    private static final float CAMERA_FAST_SPEED_MULTIPLIER = 3.0F;
    private static final float CAMERA_SLOW_SPEED_MULTIPLIER = 0.35F;
    private static final float MOUSE_ROTATION_SENSITIVITY = 0.16F;
    private static final double INITIAL_CAMERA_DISTANCE = 3.0D;
    private static final double SCROLL_ZOOM_SECONDS_PER_NOTCH = 0.08D;
    private static final double MAX_SCROLL_ZOOM_NOTCHES = 4.0D;
    private static final double MAX_FRAME_SECONDS = 0.1D;
    private static final double SELF_PLAYER_POSITION_OFFSET_RANGE = 5.0D;
    private static final double SELF_PLAYER_ROTATION_OFFSET_RANGE = 180.0D;
    private static final double SELF_PLAYER_TRANSFORM_EPSILON = 1.0E-4D;
    public static final float DEPTH_OF_FIELD_FOCUS_DISTANCE_MIN = 0.05F;
    public static final float DEPTH_OF_FIELD_FOCUS_DISTANCE_MAX = 120.0F;
    public static final float DEPTH_OF_FIELD_FOCUS_DISTANCE_DEFAULT = 8.0F;
    public static final float DEPTH_OF_FIELD_FOCAL_LENGTH_MIN = 18.0F;
    public static final float DEPTH_OF_FIELD_FOCAL_LENGTH_MAX = 200.0F;
    public static final float DEPTH_OF_FIELD_FOCAL_LENGTH_DEFAULT = 50.0F;
    public static final float DEPTH_OF_FIELD_APERTURE_MIN = 1.2F;
    public static final float DEPTH_OF_FIELD_APERTURE_MAX = 22.0F;
    public static final float DEPTH_OF_FIELD_APERTURE_DEFAULT = 2.8F;
    public static final double PHOTO_FOG_MIN_DISTANCE = 8.0D;
    public static final double PHOTO_FOG_MAX_DISTANCE = 512.0D;
    public static final double PHOTO_FOG_DEFAULT_DISTANCE = 64.0D;
    private static final int PHOTO_FOG_FALLBACK_COLOR = ARGB.color(168, 184, 196);
    private static final int PHOTO_SKY_FALLBACK_COLOR = ARGB.color(120, 169, 255);
    private static final int ENVIRONMENT_FAST_FORWARD_TICKS = 40;
    private static final int ENVIRONMENT_FOLLOWUP_TICKS = 5;
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

    @Nullable
    private static Session session;
    private static boolean environmentOverrideScope;
    private static boolean suppressEnvironmentRefreshSounds;
    private static boolean suppressSkyColorOverride;
    private static final Matrix4f depthOfFieldProjectionMatrix = new Matrix4f();
    private static boolean depthOfFieldProjectionMatrixAvailable;
    private static int nextVisualLightningEntityId = VISUAL_LIGHTNING_ENTITY_ID_START;

    private PhotoModeManager() {
    }

    public static void open(@NotNull Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            minecraft.showDebugChat(Component.translatable("panoramica.photo_mode.unavailable"));
            return;
        }

        Session previous = session;
        if (previous != null) {
            previous.clearVisualEffects(minecraft.level);
        }
        PhotoPoseManager.reload();
        session = Session.create(minecraft);
        requestEnvironmentVisualRefresh(minecraft);
        minecraft.gui.setScreen(new PhotoModeScreen());
    }

    public static void close() {
        Session active = session;
        if (active != null) {
            active.clearVisualEffects(Minecraft.getInstance().level);
        }
        PhotoModeDepthOfFieldRenderer.close();
        session = null;
        environmentOverrideScope = false;
        suppressEnvironmentRefreshSounds = false;
        refreshEnvironmentCaches(Minecraft.getInstance(), true);
    }

    public static boolean isActive() {
        return session != null;
    }

    @Nullable
    public static Session session() {
        return session;
    }

    public static boolean shouldHideHud() {
        return isActive();
    }

    public static boolean shouldHideAllNonPhotoGui() {
        return shouldHideHud() || NormalScreenshotCaptureManager.shouldForceHideHud();
    }

    public static boolean shouldHidePhotoModeUi() {
        Session active = session;
        return NormalScreenshotCaptureManager.shouldForceHideHud() || active != null && active.photoModeUiHidden();
    }

    public static boolean shouldRenderPhotoModeGrid() {
        Session active = session;
        return active != null && active.gridEnabled() && !NormalScreenshotCaptureManager.shouldForceHideHud();
    }

    public static boolean isPhotoModeUiHidden() {
        Session active = session;
        return active != null && active.photoModeUiHidden();
    }

    public static void setPhotoModeUiHidden(boolean hidden) {
        Session active = session;
        if (active != null) {
            active.setPhotoModeUiHidden(hidden);
        }
    }

    public static boolean isPauseScreen(@NotNull Minecraft minecraft) {
        return session != null && session.paused() && canPause(minecraft);
    }

    public static boolean canPause(@NotNull Minecraft minecraft) {
        return minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished();
    }

    public static void togglePaused(@NotNull Minecraft minecraft) {
        Session active = session;
        if (active == null) {
            return;
        }
        if (!canPause(minecraft)) {
            active.setPaused(false);
            return;
        }
        active.setPaused(!active.paused());
    }

    public static void resetToDefaults(@NotNull Minecraft minecraft) {
        Session active = session;
        if (active == null || minecraft.player == null) {
            return;
        }
        active.reset(minecraft);
    }

    public static void clientTick(@NotNull Minecraft minecraft) {
        Session active = session;
        if (active != null && (minecraft.level == null || minecraft.player == null)) {
            close();
            return;
        }
        if (active != null) {
            active.tickVisualEffects(minecraft);
        }
        if (active != null && active.environmentVisualTicksRemaining > 0 && shouldRunPausedEnvironmentVisualRefresh(minecraft)) {
            runPausedEnvironmentVisualTicks(minecraft, 1);
            active.environmentVisualTicksRemaining--;
        }
    }

    public static void updateMovement(@NotNull Minecraft minecraft) {
        Session active = session;
        if (active == null) {
            return;
        }
        active.updateMovement(minecraft);
    }

    public static void rotateFromMouseDrag(double dx, double dy) {
        Session active = session;
        if (active == null) {
            return;
        }
        active.rotate(dx, dy);
    }

    public static void zoomFromScroll(@NotNull Minecraft minecraft, double scrollY) {
        Session active = session;
        if (active == null) {
            return;
        }
        active.zoomFromScroll(minecraft, scrollY);
    }

    public static void setConfiguredCameraControlKeyState(@NotNull KeyEvent event, boolean down) {
        Session active = session;
        if (active == null) {
            return;
        }
        InputConstants.Key key = InputConstants.getKey(event);
        if (!key.equals(InputConstants.UNKNOWN)) {
            active.setBoundInputState(key, down);
        }
    }

    public static void returnCameraToPlayer(@NotNull Minecraft minecraft) {
        Session active = session;
        if (active == null || minecraft.player == null) {
            return;
        }
        active.returnToPlayer(minecraft.player);
    }

    public static float overrideFieldOfView(float original) {
        Session active = session;
        return active == null ? original : active.fieldOfView();
    }

    public static float overrideBrightness(float original) {
        Session active = session;
        return active == null ? original : active.brightness();
    }

    @Nullable
    public static CameraState cameraState() {
        Session active = session;
        return active == null ? null : active.cameraState();
    }

    public static void beginEnvironmentOverrideScope() {
        environmentOverrideScope = session != null;
    }

    public static void endEnvironmentOverrideScope() {
        environmentOverrideScope = false;
    }

    public static long overrideClockTicks(long original) {
        Session active = session;
        return active != null && environmentOverrideScope ? active.timePreset().clockTicks() : original;
    }

    public static float overrideRainLevel(float original) {
        Session active = session;
        return active != null && environmentOverrideScope ? active.weatherPreset().rainLevel() : original;
    }

    public static float overrideThunderLevel(float original) {
        Session active = session;
        return active != null && environmentOverrideScope ? active.weatherPreset().thunderLevel() : original;
    }

    public static long overrideGameTime(long original) {
        Session active = session;
        return active != null && environmentOverrideScope ? original + active.visualGameTimeOffsetTicks : original;
    }

    public static void applyFogOverrides(@NotNull FogData fog, int renderDistanceInChunks) {
        Session active = session;
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

    public static boolean shouldSuppressEnvironmentRefreshSound(@NotNull SoundSource source) {
        return suppressEnvironmentRefreshSounds && source == SoundSource.WEATHER;
    }

    public static void afterExtractRenderState(@NotNull GameRenderState gameRenderState) {
        Session active = session;
        if (active == null) {
            return;
        }

        LevelRenderState levelRenderState = gameRenderState.levelRenderState;
        levelRenderState.weatherRenderState.intensity = active.weatherPreset().rainLevel();
        levelRenderState.skyRenderState.rainBrightness = 1.0F - active.weatherPreset().rainLevel();
        gameRenderState.lightmapRenderState.needsUpdate = true;
    }

    public static int overrideSkyColor(int sampledSkyColor) {
        Session active = session;
        return active == null || suppressSkyColorOverride ? sampledSkyColor : active.overrideSkyColor(sampledSkyColor);
    }

    public static void extractVignette(@NotNull GuiGraphicsExtractor graphics, int width, int height) {
        Session active = session;
        if (active == null || active.vignette() <= 0.0F) {
            return;
        }

        int alpha = Mth.clamp(Math.round(active.vignette() * 210.0F), 0, 255);
        int renderX = 0;
        int renderY = 0;
        int renderWidth = width;
        int renderHeight = height;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                VIGNETTE_TEXTURE,
                renderX,
                renderY,
                0.0F,
                0.0F,
                renderWidth,
                renderHeight,
                renderWidth,
                renderHeight,
                ARGB.white(alpha / 255.0F)
        );
    }

    public static void processDepthOfFieldEffect(
            @NotNull Minecraft minecraft,
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator,
            @NotNull CameraRenderState cameraState
    ) {
        Session active = session;
        if (active == null || minecraft.level == null || !active.depthOfFieldEnabled()) {
            depthOfFieldProjectionMatrixAvailable = false;
            return;
        }

        Matrix4fc projectionMatrix = depthOfFieldProjectionMatrixAvailable ? depthOfFieldProjectionMatrix : cameraState.projectionMatrix;
        depthOfFieldProjectionMatrixAvailable = false;
        PhotoModeDepthOfFieldRenderer.process(mainRenderTarget, resourceAllocator, cameraState, projectionMatrix, active);
    }

    public static void captureDepthOfFieldProjection(@NotNull Matrix4fc projectionMatrix) {
        Session active = session;
        if (active == null || !active.depthOfFieldEnabled()) {
            depthOfFieldProjectionMatrixAvailable = false;
            return;
        }

        depthOfFieldProjectionMatrix.set(projectionMatrix);
        depthOfFieldProjectionMatrixAvailable = true;
    }

    @SuppressWarnings("deprecation")
    public static void processColorizeEffect(
            @NotNull Minecraft minecraft,
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator
    ) {
        Session active = session;
        if (active == null || minecraft.level == null) {
            return;
        }

        @Nullable Identifier postEffectId = active.colorizePreset().postEffectId();
        if (postEffectId == null) {
            return;
        }

        PostChain postChain = minecraft.getShaderManager().getPostChain(postEffectId, LevelTargetBundle.MAIN_TARGETS);
        if (postChain != null) {
            postChain.process(mainRenderTarget, resourceAllocator);
        }
    }

    public static boolean shouldHidePlayerEntity(@NotNull Entity entity) {
        Session active = session;
        if (active == null || !(entity instanceof Player)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        boolean self = isSelfPlayerEntity(minecraft, entity);
        return self ? active.hideSelfPlayer() : active.hideOtherPlayers();
    }

    public static boolean shouldForceRenderSelfPlayerEntity(@NotNull Entity entity) {
        Session active = session;
        return active != null && !active.hideSelfPlayer() && isSelfPlayerEntity(Minecraft.getInstance(), entity);
    }

    public static void applySelfPlayerRenderStateOverrides(@NotNull Entity entity, @NotNull AvatarRenderState state) {
        Session active = session;
        if (active == null || !isSelfPlayerEntity(Minecraft.getInstance(), entity)) {
            return;
        }

        Vec3 offset = active.selfPlayerPositionOffset();
        if (hasSelfPlayerTransform(offset)) {
            state.x += offset.x;
            state.y += offset.y;
            state.z += offset.z;
            state.distanceToCameraSq = active.position().distanceToSqr(state.x, state.y, state.z);
            state.lightCoords = lightCoordsAt(entity, state.x, state.y, state.z);
        }
    }

    public static void applySelfPlayerModelRotation(@NotNull AvatarRenderState state, @NotNull PoseStack poseStack, float entityScale) {
        Session active = session;
        Minecraft minecraft = Minecraft.getInstance();
        if (active == null || minecraft.player == null || state.id != minecraft.player.getId()) {
            return;
        }

        Vec3 rotation = active.selfPlayerRotationOffset();
        PhotoPose.PartRotation poseRotation = active.activePoseRotation();
        double xRotation = rotation.x + poseRotation.xDegrees();
        double yRotation = rotation.y + poseRotation.yDegrees();
        double zRotation = rotation.z + poseRotation.zDegrees();
        if (!hasSelfPlayerTransform(xRotation, yRotation, zRotation)) {
            return;
        }

        float pivotY = entityScale == 0.0F ? 0.0F : state.boundingBoxHeight / 2.0F / entityScale;
        if (Math.abs(xRotation) > SELF_PLAYER_TRANSFORM_EPSILON) {
            poseStack.rotateAround(Axis.XP.rotationDegrees((float) xRotation), 0.0F, pivotY, 0.0F);
        }
        if (Math.abs(yRotation) > SELF_PLAYER_TRANSFORM_EPSILON) {
            poseStack.rotateAround(Axis.YP.rotationDegrees((float) yRotation), 0.0F, pivotY, 0.0F);
        }
        if (Math.abs(zRotation) > SELF_PLAYER_TRANSFORM_EPSILON) {
            poseStack.rotateAround(Axis.ZP.rotationDegrees((float) zRotation), 0.0F, pivotY, 0.0F);
        }
    }

    public static void applySelfPose(@NotNull PlayerModel model, @NotNull AvatarRenderState state) {
        Session active = session;
        if (active == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || state.id != minecraft.player.getId()) {
            return;
        }

        PhotoPose pose = active.activePose();
        if (pose != null) {
            model.resetPose();
            pose.apply(PhotoPose.PlayerParts.fromModel(model));
        }
    }

    public static void requestScreenshot(@NotNull Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            minecraft.showDebugChat(Component.translatable("panoramica.photo_mode.screenshot.unavailable"));
            return;
        }

        requestScreenshot(minecraft, minecraft.gameDirectory, minecraft.gameRenderer.mainRenderTarget());
    }

    public static void requestScreenshot(@NotNull Minecraft minecraft, @NotNull File workDir, @NotNull RenderTarget target) {
        Consumer<Component> callback = message -> minecraft.execute(() -> minecraft.showDebugChat(message));
        if (!Panoramica.getOptions().areScreenshotChatMessagesEnabled()) {
            callback = message -> minecraft.execute(() -> ScreenshotPreviewManager.acceptDebugChatMessage(message));
        }

        NormalScreenshotCaptureManager.requestHiddenHudScreenshot(
                minecraft,
                workDir,
                target,
                callback,
                ScreenshotMetadataManager.captureNormalScreenshot(minecraft, true)
        );
    }

    @Nullable
    public static HitResult pick(@NotNull Minecraft minecraft) {
        Session active = session;
        if (active == null || minecraft.level == null || minecraft.player == null) {
            return null;
        }

        double blockInteractionRange = minecraft.player.blockInteractionRange();
        double entityInteractionRange = minecraft.player.entityInteractionRange();
        double maxDistance = Math.max(blockInteractionRange, entityInteractionRange);
        double maxDistanceSq = Mth.square(maxDistance);
        Vec3 from = active.position();
        Vec3 direction = active.forwardVector();
        Vec3 to = from.add(direction.scale(maxDistance));
        HitResult blockHitResult = minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, minecraft.player));
        double blockDistanceSq = blockHitResult.getLocation().distanceToSqr(from);
        if (blockHitResult.getType() != Type.MISS) {
            maxDistanceSq = blockDistanceSq;
            maxDistance = Math.sqrt(blockDistanceSq);
            to = from.add(direction.scale(maxDistance));
        }

        AABB rayBox = new AABB(from, to).inflate(1.0D);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                minecraft.player,
                from,
                to,
                rayBox,
                entity -> EntitySelector.CAN_BE_PICKED.test(entity) && !shouldHidePlayerEntity(entity),
                maxDistanceSq
        );

        if (entityHitResult != null && entityHitResult.getLocation().distanceToSqr(from) < blockDistanceSq) {
            return filterHitResult(entityHitResult, from, entityInteractionRange);
        }
        return filterHitResult(blockHitResult, from, blockInteractionRange);
    }

    @NotNull
    private static HitResult filterHitResult(@NotNull HitResult hitResult, @NotNull Vec3 from, double maxRange) {
        Vec3 hitLocation = hitResult.getLocation();
        if (hitLocation.closerThan(from, maxRange)) {
            return hitResult;
        }

        return BlockHitResult.miss(hitLocation, net.minecraft.core.Direction.getApproximateNearest(
                hitLocation.x - from.x,
                hitLocation.y - from.y,
                hitLocation.z - from.z
        ), net.minecraft.core.BlockPos.containing(hitLocation));
    }

    private static boolean isSelfPlayerEntity(@NotNull Minecraft minecraft, @NotNull Entity entity) {
        return minecraft.player != null && entity.getId() == minecraft.player.getId();
    }

    private static boolean hasSelfPlayerTransform(@NotNull Vec3 transform) {
        return hasSelfPlayerTransform(transform.x, transform.y, transform.z);
    }

    private static boolean hasSelfPlayerTransform(double x, double y, double z) {
        return Math.abs(x) > SELF_PLAYER_TRANSFORM_EPSILON
                || Math.abs(y) > SELF_PLAYER_TRANSFORM_EPSILON
                || Math.abs(z) > SELF_PLAYER_TRANSFORM_EPSILON;
    }

    private static double clampFinite(double value, double minValue, double maxValue) {
        return Double.isFinite(value) ? Mth.clamp(value, minValue, maxValue) : 0.0D;
    }

    private static int lightCoordsAt(@NotNull Entity entity, double x, double y, double z) {
        BlockPos blockPos = BlockPos.containing(x, y, z);
        int blockLight = entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, blockPos);
        int skyLight = entity.level().getBrightness(LightLayer.SKY, blockPos);
        return LightCoordsUtil.pack(blockLight, skyLight);
    }

    private static void requestEnvironmentVisualRefresh(@NotNull Minecraft minecraft) {
        Session active = session;
        if (active == null) {
            return;
        }

        active.environmentVisualTicksRemaining = ENVIRONMENT_FOLLOWUP_TICKS;
        runWithEnvironmentOverrideScope(() -> refreshEnvironmentCaches(minecraft, true));
        if (shouldRunPausedEnvironmentVisualRefresh(minecraft)) {
            runPausedEnvironmentVisualTicks(minecraft, ENVIRONMENT_FAST_FORWARD_TICKS);
        }
    }

    private static boolean shouldRunPausedEnvironmentVisualRefresh(@NotNull Minecraft minecraft) {
        Session active = session;
        return active != null
                && active.paused()
                && canPause(minecraft)
                && minecraft.level != null
                && minecraft.player != null
                && minecraft.level.tickRateManager().runsNormally();
    }

    private static void runPausedEnvironmentVisualTicks(@NotNull Minecraft minecraft, int ticks) {
        Session active = session;
        ClientLevel level = minecraft.level;
        if (active == null || level == null || minecraft.player == null || ticks <= 0 || !shouldRunPausedEnvironmentVisualRefresh(minecraft)) {
            return;
        }

        // Visual-only catch-up: do not call ClientLevel.tick() or touch the integrated server.
        boolean previousEnvironmentOverrideScope = environmentOverrideScope;
        boolean previousSuppressEnvironmentRefreshSounds = suppressEnvironmentRefreshSounds;
        environmentOverrideScope = true;
        suppressEnvironmentRefreshSounds = true;
        try {
            for (int tick = 0; tick < ticks; tick++) {
                active.advanceVisualGameTime(1L);
                refreshEnvironmentCaches(minecraft, false);
                level.tickWeatherEffects();
                minecraft.particleEngine.tick();
            }
        } finally {
            environmentOverrideScope = previousEnvironmentOverrideScope;
            suppressEnvironmentRefreshSounds = previousSuppressEnvironmentRefreshSounds;
        }
    }

    private static void runWithEnvironmentOverrideScope(@NotNull Runnable runnable) {
        boolean previousEnvironmentOverrideScope = environmentOverrideScope;
        environmentOverrideScope = session != null;
        try {
            runnable.run();
        } finally {
            environmentOverrideScope = previousEnvironmentOverrideScope;
        }
    }

    private static void refreshEnvironmentCaches(@NotNull Minecraft minecraft, boolean resetProbe) {
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

        Session active = session;
        camera.attributeProbe().tick(level, active == null ? camera.position() : active.position());
    }

    private static int sampleSkyColor(@NotNull Minecraft minecraft, @NotNull Vec3 position) {
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

    public record CameraState(@NotNull Vec3 position, float yaw, float pitch, float roll) {
    }

    public static final class Session {

        private Vec3 position;
        private float yaw;
        private float pitch;
        private float roll;
        private float fieldOfView;
        private float brightness;
        private float vignette;
        private PhotoModeColorizePreset colorizePreset = PhotoModeColorizePreset.NONE;
        private boolean depthOfFieldEnabled;
        private float depthOfFieldFocusDistance = DEPTH_OF_FIELD_FOCUS_DISTANCE_DEFAULT;
        private float depthOfFieldFocalLength = DEPTH_OF_FIELD_FOCAL_LENGTH_DEFAULT;
        private float depthOfFieldAperture = DEPTH_OF_FIELD_APERTURE_DEFAULT;
        private Vec3 selfPlayerPositionOffset = Vec3.ZERO;
        private Vec3 selfPlayerRotationOffset = Vec3.ZERO;
        private boolean hideSelfPlayer;
        private boolean hideOtherPlayers;
        private boolean paused;
        private boolean photoModeUiHidden;
        private boolean gridEnabled;
        private PhotoModeTimePreset timePreset;
        private PhotoModeWeatherPreset weatherPreset;
        private float fogIntensity;
        private float fogDistance = (float) PHOTO_FOG_DEFAULT_DISTANCE;
        private int sampledFogColor = PHOTO_FOG_FALLBACK_COLOR;
        @Nullable
        private Integer fogColorOverride;
        private int sampledSkyColor = PHOTO_SKY_FALLBACK_COLOR;
        @Nullable
        private Integer skyColorOverride;
        @Nullable
        private Identifier poseId;
        @Nullable
        private PhotoPose poseMakerPose;
        private final Set<InputConstants.Key> activeBoundInputs = new HashSet<>();
        private final VisualLightningStorm visualLightningStorm = new VisualLightningStorm();
        private long visualGameTimeOffsetTicks;
        private int environmentVisualTicksRemaining;
        private long lastMovementMillis;

        private Session(
                @NotNull Vec3 position,
                float yaw,
                float pitch,
                float fieldOfView,
                float brightness,
                boolean paused,
                @NotNull PhotoModeTimePreset timePreset,
                @NotNull PhotoModeWeatherPreset weatherPreset,
                int sampledSkyColor
        ) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.fieldOfView = Mth.clamp(fieldOfView, 30.0F, 110.0F);
            this.brightness = Mth.clamp(brightness, 0.0F, 1.0F);
            this.paused = paused;
            this.timePreset = timePreset;
            this.weatherPreset = weatherPreset;
            this.sampledSkyColor = ARGB.opaque(sampledSkyColor);
            this.lastMovementMillis = Util.getMillis();
        }

        @NotNull
        private static Session create(@NotNull Minecraft minecraft) {
            CameraStart start = CameraStart.facingPlayer(minecraft.player);
            return new Session(
                    start.position(),
                    start.yaw(),
                    start.pitch(),
                    minecraft.options.fov().get().floatValue(),
                    minecraft.options.gamma().get().floatValue(),
                    canPause(minecraft),
                    defaultTimePreset(minecraft),
                    defaultWeatherPreset(minecraft),
                    sampleSkyColor(minecraft, start.position())
            );
        }

        private void reset(@NotNull Minecraft minecraft) {
            if (minecraft.player != null) {
                this.returnToPlayer(minecraft.player);
            }
            this.sampledSkyColor = sampleSkyColor(minecraft, this.position);
            this.skyColorOverride = null;
            this.roll = 0.0F;
            this.fieldOfView = minecraft.options.fov().get().floatValue();
            this.brightness = minecraft.options.gamma().get().floatValue();
            this.vignette = 0.0F;
            this.colorizePreset = PhotoModeColorizePreset.NONE;
            this.depthOfFieldEnabled = false;
            this.depthOfFieldFocusDistance = DEPTH_OF_FIELD_FOCUS_DISTANCE_DEFAULT;
            this.depthOfFieldFocalLength = DEPTH_OF_FIELD_FOCAL_LENGTH_DEFAULT;
            this.depthOfFieldAperture = DEPTH_OF_FIELD_APERTURE_DEFAULT;
            this.selfPlayerPositionOffset = Vec3.ZERO;
            this.selfPlayerRotationOffset = Vec3.ZERO;
            this.hideSelfPlayer = false;
            this.hideOtherPlayers = false;
            this.photoModeUiHidden = false;
            this.gridEnabled = false;
            this.poseId = null;
            this.poseMakerPose = null;
            this.timePreset = defaultTimePreset(minecraft);
            this.weatherPreset = defaultWeatherPreset(minecraft);
            this.fogIntensity = 0.0F;
            this.fogDistance = (float) PHOTO_FOG_DEFAULT_DISTANCE;
            this.sampledFogColor = PHOTO_FOG_FALLBACK_COLOR;
            this.fogColorOverride = null;
            this.visualLightningStorm.clear(minecraft.level);
            this.visualGameTimeOffsetTicks = 0L;
            this.environmentVisualTicksRemaining = 0;
            this.activeBoundInputs.clear();
            this.paused = canPause(minecraft);
            this.lastMovementMillis = Util.getMillis();
            requestEnvironmentVisualRefresh(minecraft);
        }

        private void tickVisualEffects(@NotNull Minecraft minecraft) {
            this.visualLightningStorm.tick(minecraft, this);
        }

        private void clearVisualEffects(@Nullable ClientLevel level) {
            this.visualLightningStorm.clear(level);
        }

        private void returnToPlayer(@NotNull Player player) {
            CameraStart start = CameraStart.facingPlayer(player);
            this.position = start.position();
            this.yaw = start.yaw();
            this.pitch = start.pitch();
            this.lastMovementMillis = Util.getMillis();
        }

        private record CameraStart(@NotNull Vec3 position, float yaw, float pitch) {

            @NotNull
            private static CameraStart facingPlayer(@NotNull Player player) {
                Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.78D, 0.0D);
                Vec3 playerForward = Vec3.directionFromRotation(0.0F, player.getVisualRotationYInDegrees()).normalize();
                Vec3 position = player.position()
                        .add(0.0D, player.getEyeHeight(), 0.0D)
                        .add(playerForward.scale(INITIAL_CAMERA_DISTANCE));
                Vec2 rotation = target.subtract(position).rotation();
                return new CameraStart(position, rotation.y, rotation.x);
            }

        }

        @NotNull
        private static PhotoModeTimePreset defaultTimePreset(@NotNull Minecraft minecraft) {
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
        private static PhotoModeWeatherPreset defaultWeatherPreset(@NotNull Minecraft minecraft) {
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

        public void updateMovement(@NotNull Minecraft minecraft) {
            long now = Util.getMillis();
            double deltaSeconds = Math.min(MAX_FRAME_SECONDS, Math.max(0.0D, (now - this.lastMovementMillis) / 1_000.0D));
            this.lastMovementMillis = now;
            if (deltaSeconds <= 0.0D) {
                return;
            }

            float forwardAxis = axis(minecraft, minecraft.options.keyUp, minecraft.options.keyDown);
            float strafeAxis = axis(minecraft, minecraft.options.keyRight, minecraft.options.keyLeft);
            float verticalAxis = 0.0F;
            if (isBoundKeyDown(minecraft, minecraft.options.keyJump)) {
                verticalAxis += 1.0F;
            }
            if (isBoundKeyDown(minecraft, minecraft.options.keyShift)) {
                verticalAxis -= 1.0F;
            }

            if (forwardAxis == 0.0F && strafeAxis == 0.0F && verticalAxis == 0.0F) {
                return;
            }

            Vec3 forward = this.forwardVector();
            Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
            if (right.lengthSqr() < 1.0E-7D) {
                right = Vec3.directionFromRotation(0.0F, this.yaw + 90.0F);
            } else {
                right = right.normalize();
            }
            Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 movement = forward.scale(forwardAxis).add(right.scale(strafeAxis)).add(up.scale(verticalAxis));
            if (movement.lengthSqr() > 1.0D) {
                movement = movement.normalize();
            }
            this.position = this.position.add(movement.scale(this.movementSpeed(minecraft) * deltaSeconds));
        }

        public void zoomFromScroll(@NotNull Minecraft minecraft, double scrollY) {
            if (!Double.isFinite(scrollY) || scrollY == 0.0D) {
                return;
            }
            double notches = Mth.clamp(scrollY, -MAX_SCROLL_ZOOM_NOTCHES, MAX_SCROLL_ZOOM_NOTCHES);
            this.position = this.position.add(this.forwardVector().scale(this.movementSpeed(minecraft) * SCROLL_ZOOM_SECONDS_PER_NOTCH * notches));
            this.lastMovementMillis = Util.getMillis();
        }

        private void setBoundInputState(@NotNull InputConstants.Key key, boolean down) {
            if (down) {
                this.activeBoundInputs.add(key);
            } else {
                this.activeBoundInputs.remove(key);
            }
        }

        private float axis(@NotNull Minecraft minecraft, @NotNull KeyMapping positiveKey, @NotNull KeyMapping negativeKey) {
            float axis = 0.0F;
            if (this.isBoundKeyDown(minecraft, positiveKey)) {
                axis += 1.0F;
            }
            if (this.isBoundKeyDown(minecraft, negativeKey)) {
                axis -= 1.0F;
            }
            return axis;
        }

        private float movementSpeed(@NotNull Minecraft minecraft) {
            float speed = CAMERA_SPEED_BLOCKS_PER_SECOND;
            if (isBoundKeyDown(minecraft, minecraft.options.keySprint)) {
                speed *= CAMERA_FAST_SPEED_MULTIPLIER;
            }
            if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT)) {
                speed *= CAMERA_SLOW_SPEED_MULTIPLIER;
            }
            return speed;
        }

        private boolean isBoundKeyDown(@NotNull Minecraft minecraft, @NotNull KeyMapping keyMapping) {
            InputConstants.Key key = Services.PLATFORM.getKeyMappingKey(keyMapping);
            if (key.equals(InputConstants.UNKNOWN)) {
                return false;
            }
            if (key.getType() == InputConstants.Type.KEYSYM) {
                return InputConstants.isKeyDown(minecraft.getWindow(), key.getValue());
            }
            if (key.getType() == InputConstants.Type.MOUSE) {
                return GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
            }
            return this.activeBoundInputs.contains(key) || keyMapping.isDown();
        }

        public void rotate(double dx, double dy) {
            this.yaw = Mth.wrapDegrees(this.yaw + (float) dx * MOUSE_ROTATION_SENSITIVITY);
            this.pitch = Mth.clamp(this.pitch + (float) dy * MOUSE_ROTATION_SENSITIVITY, -89.5F, 89.5F);
        }

        @NotNull
        public Vec3 forwardVector() {
            return Vec3.directionFromRotation(this.pitch, this.yaw).normalize();
        }

        @NotNull
        public CameraState cameraState() {
            return new CameraState(this.position, this.yaw, this.pitch, this.roll);
        }

        @NotNull
        public Vec3 position() {
            return this.position;
        }

        public float yaw() {
            return this.yaw;
        }

        public float pitch() {
            return this.pitch;
        }

        public float roll() {
            return this.roll;
        }

        public void setRoll(float roll) {
            this.roll = Mth.clamp(roll, -180.0F, 180.0F);
        }

        public float fieldOfView() {
            return this.fieldOfView;
        }

        public void setFieldOfView(float fieldOfView) {
            this.fieldOfView = Mth.clamp(fieldOfView, 30.0F, 110.0F);
        }

        public float brightness() {
            return this.brightness;
        }

        public void setBrightness(float brightness) {
            this.brightness = Mth.clamp(brightness, 0.0F, 1.0F);
        }

        public float vignette() {
            return this.vignette;
        }

        public void setVignette(float vignette) {
            this.vignette = Mth.clamp(vignette, 0.0F, 1.0F);
        }

        @NotNull
        public PhotoModeColorizePreset colorizePreset() {
            return this.colorizePreset;
        }

        public void setColorizePreset(@NotNull PhotoModeColorizePreset colorizePreset) {
            this.colorizePreset = colorizePreset;
        }

        public boolean depthOfFieldEnabled() {
            return this.depthOfFieldEnabled;
        }

        public void setDepthOfFieldEnabled(boolean depthOfFieldEnabled) {
            this.depthOfFieldEnabled = depthOfFieldEnabled;
        }

        public float depthOfFieldFocusDistance() {
            return this.depthOfFieldFocusDistance;
        }

        public void setDepthOfFieldFocusDistance(float depthOfFieldFocusDistance) {
            this.depthOfFieldFocusDistance = Mth.clamp(depthOfFieldFocusDistance, DEPTH_OF_FIELD_FOCUS_DISTANCE_MIN, DEPTH_OF_FIELD_FOCUS_DISTANCE_MAX);
        }

        public float depthOfFieldFocalLength() {
            return this.depthOfFieldFocalLength;
        }

        public void setDepthOfFieldFocalLength(float depthOfFieldFocalLength) {
            this.depthOfFieldFocalLength = Mth.clamp(depthOfFieldFocalLength, DEPTH_OF_FIELD_FOCAL_LENGTH_MIN, DEPTH_OF_FIELD_FOCAL_LENGTH_MAX);
        }

        public float depthOfFieldAperture() {
            return this.depthOfFieldAperture;
        }

        public void setDepthOfFieldAperture(float depthOfFieldAperture) {
            this.depthOfFieldAperture = Mth.clamp(depthOfFieldAperture, DEPTH_OF_FIELD_APERTURE_MIN, DEPTH_OF_FIELD_APERTURE_MAX);
        }

        @NotNull
        public Vec3 selfPlayerPositionOffset() {
            return this.selfPlayerPositionOffset;
        }

        public double selfPlayerPositionOffsetX() {
            return this.selfPlayerPositionOffset.x;
        }

        public void setSelfPlayerPositionOffsetX(double x) {
            this.selfPlayerPositionOffset = new Vec3(
                    clampFinite(x, -SELF_PLAYER_POSITION_OFFSET_RANGE, SELF_PLAYER_POSITION_OFFSET_RANGE),
                    this.selfPlayerPositionOffset.y,
                    this.selfPlayerPositionOffset.z
            );
        }

        public double selfPlayerPositionOffsetY() {
            return this.selfPlayerPositionOffset.y;
        }

        public void setSelfPlayerPositionOffsetY(double y) {
            this.selfPlayerPositionOffset = new Vec3(
                    this.selfPlayerPositionOffset.x,
                    clampFinite(y, -SELF_PLAYER_POSITION_OFFSET_RANGE, SELF_PLAYER_POSITION_OFFSET_RANGE),
                    this.selfPlayerPositionOffset.z
            );
        }

        public double selfPlayerPositionOffsetZ() {
            return this.selfPlayerPositionOffset.z;
        }

        public void setSelfPlayerPositionOffsetZ(double z) {
            this.selfPlayerPositionOffset = new Vec3(
                    this.selfPlayerPositionOffset.x,
                    this.selfPlayerPositionOffset.y,
                    clampFinite(z, -SELF_PLAYER_POSITION_OFFSET_RANGE, SELF_PLAYER_POSITION_OFFSET_RANGE)
            );
        }

        @NotNull
        public Vec3 selfPlayerRotationOffset() {
            return this.selfPlayerRotationOffset;
        }

        public double selfPlayerRotationOffsetX() {
            return this.selfPlayerRotationOffset.x;
        }

        public void setSelfPlayerRotationOffsetX(double x) {
            this.selfPlayerRotationOffset = new Vec3(
                    clampFinite(x, -SELF_PLAYER_ROTATION_OFFSET_RANGE, SELF_PLAYER_ROTATION_OFFSET_RANGE),
                    this.selfPlayerRotationOffset.y,
                    this.selfPlayerRotationOffset.z
            );
        }

        public double selfPlayerRotationOffsetY() {
            return this.selfPlayerRotationOffset.y;
        }

        public void setSelfPlayerRotationOffsetY(double y) {
            this.selfPlayerRotationOffset = new Vec3(
                    this.selfPlayerRotationOffset.x,
                    clampFinite(y, -SELF_PLAYER_ROTATION_OFFSET_RANGE, SELF_PLAYER_ROTATION_OFFSET_RANGE),
                    this.selfPlayerRotationOffset.z
            );
        }

        public double selfPlayerRotationOffsetZ() {
            return this.selfPlayerRotationOffset.z;
        }

        public void setSelfPlayerRotationOffsetZ(double z) {
            this.selfPlayerRotationOffset = new Vec3(
                    this.selfPlayerRotationOffset.x,
                    this.selfPlayerRotationOffset.y,
                    clampFinite(z, -SELF_PLAYER_ROTATION_OFFSET_RANGE, SELF_PLAYER_ROTATION_OFFSET_RANGE)
            );
        }

        public boolean hideSelfPlayer() {
            return this.hideSelfPlayer;
        }

        public void setHideSelfPlayer(boolean hideSelfPlayer) {
            this.hideSelfPlayer = hideSelfPlayer;
        }

        public boolean hideOtherPlayers() {
            return this.hideOtherPlayers;
        }

        public void setHideOtherPlayers(boolean hideOtherPlayers) {
            this.hideOtherPlayers = hideOtherPlayers;
        }

        public boolean paused() {
            return this.paused;
        }

        public void setPaused(boolean paused) {
            this.paused = paused;
        }

        public boolean photoModeUiHidden() {
            return this.photoModeUiHidden;
        }

        public void setPhotoModeUiHidden(boolean photoModeUiHidden) {
            this.photoModeUiHidden = photoModeUiHidden;
        }

        public boolean gridEnabled() {
            return this.gridEnabled;
        }

        public void setGridEnabled(boolean gridEnabled) {
            this.gridEnabled = gridEnabled;
        }

        @NotNull
        public PhotoModeTimePreset timePreset() {
            return this.timePreset;
        }

        public void setTimePreset(@NotNull PhotoModeTimePreset timePreset) {
            if (this.timePreset != timePreset) {
                this.timePreset = timePreset;
                requestEnvironmentVisualRefresh(Minecraft.getInstance());
            }
        }

        @NotNull
        public PhotoModeWeatherPreset weatherPreset() {
            return this.weatherPreset;
        }

        public void setWeatherPreset(@NotNull PhotoModeWeatherPreset weatherPreset) {
            if (this.weatherPreset != weatherPreset) {
                this.weatherPreset = weatherPreset;
                this.visualLightningStorm.clear(Minecraft.getInstance().level);
                requestEnvironmentVisualRefresh(Minecraft.getInstance());
            }
        }

        public float fogIntensity() {
            return this.fogIntensity;
        }

        public void setFogIntensity(float fogIntensity) {
            this.fogIntensity = Mth.clamp(fogIntensity, 0.0F, 1.0F);
        }

        public float fogDistance() {
            return this.fogDistance;
        }

        public void setFogDistance(float fogDistance) {
            this.fogDistance = (float) Mth.clamp(fogDistance, PHOTO_FOG_MIN_DISTANCE, PHOTO_FOG_MAX_DISTANCE);
        }

        public int fogColor() {
            return this.fogColorOverride == null ? this.sampledFogColor : this.fogColorOverride;
        }

        @Nullable
        public Integer fogColorOverride() {
            return this.fogColorOverride;
        }

        public void setFogColor(@Nullable Integer fogColor) {
            this.fogColorOverride = fogColor == null ? null : ARGB.opaque(fogColor);
        }

        public int skyColor() {
            return this.skyColorOverride == null ? this.sampledSkyColor : this.skyColorOverride;
        }

        @Nullable
        public Integer skyColorOverride() {
            return this.skyColorOverride;
        }

        public void setSkyColor(@Nullable Integer skyColor) {
            this.skyColorOverride = skyColor == null ? null : ARGB.opaque(skyColor);
        }

        public void setPoseMakerPose(@Nullable PhotoPose poseMakerPose) {
            this.poseMakerPose = poseMakerPose;
        }

        private int overrideSkyColor(int sampledSkyColor) {
            this.sampledSkyColor = ARGB.opaque(sampledSkyColor);
            return this.skyColor();
        }

        private void sampleFogColor(@NotNull FogData fog) {
            this.sampledFogColor = ARGB.colorFromFloat(1.0F, fog.color.x(), fog.color.y(), fog.color.z());
        }

        private void advanceVisualGameTime(long ticks) {
            this.visualGameTimeOffsetTicks += Math.max(0L, ticks);
        }

        @Nullable
        public Identifier poseId() {
            if (this.poseId != null && !PhotoPoseManager.hasPose(this.poseId)) {
                this.poseId = null;
            }
            return this.poseId;
        }

        public void cyclePose() {
            List<PoseEntry> poses = PhotoPoseManager.poses();
            Identifier current = this.poseId();
            if (poses.isEmpty()) {
                this.poseId = null;
                return;
            }
            if (current == null) {
                this.poseId = poses.getFirst().id();
                return;
            }

            for (int i = 0; i < poses.size(); i++) {
                if (poses.get(i).id().equals(current)) {
                    this.poseId = i + 1 >= poses.size() ? null : poses.get(i + 1).id();
                    return;
                }
            }
            this.poseId = null;
        }

        @Nullable
        private PhotoPose activePose() {
            if (this.poseMakerPose != null) {
                return this.poseMakerPose;
            }
            return PhotoPoseManager.pose(this.poseId());
        }

        @NotNull
        private PhotoPose.PartRotation activePoseRotation() {
            PhotoPose pose = this.activePose();
            return pose == null ? PhotoPose.PartRotation.ZERO : pose.modelRotation();
        }

    }

    private static final class VisualLightningStorm {

        private final RandomSource random = RandomSource.create();
        private final List<Integer> entityIds = new ArrayList<>();
        private int nextLiveStrikeTicks;
        private int farStrikeSequence;
        private boolean nextLiveStrikeFar;
        private boolean pausedStaticMode;

        private void tick(@NotNull Minecraft minecraft, @NotNull Session active) {
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

        private void fillPausedStaticBolts(@NotNull Minecraft minecraft, @NotNull Session active) {
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

        private boolean spawnVisualLightning(@NotNull Minecraft minecraft, @NotNull Session active, @NotNull StrikeDistance distance) {
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
        private Vec3 chooseStrikePosition(@NotNull Minecraft minecraft, @NotNull Session active, @NotNull StrikeDistance distance) {
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

        private void clear(@Nullable ClientLevel level) {
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

}
