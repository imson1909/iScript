package com.iscript.iscript.script;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.script.ScriptGraphData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ScriptFileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path getWorldDir(ServerLevel level, String category) {
        String worldName = level.getServer().getWorldData().getLevelName();
        return FMLPaths.GAMEDIR.get().resolve("saves").resolve(worldName).resolve("iscript").resolve(category);
    }

    public static void ensureDir(ServerLevel level, String category) {
        try {
            Files.createDirectories(getWorldDir(level, category));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to create directory for {}", category, e);
        }
    }

    public static void saveScript(ServerLevel level, String id, String jsText, ScriptGraphData meta) {
        if (id == null || id.isEmpty()) return;
        ensureDir(level, "scripts");
        Path dir = getWorldDir(level, "scripts");
        try {
            Path jsFile = dir.resolve(safeFileName(id) + ".js");
            Files.writeString(jsFile, jsText != null ? jsText : "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            Path jsonFile = dir.resolve(safeFileName(id) + ".json");
            JsonObject json = meta != null ? meta.toJson() : new JsonObject();
            json.addProperty("id", id);
            Files.writeString(jsonFile, GSON.toJson(json), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to save script {}", id, e);
        }
    }

    public static String loadScriptJs(ServerLevel level, String id) {
        if (id == null || id.isEmpty()) return "";
        Path file = getWorldDir(level, "scripts").resolve(safeFileName(id) + ".js");
        try {
            return Files.readString(file);
        } catch (IOException e) {
            return "";
        }
    }

    public static ScriptGraphData loadScriptJson(ServerLevel level, String id) {
        if (id == null || id.isEmpty()) return null;
        Path file = getWorldDir(level, "scripts").resolve(safeFileName(id) + ".json");
        try {
            String text = Files.readString(file);
            JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
            ScriptGraphData graph = new ScriptGraphData();
            graph.fromJson(obj);
            return graph;
        } catch (Exception e) {
            IScriptMod.LOGGER.error("Failed to load script json {}", id, e);
            return null;
        }
    }

    public static List<String> listScriptIds(ServerLevel level) {
        List<String> result = new ArrayList<>();
        Path dir = getWorldDir(level, "scripts");
        if (!Files.exists(dir)) return result;
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(p -> p.toString().endsWith(".js")).forEach(p -> {
                String name = p.getFileName().toString();
                result.add(name.substring(0, name.length() - 3));
            });
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to list scripts", e);
        }
        Collections.sort(result);
        return result;
    }

    public static boolean scriptExists(ServerLevel level, String id) {
        return Files.exists(getWorldDir(level, "scripts").resolve(safeFileName(id) + ".js"));
    }

    public static void deleteScript(ServerLevel level, String id) {
        if (id == null || id.isEmpty()) return;
        Path dir = getWorldDir(level, "scripts");
        try {
            Files.deleteIfExists(dir.resolve(safeFileName(id) + ".js"));
            Files.deleteIfExists(dir.resolve(safeFileName(id) + ".json"));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to delete script {}", id, e);
        }
    }

    public static void saveJson(ServerLevel level, String category, String id, JsonObject data) {
        if (id == null || id.isEmpty()) return;
        ensureDir(level, category);
        Path file = getWorldDir(level, category).resolve(safeFileName(id) + ".json");
        try {
            Files.writeString(file, GSON.toJson(data), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to save {} {}", category, id, e);
        }
    }

    public static JsonObject loadJson(ServerLevel level, String category, String id) {
        if (id == null || id.isEmpty()) return null;
        Path file = getWorldDir(level, category).resolve(safeFileName(id) + ".json");
        try {
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> listJsonIds(ServerLevel level, String category) {
        List<String> result = new ArrayList<>();
        Path dir = getWorldDir(level, category);
        if (!Files.exists(dir)) return result;
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                String name = p.getFileName().toString();
                result.add(name.substring(0, name.length() - 5));
            });
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to list {}", category, e);
        }
        Collections.sort(result);
        return result;
    }

    public static void deleteJson(ServerLevel level, String category, String id) {
        if (id == null || id.isEmpty()) return;
        try {
            Files.deleteIfExists(getWorldDir(level, category).resolve(safeFileName(id) + ".json"));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to delete {} {}", category, id, e);
        }
    }

    public static void save(ServerLevel level, String name, String content) {
        if (name == null || name.isEmpty()) return;
        ensureDir(level, "scripts");
        Path dir = getWorldDir(level, "scripts");
        try {
            Path file = dir.resolve(safeFileName(name) + ".js");
            Files.writeString(file, content != null ? content : "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to save script {}", name, e);
        }
    }

    public static String load(ServerLevel level, String name) {
        if (name == null || name.isEmpty()) return "";
        Path file = getWorldDir(level, "scripts").resolve(safeFileName(name) + ".js");
        try {
            return Files.readString(file);
        } catch (IOException e) {
            return "";
        }
    }

    public static boolean exists(ServerLevel level, String name) {
        return Files.exists(getWorldDir(level, "scripts").resolve(safeFileName(name) + ".js"));
    }

    public static void delete(ServerLevel level, String name) {
        if (name == null || name.isEmpty()) return;
        Path dir = getWorldDir(level, "scripts");
        try {
            Files.deleteIfExists(dir.resolve(safeFileName(name) + ".js"));
            Files.deleteIfExists(dir.resolve(safeFileName(name) + ".json"));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to delete script {}", name, e);
        }
    }

    private static String safeFileName(String id) {
        return id.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}