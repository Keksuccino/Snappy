package de.keksuccino.snappy;

import de.keksuccino.snappy.platform.Services;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.jetbrains.annotations.NotNull;

@Mod(Snappy.MOD_ID)
public class SnappyNeoForge {
    
    public SnappyNeoForge(@NotNull IEventBus eventBus, @NotNull ModContainer modContainer) {

        // Snappy.init() got moved to MixinMinecraft

        if (Services.PLATFORM.isOnClient()) {

            eventBus.register(SnappyNeoForge.class);
            IConfigScreenFactory configScreenFactory = (container, parent) -> new OptionsScreen(parent);
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, configScreenFactory);

        }

    }

    @SubscribeEvent
    public static void onRegisterKeybinds(RegisterKeyMappingsEvent e) {

        for (KeyMapping keyMapping : KeyMappings.ALL) {
            e.register(keyMapping);
        }

    }

}
