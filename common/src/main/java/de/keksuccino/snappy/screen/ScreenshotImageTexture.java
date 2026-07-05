package de.keksuccino.snappy.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jetbrains.annotations.NotNull;

public class ScreenshotImageTexture extends DynamicTexture {

    public ScreenshotImageTexture(@NotNull String label, @NotNull NativeImage image) {
        super(() -> label, image);
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }

}
