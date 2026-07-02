package de.keksuccino.panoramica.screen;

import de.keksuccino.panoramica.Panoramica;
import de.keksuccino.panoramica.screen.ScreenshotBrowserCatalog.ScreenshotEntry;
import de.keksuccino.panoramica.screen.ScreenshotImageLoader.LoadedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ScreenshotThumbnailCache implements AutoCloseable {

    private static final String TEXTURE_PATH = "dynamic/screenshot_browser/thumbnail/";
    private static final int MAX_MAIN_THREAD_COMPLETIONS_PER_FRAME = 2;
    private static int textureSequence;

    private final Minecraft minecraft;
    private final Map<ScreenshotEntry, ThumbnailState> thumbnails = new HashMap<>();
    private final ConcurrentLinkedQueue<PendingLoad> pendingLoads = new ConcurrentLinkedQueue<>();
    private boolean closed;

    public ScreenshotThumbnailCache(@NotNull Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @NotNull
    public Thumbnail thumbnailFor(@NotNull ScreenshotEntry entry) {
        ThumbnailState state = this.thumbnails.computeIfAbsent(entry, ignored -> {
            ThumbnailState newState = new ThumbnailState();
            this.load(entry, newState);
            return newState;
        });

        return switch (state.status) {
            case READY -> new Thumbnail.Ready(state.textureId, state.width, state.height, state.sourceWidth, state.sourceHeight);
            case FAILED -> Thumbnail.FAILED;
            case LOADING -> Thumbnail.LOADING;
        };
    }

    public void remove(@NotNull ScreenshotEntry entry) {
        ThumbnailState state = this.thumbnails.remove(entry);
        if (state != null) {
            this.release(state);
        }
    }

    public void retainOnly(@NotNull Iterable<ScreenshotEntry> entries) {
        Map<ScreenshotEntry, Boolean> retained = new HashMap<>();
        for (ScreenshotEntry entry : entries) {
            retained.put(entry, Boolean.TRUE);
        }

        Iterator<Map.Entry<ScreenshotEntry, ThumbnailState>> iterator = this.thumbnails.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ScreenshotEntry, ThumbnailState> mapEntry = iterator.next();
            if (!retained.containsKey(mapEntry.getKey())) {
                this.release(mapEntry.getValue());
                iterator.remove();
            }
        }

        this.drainPendingLoads();
    }

    private void load(@NotNull ScreenshotEntry entry, @NotNull ThumbnailState state) {
        Util.ioPool().execute(() -> {
            byte[] imageBytes = null;
            Throwable failure = null;
            try {
                imageBytes = ScreenshotImageLoader.readThumbnailBytes(entry);
            } catch (Throwable ex) {
                failure = ex;
            }

            byte[] result = imageBytes;
            Throwable error = failure;
            this.pendingLoads.add(new PendingLoad(entry, state, result, error));
        });
    }

    private void drainPendingLoads() {
        if (this.closed) {
            this.pendingLoads.clear();
            return;
        }

        for (int i = 0; i < MAX_MAIN_THREAD_COMPLETIONS_PER_FRAME; i++) {
            PendingLoad pendingLoad = this.pendingLoads.poll();
            if (pendingLoad == null) {
                return;
            }
            this.completeLoad(pendingLoad.entry(), pendingLoad.state(), pendingLoad.imageBytes(), pendingLoad.error());
        }
    }

    private void completeLoad(
            @NotNull ScreenshotEntry entry,
            @NotNull ThumbnailState state,
            byte @Nullable [] imageBytes,
            @Nullable Throwable error
    ) {
        if (this.closed || this.thumbnails.get(entry) != state) {
            return;
        }

        if (error != null || imageBytes == null) {
            state.status = Status.FAILED;
            if (error != null) {
                Panoramica.getLogger().warn("[PANORAMICA] Could not load screenshot thumbnail {}.", entry.path(), error);
            }
            return;
        }

        Identifier textureId = nextTextureId();
        LoadedImage loadedImage = null;
        ScreenshotImageTexture texture = null;
        try {
            loadedImage = ScreenshotImageLoader.decodeThumbnail(entry, imageBytes);
            texture = new ScreenshotImageTexture("Panoramica screenshot thumbnail", loadedImage.image());
            int width = texture.getPixels().getWidth();
            int height = texture.getPixels().getHeight();
            this.minecraft.getTextureManager().register(textureId, texture);
            texture = null;
            state.status = Status.READY;
            state.textureId = textureId;
            state.width = width;
            state.height = height;
            state.sourceWidth = loadedImage.sourceWidth();
            state.sourceHeight = loadedImage.sourceHeight();
        } catch (Throwable ex) {
            if (texture != null) {
                texture.close();
            } else if (loadedImage != null) {
                loadedImage.image().close();
            }
            state.status = Status.FAILED;
            Panoramica.getLogger().warn("[PANORAMICA] Could not upload screenshot thumbnail {}.", entry.path(), ex);
        }
    }

    @NotNull
    private static Identifier nextTextureId() {
        return Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, TEXTURE_PATH + textureSequence++);
    }

    private void release(@NotNull ThumbnailState state) {
        if (state.textureId != null) {
            this.minecraft.getTextureManager().release(state.textureId);
            state.textureId = null;
        }
        state.status = Status.FAILED;
    }

    @Override
    public void close() {
        this.closed = true;
        this.pendingLoads.clear();
        for (ThumbnailState state : this.thumbnails.values()) {
            this.release(state);
        }
        this.thumbnails.clear();
    }

    private enum Status {
        LOADING,
        READY,
        FAILED
    }

    private static final class ThumbnailState {
        private Status status = Status.LOADING;
        @Nullable
        private Identifier textureId;
        private int width;
        private int height;
        private int sourceWidth;
        private int sourceHeight;
    }

    private record PendingLoad(
            @NotNull ScreenshotEntry entry,
            @NotNull ThumbnailState state,
            byte @Nullable [] imageBytes,
            @Nullable Throwable error
    ) {
    }

    public sealed interface Thumbnail permits Thumbnail.Ready, Thumbnail.Loading, Thumbnail.Failed {

        Thumbnail LOADING = new Loading();
        Thumbnail FAILED = new Failed();

        record Ready(@NotNull Identifier textureId, int width, int height, int sourceWidth, int sourceHeight) implements Thumbnail {
        }

        final class Loading implements Thumbnail {
            private Loading() {
            }
        }

        final class Failed implements Thumbnail {
            private Failed() {
            }
        }
    }

}
