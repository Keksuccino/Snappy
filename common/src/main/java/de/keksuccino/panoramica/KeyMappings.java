package de.keksuccino.panoramica;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyMappings {

    public static final Identifier PANORAMICA_KEYMAPPING_CATEGORY_ID = Identifier.fromNamespaceAndPath("panoramica", "keybind.category.main");
    public static final KeyMapping.Category PANORAMICA_KEYMAPPING_CATEGORY = KeyMapping.Category.register(PANORAMICA_KEYMAPPING_CATEGORY_ID);

    public static final KeyMapping KEY_TOGGLE_ZOOM = new KeyMapping("panoramica.keybinds.keybind.zoom", InputConstants.KEY_Z, PANORAMICA_KEYMAPPING_CATEGORY);

}
