package de.keksuccino.snappy.photo;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record PhotoPose(
        @NotNull String nameKey,
        @NotNull PartRotation modelRotation,
        double modelYOffset,
        @NotNull Map<BodyPart, PartRotation> rotations
) {

    public static final double MODEL_Y_OFFSET_MIN = -5.0D;
    public static final double MODEL_Y_OFFSET_MAX = 5.0D;

    public PhotoPose(@NotNull String nameKey, @NotNull Map<BodyPart, PartRotation> rotations) {
        this(nameKey, PartRotation.ZERO, 0.0D, rotations);
    }

    public PhotoPose(@NotNull String nameKey, @NotNull PartRotation modelRotation, @NotNull Map<BodyPart, PartRotation> rotations) {
        this(nameKey, modelRotation, 0.0D, rotations);
    }

    public PhotoPose {
        nameKey = Objects.requireNonNull(nameKey, "nameKey");
        modelRotation = Objects.requireNonNull(modelRotation, "modelRotation");
        modelYOffset = clampModelYOffset(modelYOffset);
        rotations = Map.copyOf(Objects.requireNonNull(rotations, "rotations"));
    }

    @NotNull
    public Map<BodyPart, PartRotation> rotations() {
        return this.rotations;
    }

    public static double clampModelYOffset(double value) {
        return Double.isFinite(value) ? Mth.clamp(value, MODEL_Y_OFFSET_MIN, MODEL_Y_OFFSET_MAX) : 0.0D;
    }

    public void apply(@NotNull PlayerParts parts) {
        for (Map.Entry<BodyPart, PartRotation> entry : this.rotations.entrySet()) {
            ModelPart part = switch (entry.getKey()) {
                case HEAD -> parts.head();
                case HAT -> parts.hat();
                case BODY -> parts.body();
                case JACKET -> parts.jacket();
                case LEFT_ARM -> parts.leftArm();
                case LEFT_SLEEVE -> parts.leftSleeve();
                case RIGHT_ARM -> parts.rightArm();
                case RIGHT_SLEEVE -> parts.rightSleeve();
                case LEFT_LEG -> parts.leftLeg();
                case LEFT_PANTS -> parts.leftPants();
                case RIGHT_LEG -> parts.rightLeg();
                case RIGHT_PANTS -> parts.rightPants();
            };
            entry.getValue().apply(part);
        }
    }

    @NotNull
    public static Map<BodyPart, PartRotation> emptyRotationMap() {
        return new EnumMap<>(BodyPart.class);
    }

    public enum BodyPart {
        HEAD("head", "snappy.photo_mode.pose_maker.part.head"),
        HAT("hat", "snappy.photo_mode.pose_maker.part.hat"),
        BODY("body", "snappy.photo_mode.pose_maker.part.body"),
        JACKET("jacket", "snappy.photo_mode.pose_maker.part.jacket"),
        LEFT_ARM("left_arm", "snappy.photo_mode.pose_maker.part.left_arm"),
        LEFT_SLEEVE("left_sleeve", "snappy.photo_mode.pose_maker.part.left_sleeve"),
        RIGHT_ARM("right_arm", "snappy.photo_mode.pose_maker.part.right_arm"),
        RIGHT_SLEEVE("right_sleeve", "snappy.photo_mode.pose_maker.part.right_sleeve"),
        LEFT_LEG("left_leg", "snappy.photo_mode.pose_maker.part.left_leg"),
        LEFT_PANTS("left_pants", "snappy.photo_mode.pose_maker.part.left_pants"),
        RIGHT_LEG("right_leg", "snappy.photo_mode.pose_maker.part.right_leg"),
        RIGHT_PANTS("right_pants", "snappy.photo_mode.pose_maker.part.right_pants");

        private final String jsonName;
        private final String labelKey;

        BodyPart(@NotNull String jsonName, @NotNull String labelKey) {
            this.jsonName = jsonName;
            this.labelKey = labelKey;
        }

        @NotNull
        public String jsonName() {
            return this.jsonName;
        }

        @NotNull
        public String labelKey() {
            return this.labelKey;
        }

        @NotNull
        public static BodyPart fromJsonName(@NotNull String name) {
            for (BodyPart part : values()) {
                if (part.jsonName.equals(name)) {
                    return part;
                }
            }
            throw new IllegalArgumentException("Unknown player body part: " + name);
        }
    }

    public record PartRotation(float x, float y, float z) {

        public static final PartRotation ZERO = new PartRotation(0.0F, 0.0F, 0.0F);

        public void apply(@NotNull ModelPart part) {
            part.xRot += this.x;
            part.yRot += this.y;
            part.zRot += this.z;
        }

        public boolean isZero() {
            return this.x == 0.0F && this.y == 0.0F && this.z == 0.0F;
        }

        public float xDegrees() {
            return (float) Math.toDegrees(this.x);
        }

        public float yDegrees() {
            return (float) Math.toDegrees(this.y);
        }

        public float zDegrees() {
            return (float) Math.toDegrees(this.z);
        }

        public static PartRotation degrees(float x, float y, float z) {
            float factor = (float) (Math.PI / 180.0);
            return new PartRotation(x * factor, y * factor, z * factor);
        }

    }

    public record PlayerParts(
            @NotNull ModelPart head,
            @NotNull ModelPart hat,
            @NotNull ModelPart body,
            @NotNull ModelPart jacket,
            @NotNull ModelPart leftArm,
            @NotNull ModelPart leftSleeve,
            @NotNull ModelPart rightArm,
            @NotNull ModelPart rightSleeve,
            @NotNull ModelPart leftLeg,
            @NotNull ModelPart leftPants,
            @NotNull ModelPart rightLeg,
            @NotNull ModelPart rightPants
    ) {

        @NotNull
        public static PlayerParts fromModel(@NotNull PlayerModel model) {
            return new PlayerParts(
                    model.head,
                    model.hat,
                    model.body,
                    model.jacket,
                    model.leftArm,
                    model.leftSleeve,
                    model.rightArm,
                    model.rightSleeve,
                    model.leftLeg,
                    model.leftPants,
                    model.rightLeg,
                    model.rightPants
            );
        }

    }

}
