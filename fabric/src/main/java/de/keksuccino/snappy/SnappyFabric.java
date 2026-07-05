package de.keksuccino.snappy;

import de.keksuccino.snappy.platform.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class SnappyFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {

        Snappy.init();

        if (Services.PLATFORM.isOnClient()) {

            for (KeyMapping keyMapping : KeyMappings.ALL) {
                KeyMappingHelper.registerKeyMapping(keyMapping);
            }

        }

    }

}
