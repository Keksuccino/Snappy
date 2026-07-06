package de.keksuccino.snappy.photo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.snappy.Snappy;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PhotoPoseManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String POSE_DIRECTORY = "photo_poses";
    private static final String NAME_KEY = "name";
    private static final String MODEL_KEY = "model";
    private static final String MODEL_Y_OFFSET_KEY = "y_offset";
    private static final String PARTS_KEY = "parts";
    private static final float JSON_ROTATION_EPSILON = 1.0E-4F;
    private static final double JSON_OFFSET_EPSILON = 1.0E-4D;

    private static List<PoseEntry> poses = List.of();
    private static boolean loaded;

    private PhotoPoseManager() {
    }

    @NotNull
    public static List<PoseEntry> poses() {
        ensureLoaded();
        return poses;
    }

    public static boolean hasPose(@Nullable Identifier id) {
        if (id == null) {
            return false;
        }
        return poses().stream().anyMatch(entry -> entry.id().equals(id));
    }

    @Nullable
    public static PhotoPose pose(@Nullable Identifier id) {
        if (id == null) {
            return null;
        }
        return poses().stream()
                .filter(entry -> entry.id().equals(id))
                .map(PoseEntry::pose)
                .findFirst()
                .orElse(null);
    }

    public static void reload() {
        loaded = false;
        ensureLoaded();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            poses = List.of();
            return;
        }

        poses = load(minecraft.getResourceManager());
    }

    @NotNull
    private static List<PoseEntry> load(@NotNull ResourceManager resourceManager) {
        List<PoseEntry> loadedPoses = new ArrayList<>();
        Map<Identifier, Resource> resources = resourceManager.listResources(POSE_DIRECTORY, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                loadedPoses.add(new PoseEntry(toPoseId(resourceId), parseRoot(JsonParser.parseReader(reader))));
            } catch (Exception ex) {
                Snappy.getLogger().warn("[SNAPPY] Could not load photo pose '{}'.", resourceId, ex);
            }
        }

        loadedPoses.sort(Comparator.comparing(entry -> entry.id().toString()));
        return List.copyOf(loadedPoses);
    }

    @NotNull
    private static Identifier toPoseId(@NotNull Identifier resourceId) {
        String path = resourceId.getPath();
        if (path.startsWith(POSE_DIRECTORY + "/")) {
            path = path.substring(POSE_DIRECTORY.length() + 1);
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    @NotNull
    public static PhotoPose fromJsonString(@NotNull String json) {
        return parseRoot(JsonParser.parseString(json));
    }

    @NotNull
    private static PhotoPose parseRoot(@NotNull JsonElement rootElement) {
        if (!rootElement.isJsonObject()) {
            throw new IllegalArgumentException("Root must be an object.");
        }
        return parse(rootElement.getAsJsonObject());
    }

    @NotNull
    private static PhotoPose parse(@NotNull JsonObject root) {
        String nameKey = GsonHelper.getAsString(root, NAME_KEY);
        JsonObject model = GsonHelper.getAsJsonObject(root, MODEL_KEY, new JsonObject());
        PhotoPose.PartRotation modelRotation = parseRotation(model);
        double modelYOffset = GsonHelper.getAsDouble(model, MODEL_Y_OFFSET_KEY, 0.0D);
        JsonObject parts = GsonHelper.getAsJsonObject(root, PARTS_KEY, new JsonObject());
        Map<PhotoPose.BodyPart, PhotoPose.PartRotation> rotations = PhotoPose.emptyRotationMap();
        for (Map.Entry<String, JsonElement> entry : parts.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                throw new IllegalArgumentException("Body part '" + entry.getKey() + "' must be an object.");
            }
            rotations.put(PhotoPose.BodyPart.fromJsonName(entry.getKey()), parseRotation(entry.getValue().getAsJsonObject()));
        }
        return new PhotoPose(nameKey, modelRotation, modelYOffset, Map.copyOf(rotations));
    }

    @NotNull
    private static PhotoPose.PartRotation parseRotation(@NotNull JsonObject rotation) {
        return PhotoPose.PartRotation.degrees(
                GsonHelper.getAsFloat(rotation, "x", 0.0F),
                GsonHelper.getAsFloat(rotation, "y", 0.0F),
                GsonHelper.getAsFloat(rotation, "z", 0.0F)
        );
    }

    @NotNull
    public static String toJsonString(@NotNull PhotoPose pose) {
        return GSON.toJson(toJson(pose));
    }

    @NotNull
    private static JsonObject toJson(@NotNull PhotoPose pose) {
        JsonObject root = new JsonObject();
        root.addProperty(NAME_KEY, pose.nameKey());
        addModel(root, pose);

        JsonObject parts = new JsonObject();
        for (PhotoPose.BodyPart part : PhotoPose.BodyPart.values()) {
            PhotoPose.PartRotation rotation = pose.rotations().get(part);
            if (rotation != null && !rotation.isZero()) {
                addRotation(parts, part.jsonName(), rotation);
            }
        }
        root.add(PARTS_KEY, parts);
        return root;
    }

    private static void addModel(@NotNull JsonObject root, @NotNull PhotoPose pose) {
        JsonObject model = new JsonObject();
        addRotationAxes(model, pose.modelRotation());
        addOffsetAxis(model, MODEL_Y_OFFSET_KEY, pose.modelYOffset());
        if (!model.entrySet().isEmpty()) {
            root.add(MODEL_KEY, model);
        }
    }

    private static void addRotation(@NotNull JsonObject parent, @NotNull String key, @NotNull PhotoPose.PartRotation rotation) {
        if (rotation.isZero()) {
            return;
        }

        JsonObject object = new JsonObject();
        addRotationAxes(object, rotation);
        parent.add(key, object);
    }

    private static void addRotationAxes(@NotNull JsonObject object, @NotNull PhotoPose.PartRotation rotation) {
        addAxis(object, "x", rotation.xDegrees());
        addAxis(object, "y", rotation.yDegrees());
        addAxis(object, "z", rotation.zDegrees());
    }

    private static void addAxis(@NotNull JsonObject object, @NotNull String key, float value) {
        if (Math.abs(value) > JSON_ROTATION_EPSILON) {
            object.addProperty(key, roundJsonDegrees(value));
        }
    }

    private static void addOffsetAxis(@NotNull JsonObject object, @NotNull String key, double value) {
        if (Math.abs(value) > JSON_OFFSET_EPSILON) {
            object.addProperty(key, roundJsonBlocks(value));
        }
    }

    private static float roundJsonDegrees(float value) {
        return Math.round(value * 1000.0F) / 1000.0F;
    }

    private static double roundJsonBlocks(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }

    public record PoseEntry(@NotNull Identifier id, @NotNull PhotoPose pose) {
    }

}
