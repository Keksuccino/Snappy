package de.keksuccino.panoramica.mixin.mixins.common.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface AccessorMixinLevelRenderer {

    @Nullable
    @Accessor("skyRenderer")
    SkyRenderer getSkyRenderer_Panoramica();

    @Accessor("skyRenderer")
    void setSkyRenderer_Panoramica(@Nullable SkyRenderer skyRenderer);

}
