package de.keksuccino.snappy.mixin.mixins.common.client;

import de.keksuccino.snappy.photo.PhotoModeManager;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeProbe.class)
public class MixinEnvironmentAttributeProbe {

    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private <Value> void after_getValue_Snappy(EnvironmentAttribute<Value> attribute, float partialTicks, CallbackInfoReturnable<Value> info) {
        if ((Object) attribute == EnvironmentAttributes.SKY_COLOR && info.getReturnValue() instanceof Integer skyColor) {
            info.setReturnValue((Value) (Integer) PhotoModeManager.overrideSkyColor(skyColor));
        }
    }

}
