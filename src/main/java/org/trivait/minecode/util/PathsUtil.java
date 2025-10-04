package org.trivait.minecode.util;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class PathsUtil {
    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("minecode");
    }

    public static Path getScriptsDir() {
        Path p = getConfigDir().resolve("scripts");
        try {
            Files.createDirectories(p);
        } catch (Exception ignored){}
        return p;
    }

    public static Path resolveScript(String name) {
        return getScriptsDir().resolve(name + ".mc");
    }
}
