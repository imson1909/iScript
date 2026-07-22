package com.iscript.iscript.script;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.Graph;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ScriptFileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path basePath;

    public static void setBasePath(Path path) {
        basePath = path;
    }

    public static Path getWorldDir(ServerLevel level, String category) {
        if (basePath != null) return basePath.resolve(category);
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

    private static void writeAtomic(Path temp, Path target, String content) throws IOException {
        Files.deleteIfExists(temp);
        try (FileOutputStream fos = new FileOutputStream(temp.toFile())) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            fos.getFD().sync();
        }
        Files.deleteIfExists(target);
        Files.move(temp, target);
        IScriptMod.LOGGER.info("Atomic write: {} -> {}", temp, target);
    }

    public static void saveScript(ServerLevel level, String id, String jsText, Graph meta) {
        if (id == null || id.isEmpty()) return;
        ensureDir(level, "scripts");
        Path dir = getWorldDir(level, "scripts");
        String safeId = safeFileName(id);
        try {
            writeAtomic(dir.resolve(safeId + ".js.tmp"), dir.resolve(safeId + ".js"), jsText != null ? jsText : "");

            JsonObject json = meta != null ? meta.toJson() : new JsonObject();
            json.addProperty("id", id);
            writeAtomic(dir.resolve(safeId + ".json.tmp"), dir.resolve(safeId + ".json"), GSON.toJson(json));
            IScriptMod.LOGGER.info("Script saved to disk: {}", id);
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

    public static Graph loadScriptJson(ServerLevel level, String id) {
        if (id == null || id.isEmpty()) return null;
        Path file = getWorldDir(level, "scripts").resolve(safeFileName(id) + ".json");
        if (!Files.exists(file)) return null;
        try {
            String text = Files.readString(file);
            JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
            Graph graph = new Graph(ScriptNodeType.class);
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
        Path dir = getWorldDir(level, category);
        try {
            writeAtomic(dir.resolve(safeFileName(id) + ".json.tmp"), dir.resolve(safeFileName(id) + ".json"), GSON.toJson(data));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to save {} {}", category, id, e);
        }
    }

    public static JsonObject loadJson(ServerLevel level, String category, String id) {
        if (id == null || id.isEmpty()) return null;
        Path file = getWorldDir(level, category).resolve(safeFileName(id) + ".json");
        if (!Files.exists(file)) return null;
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
            writeAtomic(dir.resolve(safeFileName(name) + ".js.tmp"), dir.resolve(safeFileName(name) + ".js"), content != null ? content : "");
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