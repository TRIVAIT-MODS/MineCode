package org.trivait.minecode.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Path SCRIPT_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("autocode_script.txt");

    public static void saveScript(String text) {
        try {
            Files.writeString(SCRIPT_PATH, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Не удалось сохранить скрипт: " + e.getMessage());
        }
    }

    public static String loadScript() {
        try {
            if (Files.exists(SCRIPT_PATH)) {
                return Files.readString(SCRIPT_PATH, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Не удалось загрузить скрипт: " + e.getMessage());
        }
        return "";
    }
    public static Path getScriptFile() {
        return SCRIPT_PATH;
    }

}
