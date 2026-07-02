package de.keksuccino.panoramica;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyMappings {

    public static final Identifier PANORAMICA_KEYMAPPING_CATEGORY_ID = Identifier.fromNamespaceAndPath("panoramica", "keybind.category.main");
    public static final KeyMapping.Category PANORAMICA_KEYMAPPING_CATEGORY = KeyMapping.Category.register(PANORAMICA_KEYMAPPING_CATEGORY_ID);

    public static final KeyMapping KEY_TAKE_PANORAMA = new KeyMapping("panoramica.keybinds.keybind.take_panorama", InputConstants.KEY_F8, PANORAMICA_KEYMAPPING_CATEGORY);

}
