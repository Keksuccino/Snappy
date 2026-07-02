package de.keksuccino.panoramica;

import de.keksuccino.panoramica.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.jetbrains.annotations.NotNull;

@Mod(Panoramica.MOD_ID)
public class PanoramicaNeoForge {
    
    public PanoramicaNeoForge(@NotNull IEventBus eventBus) {

        // Panoramica.init() got moved to MixinMinecraft

        if (Services.PLATFORM.isOnClient()) {

            eventBus.register(PanoramicaNeoForge.class);

        }

    }

    @SubscribeEvent
    public static void onRegisterKeybinds(RegisterKeyMappingsEvent e) {

        e.register(KeyMappings.KEY_TOGGLE_ZOOM);

    }

}