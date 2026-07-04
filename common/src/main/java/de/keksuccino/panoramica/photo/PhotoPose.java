package de.keksuccino.panoramica.photo;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public record PhotoPose(@NotNull String nameKey, @NotNull Map<BodyPart, PartRotation> rotations) {

    @NotNull
    public Map<BodyPart, PartRotation> rotations() {
        return this.rotations;
    }

    public void apply(@NotNull PlayerParts parts) {
        for (Map.Entry<BodyPart, PartRotation> entry : this.rotations.entrySet()) {
            ModelPart part = switch (entry.getKey()) {
                case HEAD -> parts.head();
                case BODY -> parts.body();
                case LEFT_ARM -> parts.leftArm();
                case RIGHT_ARM -> parts.rightArm();
                case LEFT_LEG -> parts.leftLeg();
                case RIGHT_LEG -> parts.rightLeg();
            };
            entry.getValue().apply(part);
        }
    }

    @NotNull
    public static Map<BodyPart, PartRotation> emptyRotationMap() {
        return new EnumMap<>(BodyPart.class);
    }

    public enum BodyPart {
        HEAD("head"),
        BODY("body"),
        LEFT_ARM("left_arm"),
        RIGHT_ARM("right_arm"),
        LEFT_LEG("left_leg"),
        RIGHT_LEG("right_leg");

        private final String jsonName;

        BodyPart(@NotNull String jsonName) {
            this.jsonName = jsonName;
        }

        @NotNull
        public String jsonName() {
            return this.jsonName;
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

        public void apply(@NotNull ModelPart part) {
            part.xRot += this.x;
            part.yRot += this.y;
            part.zRot += this.z;
        }

        public static PartRotation degrees(float x, float y, float z) {
            float factor = (float) (Math.PI / 180.0);
            return new PartRotation(x * factor, y * factor, z * factor);
        }

    }

    public record PlayerParts(
            @NotNull ModelPart head,
            @NotNull ModelPart body,
            @NotNull ModelPart leftArm,
            @NotNull ModelPart rightArm,
            @NotNull ModelPart leftLeg,
            @NotNull ModelPart rightLeg
    ) {
    }

}
