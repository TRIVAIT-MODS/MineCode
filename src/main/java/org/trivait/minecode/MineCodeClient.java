package org.trivait.minecode;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.trivait.minecode.engine.ScriptEngine;
import org.trivait.minecode.engine.ScriptRunner;
import org.trivait.minecode.engine.commands.BlockBreaker;
import org.trivait.minecode.engine.commands.ScriptDelayQueue;
import org.trivait.minecode.gui.CodeScreen;
import org.trivait.minecode.util.ConfigManager;
import org.trivait.minecode.util.KeyBindings;

public class MineCodeClient implements ClientModInitializer {
    private static boolean menuOpen = false;

    @Override
    public void onInitializeClient() {
        KeyBindings.OPEN_CODE_MENU.setPressed(false);

        ScriptDelayQueue.init();
        BlockBreaker.init();


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.OPEN_CODE_MENU.wasPressed()) {
                if (client.currentScreen instanceof CodeScreen) {
                    client.setScreen(null);
                    menuOpen = false;
                } else {
                    client.setScreen(new CodeScreen());
                    menuOpen = true;
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.RUN_SCRIPT.wasPressed()) {
                System.out.println("[AUTOCode] Нажата клавиша R для запуска скрипта");

                String code;

                if (client.currentScreen instanceof CodeScreen screen) {
                    code = screen.getScriptText();
                } else {
                    code = ConfigManager.loadScript();
                }

                if (code != null && !code.isBlank()) {
                    client.inGameHud.getChatHud().addMessage(Text.literal("▶ Скрипт запущен"));
                    System.out.println("[AUTOCode] Получен код:\n" + code);
                    ScriptEngine.runScript(code, client);
                } else {
                    client.inGameHud.getChatHud().addMessage(Text.literal("⚠ Скрипт пуст — нечего запускать."));
                    System.out.println("[AUTOCode] Пустой скрипт, запуск отменён");
                }
            }
        });
    }
}
