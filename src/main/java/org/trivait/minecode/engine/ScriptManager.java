// src/main/java/org/trivait/minecode/engine/ScriptManager.java
package org.trivait.minecode.engine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.trivait.minecode.engine.functions.FunctionRegistry;
import org.trivait.minecode.util.PathsUtil;
import org.trivait.minecode.util.ToastUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScriptManager {
    private static Path selectedScript;
    private static volatile Runner currentRunner;

    public static void init() {
        try {
            Files.createDirectories(PathsUtil.getScriptsDir());
        } catch (IOException ignored){}
    }

    public static void select(Path p) {
        selectedScript = p;
    }

    public static void runSelected(MinecraftClient client) {
        if (selectedScript == null) {
            ToastUtil.infoText(Text.literal("Скрипт не выбран"));
            return;
        }
        if (currentRunner != null) {
            ToastUtil.infoText(Text.literal("Скрипт уже выполняется"));
            return;
        }
        try {
            String code = Files.readString(selectedScript);
            List<Instruction> program = Parser.parse(code);
            currentRunner = new Runner(program);
            ToastUtil.info("minecode.toast.running");
        } catch (Exception e) {
            ToastUtil.infoText(Text.literal("Ошибка: " + e.getMessage()));
        }
    }

    public static void stopRunning() {
        if (currentRunner != null) {
            currentRunner.stop();
            currentRunner = null;
            ToastUtil.info("minecode.toast.stopped");
        }
    }

    public static void tick(MinecraftClient client) {
        if (currentRunner != null) {
            boolean alive = currentRunner.tick(client);
            if (!alive) currentRunner = null;
        }
    }

    public static List<String> hintFor(String prefix) {
        return FunctionRegistry.hintNames(prefix);
    }
}
