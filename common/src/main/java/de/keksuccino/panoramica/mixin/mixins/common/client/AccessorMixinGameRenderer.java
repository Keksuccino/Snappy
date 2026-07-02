package de.keksuccino.panoramica.mixin.mixins.common.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface AccessorMixinGameRenderer {

    @Accessor("mainRenderTarget")
    RenderTarget getMainRenderTarget_Panoramica();

    @Mutable
    @Accessor("mainRenderTarget")
    void setMainRenderTarget_Panoramica(RenderTarget mainRenderTarget);

    @Accessor("renderBlockOutline")
    boolean getRenderBlockOutline_Panoramica();

}
