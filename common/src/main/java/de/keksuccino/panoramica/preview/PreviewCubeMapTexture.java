package de.keksuccino.panoramica.preview;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.NotNull;

public class PreviewCubeMapTexture extends AbstractTexture {

    private static final int[] SIDE_LOAD_ORDER = new int[]{1, 3, 5, 4, 0, 2};

    public PreviewCubeMapTexture(NativeImage @NotNull [] faces) {
        try {
            this.upload(faces);
        } finally {
            for (NativeImage face : faces) {
                if (face != null) {
                    face.close();
                }
            }
        }
    }

    private void upload(NativeImage @NotNull [] faces) {
        NativeImage first = faces[SIDE_LOAD_ORDER[0]];
        if (first == null) {
            throw new IllegalArgumentException("Missing panorama preview face " + SIDE_LOAD_ORDER[0]);
        }

        int width = first.getWidth();
        int height = first.getHeight();
        if (width != height) {
            throw new IllegalArgumentException("Panorama preview faces must be square, got " + width + "x" + height);
        }

        try (NativeImage stackedImage = new NativeImage(width, height * SIDE_LOAD_ORDER.length, false)) {
            for (int i = 0; i < SIDE_LOAD_ORDER.length; i++) {
                int faceIndex = SIDE_LOAD_ORDER[i];
                NativeImage face = faces[faceIndex];
                if (face == null) {
                    throw new IllegalArgumentException("Missing panorama preview face " + faceIndex);
                }
                if (face.getWidth() != width || face.getHeight() != height) {
                    throw new IllegalArgumentException("Panorama preview face " + faceIndex + " is " + face.getWidth() + "x" + face.getHeight() + ", expected " + width + "x" + height);
                }
                face.copyRect(stackedImage, 0, 0, 0, i * height, width, height, false, true);
            }

            GpuDevice device = RenderSystem.getDevice();
            this.close();
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            this.texture = device.createTexture(() -> "Panoramica panorama screenshot preview cubemap", 21, GpuFormat.RGBA8_UNORM, width, height, SIDE_LOAD_ORDER.length, 1);
            this.textureView = device.createTextureView(this.texture);
            GpuBufferSlice stagingBuffer = device.createCommandEncoder().transientMemory().uploadStaging(stackedImage.getPixelBytes(), 1L, 16);

            for (int i = 0; i < SIDE_LOAD_ORDER.length; i++) {
                device.createCommandEncoder().copyBufferToTexture(stagingBuffer, 0, height * i, stackedImage.getWidth(), stackedImage.getHeight(), this.texture, 0, 0, width, height, 0, i);
            }
        }
    }

}
