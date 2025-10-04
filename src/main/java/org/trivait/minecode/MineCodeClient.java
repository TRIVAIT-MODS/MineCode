package org.trivait.minecode;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.trivait.minecode.engine.ScriptManager;
import org.trivait.minecode.engine.functions.*;
import org.trivait.minecode.ui.ManagerScreen;

public class MineCodeClient implements ClientModInitializer {
    public static final String MOD_ID = "minecode";

    private static KeyBinding openMenuKey;
    private static KeyBinding runSelectedKey;
    private static KeyBinding stopScriptKey;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "minecode.menu.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "MineCode"
        ));
        runSelectedKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "minecode.menu.run",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "MineCode"
        ));
        stopScriptKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "minecode.menu.stop",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "MineCode"
        ));

        FunctionRegistry.register(new SayFunction());
        FunctionRegistry.register(new WaitFunction());
        FunctionRegistry.register(new LookFunction());
        FunctionRegistry.register(new WalkFunction());
        FunctionRegistry.register(new IntFunction());
        FunctionRegistry.register(new BooleanFunction());
        FunctionRegistry.register(new StringFunction());
        FunctionRegistry.register(new IfFunction());
        FunctionRegistry.register(new WhileFunction());
        FunctionRegistry.register(new DoWhileFunction());
        FunctionRegistry.register(new ForFunction());
        FunctionRegistry.register(new SwitchFunction());

        ScriptManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                client.setScreen(new ManagerScreen());
            }
            while (runSelectedKey.wasPressed()) {
                ScriptManager.runSelected(client);
            }
            while (stopScriptKey.wasPressed()) {
                ScriptManager.stopRunning();
            }
            ScriptManager.tick(client);
        });
    }
}
