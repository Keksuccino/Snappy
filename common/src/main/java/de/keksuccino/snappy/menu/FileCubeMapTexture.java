package de.keksuccino.snappy.menu;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileCubeMapTexture extends AbstractTexture {

    private static final int[] SIDE_LOAD_ORDER = new int[]{1, 3, 5, 4, 0, 2};

    private final Path folder;

    public FileCubeMapTexture(@NotNull Path folder) {
        this.folder = folder;
    }

    public void load() throws IOException {
        try (NativeImage image = this.loadStackedImage()) {
            this.upload(image);
        }
    }

    @NotNull
    private NativeImage loadStackedImage() throws IOException {
        NativeImage first = this.readSide(SIDE_LOAD_ORDER[0]);

        try {
            int width = first.getWidth();
            int height = first.getHeight();
            this.validateSideDimensions(width, height, SIDE_LOAD_ORDER[0], width, height);
            NativeImage stackedImage = new NativeImage(width, height * SIDE_LOAD_ORDER.length, false);
            first.copyRect(stackedImage, 0, 0, 0, 0, width, height, false, true);

            for (int i = 1; i < SIDE_LOAD_ORDER.length; i++) {
                int side = SIDE_LOAD_ORDER[i];
                try (NativeImage part = this.readSide(side)) {
                    this.validateSideDimensions(part.getWidth(), part.getHeight(), side, width, height);
                    part.copyRect(stackedImage, 0, 0, 0, i * height, width, height, false, true);
                }
            }

            return stackedImage;
        } finally {
            first.close();
        }
    }

    @NotNull
    private NativeImage readSide(int side) throws IOException {
        Path imagePath = this.folder.resolve("panorama_" + side + ".png");
        try (InputStream inputStream = Files.newInputStream(imagePath)) {
            return NativeImage.read(inputStream);
        }
    }

    private void validateSideDimensions(int width, int height, int side, int expectedWidth, int expectedHeight) throws IOException {
        if (width != height) {
            throw new IOException("Panorama side " + side + " is not square: " + width + "x" + height);
        }
        if (width != expectedWidth || height != expectedHeight) {
            throw new IOException("Panorama side " + side + " is " + width + "x" + height + ", expected " + expectedWidth + "x" + expectedHeight);
        }
    }

    private void upload(@NotNull NativeImage image) {
        GpuDevice device = RenderSystem.getDevice();
        int width = image.getWidth();
        int height = image.getHeight() / SIDE_LOAD_ORDER.length;
        this.close();
        this.texture = device.createTexture(() -> "Snappy cubemap " + this.folder, 21, GpuFormat.RGBA8_UNORM, width, height, SIDE_LOAD_ORDER.length, 1);
        this.textureView = device.createTextureView(this.texture);
        GpuBufferSlice stagingBuffer = device.createCommandEncoder().transientMemory().uploadStaging(image.getPixelBytes(), 1L, 16);

        for (int i = 0; i < SIDE_LOAD_ORDER.length; i++) {
            device.createCommandEncoder().copyBufferToTexture(stagingBuffer, 0, height * i, image.getWidth(), image.getHeight(), this.texture, 0, 0, width, height, 0, i);
        }
    }

}
