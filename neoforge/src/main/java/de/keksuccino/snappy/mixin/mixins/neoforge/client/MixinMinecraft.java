package de.keksuccino.snappy.mixin.mixins.neoforge.client;

import de.keksuccino.snappy.Snappy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    /** @reason NeoForge reaches Minecraft construction after loader services are ready, so this is the earliest shared Snappy init point for this loader. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void after_construct_Snappy(GameConfig gameConfig, CallbackInfo info) {
        Snappy.init();
    }

}
