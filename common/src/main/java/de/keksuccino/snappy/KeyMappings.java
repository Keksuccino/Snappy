package de.keksuccino.snappy;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.List;

public class KeyMappings {

    public static final Identifier SNAPPY_KEYMAPPING_CATEGORY_ID = Identifier.fromNamespaceAndPath("snappy", "keybind.category.main");
    public static final KeyMapping.Category SNAPPY_KEYMAPPING_CATEGORY = KeyMapping.Category.register(SNAPPY_KEYMAPPING_CATEGORY_ID);

    public static final KeyMapping KEY_TAKE_PANORAMA = new KeyMapping("snappy.keybinds.keybind.take_panorama", InputConstants.KEY_F8, SNAPPY_KEYMAPPING_CATEGORY);
    public static final KeyMapping KEY_OPEN_PHOTO_MODE = new KeyMapping("snappy.keybinds.keybind.open_photo_mode", InputConstants.KEY_F9, SNAPPY_KEYMAPPING_CATEGORY);
    public static final KeyMapping KEY_PHOTO_MODE_TOGGLE_GRID = new KeyMapping("snappy.keybinds.keybind.photo_mode_toggle_grid", InputConstants.KEY_G, SNAPPY_KEYMAPPING_CATEGORY);
    public static final KeyMapping KEY_PHOTO_MODE_HIDE_UI = new KeyMapping("snappy.keybinds.keybind.photo_mode_hide_ui", InputConstants.KEY_H, SNAPPY_KEYMAPPING_CATEGORY);
    public static final KeyMapping KEY_PHOTO_MODE_SLOW_CAMERA = new KeyMapping("snappy.keybinds.keybind.photo_mode_slow_camera", InputConstants.KEY_LALT, SNAPPY_KEYMAPPING_CATEGORY);
    public static final List<KeyMapping> ALL = List.of(
            KEY_TAKE_PANORAMA,
            KEY_OPEN_PHOTO_MODE,
            KEY_PHOTO_MODE_TOGGLE_GRID,
            KEY_PHOTO_MODE_HIDE_UI,
            KEY_PHOTO_MODE_SLOW_CAMERA
    );

}
