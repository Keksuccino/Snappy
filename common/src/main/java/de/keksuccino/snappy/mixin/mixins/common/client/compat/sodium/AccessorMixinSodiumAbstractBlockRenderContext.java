package de.keksuccino.snappy.mixin.mixins.common.client.compat.sodium;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public interface AccessorMixinSodiumAbstractBlockRenderContext {

    @Accessor("state")
    BlockState getState_Snappy();

    @Accessor("pos")
    BlockPos getPos_Snappy();

}
