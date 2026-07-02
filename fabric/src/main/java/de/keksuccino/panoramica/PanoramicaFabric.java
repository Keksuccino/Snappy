package de.keksuccino.panoramica;

import de.keksuccino.panoramica.platform.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class PanoramicaFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {

        Panoramica.init();

        if (Services.PLATFORM.isOnClient()) {

            KeyMappingHelper.registerKeyMapping(KeyMappings.KEY_TOGGLE_ZOOM);

        }

    }

}
