package org.trivait.minecode.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ToastUtil {
    public static void info(String key) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.translatable(key));
    }

    public static void infoText(Text text) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
    }
}
