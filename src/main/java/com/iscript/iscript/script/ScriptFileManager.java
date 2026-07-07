package com.iscript.iscript.script;

import com.iscript.iscript.IScriptMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ScriptFileManager {
    private static Path getWorldScriptDir(ServerLevel level) {
        String worldName = level.getServer().getWorldData().getLevelName();
        return FMLPaths.GAMEDIR.get().resolve("saves").resolve(worldName).resolve("iscript").resolve("scripts");
    }

    public static void ensureDir(ServerLevel level) {
        try {
            Files.createDirectories(getWorldScriptDir(level));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to create script directory", e);
        }
    }

    public static String load(ServerLevel level, String name) {
        if (name.isEmpty()) return "";
        Path file = getWorldScriptDir(level).resolve(name + ".js");
        try {
            return Files.readString(file);
        } catch (IOException e) {
            return "";
        }
    }

    public static void save(ServerLevel level, String name, String content) {
        if (name.isEmpty()) return;
        Path dir = getWorldScriptDir(level);
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(name + ".js");
            Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to save script {}", name, e);
        }
    }

    public static Map<String, String> list(ServerLevel level) {
        Map<String, String> result = new HashMap<>();
        Path dir = getWorldScriptDir(level);
        if (!Files.exists(dir)) return result;
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(p -> p.toString().endsWith(".js")).forEach(p -> {
                String name = p.getFileName().toString().replace(".js", "");
                try {
                    String content = Files.readString(p);
                    result.put(name, content.substring(0, Math.min(50, content.length())) + "...");
                } catch (IOException e) {
                    result.put(name, "[error]");
                }
            });
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to list scripts", e);
        }
        return result;
    }

    public static boolean exists(ServerLevel level, String name) {
        return Files.exists(getWorldScriptDir(level).resolve(name + ".js"));
    }

    public static void delete(ServerLevel level, String name) {
        try {
            Files.deleteIfExists(getWorldScriptDir(level).resolve(name + ".js"));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to delete script {}", name, e);
        }
    }
}