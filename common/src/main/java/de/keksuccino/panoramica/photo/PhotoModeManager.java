package de.keksuccino.panoramica.photo;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.capture.NormalScreenshotCaptureManager;
import de.keksuccino.panoramica.metadata.ScreenshotMetadataManager;
import de.keksuccino.panoramica.photo.PhotoPoseManager.PoseEntry;
import de.keksuccino.panoramica.preview.ScreenshotPreviewManager;
import de.keksuccino.panoramica.screen.PhotoModeScreen;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
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
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.List;
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
    private static final int ENVIRONMENT_FAST_FORWARD_TICKS = 40;
    private static final int ENVIRONMENT_FOLLOWUP_TICKS = 5;

    @Nullable
    private static Session session;
    private static boolean environmentOverrideScope;
    private static boolean suppressEnvironmentRefreshSounds;

    private PhotoModeManager() {
    }

    public static void open(@NotNull Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            minecraft.showDebugChat(Component.translatable("panoramica.photo_mode.unavailable"));
            return;
        }

        PhotoPoseManager.reload();
        session = Session.create(minecraft);
        requestEnvironmentVisualRefresh(minecraft);
        minecraft.gui.setScreen(new PhotoModeScreen());
    }

    public static void close() {
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
        return NormalScreenshotCaptureManager.shouldForceHideHud();
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

    public static void extractVignette(@NotNull GuiGraphicsExtractor graphics, int width, int height) {
        Session active = session;
        if (active == null || active.vignette() <= 0.0F || shouldHidePhotoModeUi()) {
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

    public static boolean shouldHidePlayerEntity(@NotNull Entity entity) {
        Session active = session;
        if (active == null || !(entity instanceof Player)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        boolean self = entity.getId() == minecraft.player.getId();
        return self ? active.hideSelfPlayer() : active.hideOtherPlayers();
    }

    public static void applySelfPose(@NotNull PlayerModel model, @NotNull AvatarRenderState state) {
        Session active = session;
        if (active == null || active.poseId() == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || state.id != minecraft.player.getId()) {
            return;
        }

        PhotoPose pose = PhotoPoseManager.pose(active.poseId());
        if (pose != null) {
            pose.apply(new PhotoPose.PlayerParts(model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg));
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

    public record CameraState(@NotNull Vec3 position, float yaw, float pitch, float roll) {
    }

    public static final class Session {

        private Vec3 position;
        private float yaw;
        private float pitch;
        private float roll;
        private float fieldOfView;
        private float vignette;
        private boolean hideSelfPlayer;
        private boolean hideOtherPlayers;
        private boolean paused;
        private PhotoModeTimePreset timePreset;
        private PhotoModeWeatherPreset weatherPreset;
        @Nullable
        private Identifier poseId;
        private long visualGameTimeOffsetTicks;
        private int environmentVisualTicksRemaining;
        private long lastMovementMillis;

        private Session(
                @NotNull Vec3 position,
                float yaw,
                float pitch,
                float fieldOfView,
                boolean paused,
                @NotNull PhotoModeTimePreset timePreset,
                @NotNull PhotoModeWeatherPreset weatherPreset
        ) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.fieldOfView = Mth.clamp(fieldOfView, 30.0F, 110.0F);
            this.paused = paused;
            this.timePreset = timePreset;
            this.weatherPreset = weatherPreset;
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
                    canPause(minecraft),
                    defaultTimePreset(minecraft),
                    defaultWeatherPreset(minecraft)
            );
        }

        private void reset(@NotNull Minecraft minecraft) {
            if (minecraft.player != null) {
                this.returnToPlayer(minecraft.player);
            }
            this.roll = 0.0F;
            this.fieldOfView = minecraft.options.fov().get().floatValue();
            this.vignette = 0.0F;
            this.hideSelfPlayer = false;
            this.hideOtherPlayers = false;
            this.poseId = null;
            this.timePreset = defaultTimePreset(minecraft);
            this.weatherPreset = defaultWeatherPreset(minecraft);
            this.visualGameTimeOffsetTicks = 0L;
            this.environmentVisualTicksRemaining = 0;
            this.paused = canPause(minecraft);
            this.lastMovementMillis = Util.getMillis();
            requestEnvironmentVisualRefresh(minecraft);
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

            float forwardAxis = axis(minecraft, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN);
            float strafeAxis = axis(minecraft, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT);
            float verticalAxis = 0.0F;
            if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_SPACE)) {
                verticalAxis += 1.0F;
            }
            if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL)) {
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

        private static float axis(@NotNull Minecraft minecraft, int positiveKey, int positiveFallbackKey, int negativeKey, int negativeFallbackKey) {
            float axis = 0.0F;
            if (InputConstants.isKeyDown(minecraft.getWindow(), positiveKey) || InputConstants.isKeyDown(minecraft.getWindow(), positiveFallbackKey)) {
                axis += 1.0F;
            }
            if (InputConstants.isKeyDown(minecraft.getWindow(), negativeKey) || InputConstants.isKeyDown(minecraft.getWindow(), negativeFallbackKey)) {
                axis -= 1.0F;
            }
            return axis;
        }

        private float movementSpeed(@NotNull Minecraft minecraft) {
            float speed = CAMERA_SPEED_BLOCKS_PER_SECOND;
            if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT)) {
                speed *= CAMERA_FAST_SPEED_MULTIPLIER;
            }
            if (InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT)) {
                speed *= CAMERA_SLOW_SPEED_MULTIPLIER;
            }
            return speed;
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

        public float vignette() {
            return this.vignette;
        }

        public void setVignette(float vignette) {
            this.vignette = Mth.clamp(vignette, 0.0F, 1.0F);
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
                requestEnvironmentVisualRefresh(Minecraft.getInstance());
            }
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

    }

}
