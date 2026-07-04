package de.keksuccino.panoramica.photo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.panoramica.Panoramica;
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

    private static final String POSE_DIRECTORY = "photo_poses";
    private static final String NAME_KEY = "name";
    private static final String PARTS_KEY = "parts";

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
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (!rootElement.isJsonObject()) {
                    throw new IllegalArgumentException("Root must be an object.");
                }
                loadedPoses.add(new PoseEntry(toPoseId(resourceId), parse(rootElement.getAsJsonObject())));
            } catch (Exception ex) {
                Panoramica.getLogger().warn("[PANORAMICA] Could not load photo pose '{}'.", resourceId, ex);
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
    private static PhotoPose parse(@NotNull JsonObject root) {
        String nameKey = GsonHelper.getAsString(root, NAME_KEY);
        JsonObject parts = GsonHelper.getAsJsonObject(root, PARTS_KEY, new JsonObject());
        Map<PhotoPose.BodyPart, PhotoPose.PartRotation> rotations = PhotoPose.emptyRotationMap();
        for (Map.Entry<String, JsonElement> entry : parts.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                throw new IllegalArgumentException("Body part '" + entry.getKey() + "' must be an object.");
            }
            JsonObject rotation = entry.getValue().getAsJsonObject();
            rotations.put(PhotoPose.BodyPart.fromJsonName(entry.getKey()), PhotoPose.PartRotation.degrees(
                    GsonHelper.getAsFloat(rotation, "x", 0.0F),
                    GsonHelper.getAsFloat(rotation, "y", 0.0F),
                    GsonHelper.getAsFloat(rotation, "z", 0.0F)
            ));
        }
        return new PhotoPose(nameKey, Map.copyOf(rotations));
    }

    public record PoseEntry(@NotNull Identifier id, @NotNull PhotoPose pose) {
    }

}
