package org.trivait.minecode.util;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyBinding OPEN_CODE_MENU = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.autocode.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.misc")
    );
    public static final KeyBinding RUN_SCRIPT = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.autocode.run_script", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.misc")
    );

}
