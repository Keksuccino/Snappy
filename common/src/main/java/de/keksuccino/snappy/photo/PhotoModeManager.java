package de.keksuccino.snappy.photo;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.keksuccino.snappy.KeyMappings;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.capture.NormalScreenshotCaptureManager;
import de.keksuccino.snappy.client.input.PhotoModeCameraController;
import de.keksuccino.snappy.client.render.PhotoEnvironmentManager;
import de.keksuccino.snappy.client.render.VisualLightningStormManager;
import de.keksuccino.snappy.client.render.config.BloomConfig;
import de.keksuccino.snappy.client.render.config.ColorAdjustmentConfig;
import de.keksuccino.snappy.client.render.config.DepthOfFieldConfig;
import de.keksuccino.snappy.client.render.config.StylizeConfig;
import de.keksuccino.snappy.metadata.ScreenshotMetadataManager;
import de.keksuccino.snappy.photo.PhotoPoseManager.PoseEntry;
import de.keksuccino.snappy.preview.ScreenshotPreviewManager;
import de.keksuccino.snappy.screen.PhotoModeScreen;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public final class PhotoModeManager {

    private static final Identifier VIGNETTE_TEXTURE = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "textures/photo_mode/effects/vignette_overlay.png");
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
    public static final float GAMMA_MIN = -1.0F;
    public static final float GAMMA_MAX = 1.0F;
    public static final float GAMMA_DEFAULT = 0.0F;
    public static final float SATURATION_MIN = -1.0F;
    public static final float SATURATION_MAX = 1.0F;
    public static final float SATURATION_DEFAULT = 0.0F;
    public static final float CONTRAST_MIN = -1.0F;
    public static final float CONTRAST_MAX = 1.0F;
    public static final float CONTRAST_DEFAULT = 0.0F;
    public static final float OVEREXPOSURE_MIN = -1.0F;
    public static final float OVEREXPOSURE_MAX = 1.0F;
    public static final float OVEREXPOSURE_DEFAULT = 0.0F;
    public static final float BLOOM_MIN = 0.0F;
    public static final float BLOOM_MAX = 1.0F;
    public static final float BLOOM_DEFAULT = 0.0F;
    public static final double PHOTO_FOG_MIN_DISTANCE = PhotoEnvironmentManager.PHOTO_FOG_MIN_DISTANCE;
    public static final double PHOTO_FOG_MAX_DISTANCE = PhotoEnvironmentManager.PHOTO_FOG_MAX_DISTANCE;
    public static final double PHOTO_FOG_DEFAULT_DISTANCE = PhotoEnvironmentManager.PHOTO_FOG_DEFAULT_DISTANCE;

    @Nullable
    private static Session session;
    private static final Matrix4f depthOfFieldProjectionMatrix = new Matrix4f();
    private static boolean depthOfFieldProjectionMatrixAvailable;

    private PhotoModeManager() {
    }

    public static void open(@NotNull Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            minecraft.showDebugChat(Component.translatable("snappy.photo_mode.unavailable"));
            return;
        }

        Session previous = session;
        if (previous != null) {
            previous.clearVisualEffects(minecraft.level);
        }
        PhotoPoseManager.reload();
        session = Session.create(minecraft);
        requestWorldVisualRefresh(minecraft);
        minecraft.gui.setScreen(new PhotoModeScreen());
    }

    public static void close() {
        Session active = session;
        if (active != null) {
            active.clearVisualEffects(Minecraft.getInstance().level);
        }
        PhotoModeDepthOfFieldRenderer.close();
        PhotoModeColorAdjustmentRenderer.close();
        PhotoModeBloomRenderer.close();
        PhotoModeStylizeRenderer.close();
        session = null;
        PhotoEnvironmentManager.reset(Minecraft.getInstance());
    }

    public static boolean isActive() {
        return session != null;
    }

    @Nullable
    public static Session session() {
        return session;
    }

    @Nullable
    public static PhotoModeState state() {
        Session active = session;
        return active == null ? null : active.state();
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
        while (KeyMappings.KEY_OPEN_PHOTO_MODE.consumeClick()) {
            if (session == null) {
                open(minecraft);
            }
        }
        active = session;
        if (active != null) {
            active.tickVisualEffects(minecraft);
        }
        PhotoEnvironmentManager.tickWorldVisualRefresh(minecraft, active);
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

    @Nullable
    public static CameraState cameraState() {
        Session active = session;
        return active == null ? null : active.cameraState();
    }

    public static void beginWorldOverrideScope() {
        PhotoEnvironmentManager.beginWorldOverrideScope(session);
    }

    public static void endWorldOverrideScope() {
        PhotoEnvironmentManager.endWorldOverrideScope();
    }

    public static long overrideClockTicks(long original) {
        return PhotoEnvironmentManager.overrideClockTicks(session, original);
    }

    public static float overrideRainLevel(float original) {
        return PhotoEnvironmentManager.overrideRainLevel(session, original);
    }

    public static float overrideThunderLevel(float original) {
        return PhotoEnvironmentManager.overrideThunderLevel(session, original);
    }

    public static long overrideGameTime(long original) {
        return PhotoEnvironmentManager.overrideGameTime(session, original);
    }

    public static void applyFogOverrides(@NotNull FogData fog, int renderDistanceInChunks) {
        PhotoEnvironmentManager.applyFogOverrides(session, fog, renderDistanceInChunks);
    }

    public static boolean shouldSuppressWorldRefreshSound(@NotNull SoundSource source) {
        return PhotoEnvironmentManager.shouldSuppressWorldRefreshSound(source);
    }

    public static void afterExtractRenderState(@NotNull GameRenderState gameRenderState) {
        PhotoEnvironmentManager.afterExtractRenderState(session, gameRenderState);
    }

    public static int overrideSkyColor(int sampledSkyColor) {
        return PhotoEnvironmentManager.overrideSkyColor(session, sampledSkyColor);
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
        PhotoModeDepthOfFieldRenderer.process(mainRenderTarget, resourceAllocator, cameraState, projectionMatrix, active.depthOfFieldConfig());
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

    public static void processColorAdjustmentEffect(
            @NotNull Minecraft minecraft,
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator
    ) {
        Session active = session;
        if (active == null || minecraft.level == null || !active.hasColorAdjustments()) {
            return;
        }

        PhotoModeColorAdjustmentRenderer.process(mainRenderTarget, resourceAllocator, active.colorAdjustmentConfig());
    }

    public static void processBloomEffect(
            @NotNull Minecraft minecraft,
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator
    ) {
        Session active = session;
        if (active == null || minecraft.level == null || !active.hasBloom()) {
            return;
        }

        PhotoModeBloomRenderer.process(mainRenderTarget, resourceAllocator, active.bloomConfig());
    }

    public static void processStylizeEffect(
            @NotNull Minecraft minecraft,
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator
    ) {
        Session active = session;
        if (active == null || minecraft.level == null || !active.stylizePreset().appliesShader()) {
            return;
        }

        PhotoModeStylizeRenderer.process(mainRenderTarget, resourceAllocator, active.stylizeConfig());
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

    public static boolean shouldHideBeaconBeams() {
        Session active = session;
        return active != null && active.hideBeaconBeams();
    }

    public static void applySelfPlayerRenderStateOverrides(@NotNull Entity entity, @NotNull AvatarRenderState state, float partialTicks) {
        Session active = session;
        if (active == null || !isSelfPlayerEntity(Minecraft.getInstance(), entity)) {
            return;
        }

        applySelfPlayerArmorMode(active, state);
        applySelfPlayerHeldItemsMode(active, state);

        Vec3 offset = active.selfPlayerPositionOffset();
        double poseYOffset = active.activePoseYOffset();
        if (Math.abs(poseYOffset) > SELF_PLAYER_TRANSFORM_EPSILON) {
            offset = offset.add(0.0D, poseYOffset, 0.0D);
        }
        if (hasSelfPlayerTransform(offset)) {
            state.x += offset.x;
            state.y += offset.y;
            state.z += offset.z;
            state.distanceToCameraSq = active.position().distanceToSqr(state.x, state.y, state.z);
            state.lightCoords = lightCoordsAt(entity, entity.getLightProbePosition(partialTicks).add(offset));
        }
    }

    private static void applySelfPlayerArmorMode(@NotNull Session active, @NotNull AvatarRenderState state) {
        PhotoModeArmorMode armorMode = active.armorMode();
        if (!armorMode.hideHeadSlot() && !armorMode.hideBodySlots()) {
            return;
        }

        if (armorMode.hideHeadSlot()) {
            state.headEquipment = ItemStack.EMPTY;
            state.headItem.clear();
            state.wornHeadType = null;
            state.wornHeadProfile = null;
        }
        if (armorMode.hideBodySlots()) {
            state.chestEquipment = ItemStack.EMPTY;
            state.legsEquipment = ItemStack.EMPTY;
            state.feetEquipment = ItemStack.EMPTY;
        }
    }

    private static void applySelfPlayerHeldItemsMode(@NotNull Session active, @NotNull AvatarRenderState state) {
        PhotoModeHeldItemsMode heldItemsMode = active.heldItemsMode();
        if (heldItemsMode.showMainHand() && heldItemsMode.showOffHand()) {
            return;
        }

        if (!heldItemsMode.showMainHand()) {
            clearHandItemState(state, state.mainArm);
        }
        if (!heldItemsMode.showOffHand()) {
            clearHandItemState(state, state.mainArm.getOpposite());
        }
    }

    private static void clearHandItemState(@NotNull AvatarRenderState state, @NotNull HumanoidArm arm) {
        if (arm == HumanoidArm.RIGHT) {
            state.rightHandItemState.clear();
            state.rightHandItemStack = ItemStack.EMPTY;
        } else {
            state.leftHandItemState.clear();
            state.leftHandItemStack = ItemStack.EMPTY;
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
            minecraft.showDebugChat(Component.translatable("snappy.photo_mode.screenshot.unavailable"));
            return;
        }

        requestScreenshot(minecraft, minecraft.gameDirectory, minecraft.gameRenderer.mainRenderTarget());
    }

    public static void requestScreenshot(@NotNull Minecraft minecraft, @NotNull File workDir, @NotNull RenderTarget target) {
        Consumer<Component> callback = message -> minecraft.execute(() -> minecraft.showDebugChat(message));
        if (!Snappy.getOptions().areScreenshotChatMessagesEnabled()) {
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

    private static int lightCoordsAt(@NotNull Entity entity, @NotNull Vec3 position) {
        BlockPos blockPos = BlockPos.containing(position);
        int blockLight = entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, blockPos);
        int skyLight = entity.level().getBrightness(LightLayer.SKY, blockPos);
        return LightCoordsUtil.pack(blockLight, skyLight);
    }

    private static void requestWorldVisualRefresh(@NotNull Minecraft minecraft) {
        PhotoEnvironmentManager.requestWorldVisualRefresh(minecraft, session);
    }

    public record CameraState(@NotNull Vec3 position, float yaw, float pitch, float roll) {
    }

    public static final class Session implements PhotoModeCameraController.MutableCamera {

        private Vec3 position;
        private float yaw;
        private float pitch;
        private float roll;
        private float fieldOfView;
        private float vignette;
        private PhotoModeColorizePreset colorizePreset = PhotoModeColorizePreset.NONE;
        private PhotoModeStylizePreset stylizePreset = PhotoModeStylizePreset.NONE;
        private float gamma = GAMMA_DEFAULT;
        private float saturation = SATURATION_DEFAULT;
        private float contrast = CONTRAST_DEFAULT;
        private float overexposure = OVEREXPOSURE_DEFAULT;
        private float bloom = BLOOM_DEFAULT;
        private boolean depthOfFieldEnabled;
        private float depthOfFieldFocusDistance = DEPTH_OF_FIELD_FOCUS_DISTANCE_DEFAULT;
        private float depthOfFieldFocalLength = DEPTH_OF_FIELD_FOCAL_LENGTH_DEFAULT;
        private float depthOfFieldAperture = DEPTH_OF_FIELD_APERTURE_DEFAULT;
        private Vec3 selfPlayerPositionOffset = Vec3.ZERO;
        private Vec3 selfPlayerRotationOffset = Vec3.ZERO;
        private boolean hideSelfPlayer;
        private boolean hideOtherPlayers;
        private PhotoModeArmorMode armorMode = PhotoModeArmorMode.SHOW_ALL;
        private PhotoModeHeldItemsMode heldItemsMode = PhotoModeHeldItemsMode.SHOW_BOTH_HANDS;
        private boolean hideBeaconBeams;
        private boolean paused;
        private boolean photoModeUiHidden;
        private boolean gridEnabled;
        private PhotoModeTimePreset timePreset;
        private PhotoModeWeatherPreset weatherPreset;
        private float fogIntensity;
        private float fogDistance = (float) PHOTO_FOG_DEFAULT_DISTANCE;
        private int sampledFogColor = PhotoEnvironmentManager.PHOTO_FOG_FALLBACK_COLOR;
        @Nullable
        private Integer fogColorOverride;
        private int sampledSkyColor = PhotoEnvironmentManager.PHOTO_SKY_FALLBACK_COLOR;
        @Nullable
        private Integer skyColorOverride;
        @Nullable
        private Identifier poseId;
        @Nullable
        private PhotoPose poseMakerPose;
        private final PhotoModeCameraController cameraController = new PhotoModeCameraController();
        private final VisualLightningStormManager visualLightningStorm = new VisualLightningStormManager();
        private long visualGameTimeOffsetTicks;
        private int worldVisualTicksRemaining;

        private Session(
                @NotNull Vec3 position,
                float yaw,
                float pitch,
                float fieldOfView,
                boolean paused,
                @NotNull PhotoModeTimePreset timePreset,
                @NotNull PhotoModeWeatherPreset weatherPreset,
                int sampledSkyColor
        ) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.fieldOfView = Mth.clamp(fieldOfView, 30.0F, 110.0F);
            this.paused = paused;
            this.timePreset = timePreset;
            this.weatherPreset = weatherPreset;
            this.sampledSkyColor = ARGB.opaque(sampledSkyColor);
        }

        @NotNull
        private static Session create(@NotNull Minecraft minecraft) {
            PhotoModeCameraController.CameraStart start = PhotoModeCameraController.facingPlayer(minecraft.player);
            return new Session(
                    start.position(),
                    start.yaw(),
                    start.pitch(),
                    minecraft.options.fov().get().floatValue(),
                    canPause(minecraft),
                    PhotoEnvironmentManager.defaultTimePreset(minecraft),
                    PhotoEnvironmentManager.defaultWeatherPreset(minecraft),
                    PhotoEnvironmentManager.sampleSkyColor(minecraft, start.position())
            );
        }

        private void reset(@NotNull Minecraft minecraft) {
            if (minecraft.player != null) {
                this.returnToPlayer(minecraft.player);
            }
            this.sampledSkyColor = PhotoEnvironmentManager.sampleSkyColor(minecraft, this.position);
            this.skyColorOverride = null;
            this.roll = 0.0F;
            this.fieldOfView = minecraft.options.fov().get().floatValue();
            this.vignette = 0.0F;
            this.colorizePreset = PhotoModeColorizePreset.NONE;
            this.stylizePreset = PhotoModeStylizePreset.NONE;
            this.gamma = GAMMA_DEFAULT;
            this.saturation = SATURATION_DEFAULT;
            this.contrast = CONTRAST_DEFAULT;
            this.overexposure = OVEREXPOSURE_DEFAULT;
            this.bloom = BLOOM_DEFAULT;
            this.depthOfFieldEnabled = false;
            this.depthOfFieldFocusDistance = DEPTH_OF_FIELD_FOCUS_DISTANCE_DEFAULT;
            this.depthOfFieldFocalLength = DEPTH_OF_FIELD_FOCAL_LENGTH_DEFAULT;
            this.depthOfFieldAperture = DEPTH_OF_FIELD_APERTURE_DEFAULT;
            this.selfPlayerPositionOffset = Vec3.ZERO;
            this.selfPlayerRotationOffset = Vec3.ZERO;
            this.hideSelfPlayer = false;
            this.hideOtherPlayers = false;
            this.armorMode = PhotoModeArmorMode.SHOW_ALL;
            this.heldItemsMode = PhotoModeHeldItemsMode.SHOW_BOTH_HANDS;
            this.hideBeaconBeams = false;
            this.photoModeUiHidden = false;
            this.gridEnabled = false;
            this.poseId = null;
            this.poseMakerPose = null;
            this.timePreset = PhotoEnvironmentManager.defaultTimePreset(minecraft);
            this.weatherPreset = PhotoEnvironmentManager.defaultWeatherPreset(minecraft);
            this.fogIntensity = 0.0F;
            this.fogDistance = (float) PHOTO_FOG_DEFAULT_DISTANCE;
            this.sampledFogColor = PhotoEnvironmentManager.PHOTO_FOG_FALLBACK_COLOR;
            this.fogColorOverride = null;
            this.visualLightningStorm.clear(minecraft.level);
            this.visualGameTimeOffsetTicks = 0L;
            this.worldVisualTicksRemaining = 0;
            this.cameraController.resetInputState();
            this.paused = canPause(minecraft);
            requestWorldVisualRefresh(minecraft);
        }

        private void tickVisualEffects(@NotNull Minecraft minecraft) {
            this.visualLightningStorm.tick(minecraft, this);
        }

        private void clearVisualEffects(@Nullable ClientLevel level) {
            this.visualLightningStorm.clear(level);
        }

        private void returnToPlayer(@NotNull Player player) {
            PhotoModeCameraController.CameraStart start = PhotoModeCameraController.facingPlayer(player);
            this.position = start.position();
            this.yaw = start.yaw();
            this.pitch = start.pitch();
            this.cameraController.markMoved();
        }

        public void updateMovement(@NotNull Minecraft minecraft) {
            this.cameraController.updateMovement(minecraft, this);
        }

        public void zoomFromScroll(@NotNull Minecraft minecraft, double scrollY) {
            this.cameraController.zoomFromScroll(minecraft, this, scrollY);
        }

        private void setBoundInputState(@NotNull InputConstants.Key key, boolean down) {
            this.cameraController.setBoundInputState(key, down);
        }

        public void rotate(double dx, double dy) {
            this.cameraController.rotate(this, dx, dy);
        }

        @NotNull
        public Vec3 forwardVector() {
            return this.cameraController.forwardVector(this);
        }

        @NotNull
        public CameraState cameraState() {
            return new CameraState(this.position, this.yaw, this.pitch, this.roll);
        }

        @NotNull
        public PhotoModeState state() {
            return new PhotoModeState(
                    this.cameraState(),
                    this.fieldOfView,
                    this.vignette,
                    this.colorizePreset,
                    this.colorAdjustmentConfig(),
                    this.bloomConfig(),
                    this.stylizeConfig(),
                    this.depthOfFieldConfig(),
                    this.selfPlayerPositionOffset,
                    this.selfPlayerRotationOffset,
                    this.hideSelfPlayer,
                    this.hideOtherPlayers,
                    this.armorMode,
                    this.heldItemsMode,
                    this.hideBeaconBeams,
                    this.paused,
                    this.photoModeUiHidden,
                    this.gridEnabled,
                    this.timePreset,
                    this.weatherPreset,
                    this.fogIntensity,
                    this.fogDistance,
                    this.fogColor(),
                    this.skyColor(),
                    this.poseId()
            );
        }

        @NotNull
        public Vec3 position() {
            return this.position;
        }

        @Override
        public void setPosition(@NotNull Vec3 position) {
            this.position = position;
        }

        public float yaw() {
            return this.yaw;
        }

        @Override
        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public float pitch() {
            return this.pitch;
        }

        @Override
        public void setPitch(float pitch) {
            this.pitch = pitch;
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

        public float gamma() {
            return this.gamma;
        }

        public void setGamma(float gamma) {
            this.gamma = Mth.clamp(gamma, GAMMA_MIN, GAMMA_MAX);
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

        @NotNull
        public PhotoModeStylizePreset stylizePreset() {
            return this.stylizePreset;
        }

        public void setStylizePreset(@NotNull PhotoModeStylizePreset stylizePreset) {
            this.stylizePreset = stylizePreset;
        }

        public float saturation() {
            return this.saturation;
        }

        public void setSaturation(float saturation) {
            this.saturation = Mth.clamp(saturation, SATURATION_MIN, SATURATION_MAX);
        }

        public float contrast() {
            return this.contrast;
        }

        public void setContrast(float contrast) {
            this.contrast = Mth.clamp(contrast, CONTRAST_MIN, CONTRAST_MAX);
        }

        public float overexposure() {
            return this.overexposure;
        }

        public void setOverexposure(float overexposure) {
            this.overexposure = Mth.clamp(overexposure, OVEREXPOSURE_MIN, OVEREXPOSURE_MAX);
        }

        public float bloom() {
            return this.bloom;
        }

        public void setBloom(float bloom) {
            this.bloom = Mth.clamp(bloom, BLOOM_MIN, BLOOM_MAX);
        }

        public boolean hasColorAdjustments() {
            return this.colorAdjustmentConfig().active();
        }

        public boolean hasBloom() {
            return this.bloomConfig().active();
        }

        @NotNull
        public ColorAdjustmentConfig colorAdjustmentConfig() {
            return new ColorAdjustmentConfig(this.saturation, this.contrast, this.overexposure, this.gamma);
        }

        @NotNull
        public BloomConfig bloomConfig() {
            return new BloomConfig(this.bloom);
        }

        @NotNull
        public StylizeConfig stylizeConfig() {
            return new StylizeConfig(this.stylizePreset);
        }

        public boolean depthOfFieldEnabled() {
            return this.depthOfFieldEnabled;
        }

        @NotNull
        public DepthOfFieldConfig depthOfFieldConfig() {
            return new DepthOfFieldConfig(
                    this.depthOfFieldEnabled,
                    this.depthOfFieldFocusDistance,
                    this.depthOfFieldFocalLength,
                    this.depthOfFieldAperture
            );
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

        @NotNull
        public PhotoModeArmorMode armorMode() {
            return this.armorMode;
        }

        public void setArmorMode(@NotNull PhotoModeArmorMode armorMode) {
            this.armorMode = armorMode;
        }

        @NotNull
        public PhotoModeHeldItemsMode heldItemsMode() {
            return this.heldItemsMode;
        }

        public void setHeldItemsMode(@NotNull PhotoModeHeldItemsMode heldItemsMode) {
            this.heldItemsMode = heldItemsMode;
        }

        public boolean hideBeaconBeams() {
            return this.hideBeaconBeams;
        }

        public void setHideBeaconBeams(boolean hideBeaconBeams) {
            this.hideBeaconBeams = hideBeaconBeams;
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
                requestWorldVisualRefresh(Minecraft.getInstance());
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
                requestWorldVisualRefresh(Minecraft.getInstance());
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

        public int overrideSkyColor(int sampledSkyColor) {
            this.sampledSkyColor = ARGB.opaque(sampledSkyColor);
            return this.skyColor();
        }

        public void sampleFogColor(@NotNull FogData fog) {
            this.sampledFogColor = ARGB.colorFromFloat(1.0F, fog.color.x(), fog.color.y(), fog.color.z());
        }

        public long visualGameTimeOffsetTicks() {
            return this.visualGameTimeOffsetTicks;
        }

        public int worldVisualTicksRemaining() {
            return this.worldVisualTicksRemaining;
        }

        public void setWorldVisualTicksRemaining(int worldVisualTicksRemaining) {
            this.worldVisualTicksRemaining = Math.max(0, worldVisualTicksRemaining);
        }

        public void advanceVisualGameTime(long ticks) {
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

        private double activePoseYOffset() {
            PhotoPose pose = this.activePose();
            return pose == null ? 0.0D : pose.modelYOffset();
        }

    }

}
