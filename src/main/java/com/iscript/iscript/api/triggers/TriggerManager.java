package com.iscript.iscript.api.triggers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.script.ScriptFileManager;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class TriggerManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, TriggerData> triggers = new HashMap<>();

    public static void loadAll(ServerLevel level) {
        triggers.clear();
        Path dir = ScriptFileManager.getWorldDir(level, "triggers");
        if (!Files.exists(dir)) return;
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    String text = Files.readString(p);
                    JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
                    TriggerData data = new TriggerData();
                    data.fromJson(obj);
                    if (data.getId() != null && !data.getId().isEmpty()) {
                        triggers.put(data.getId(), data);
                    }
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("Failed to load trigger {}", p.getFileName(), e);
                }
            });
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to list triggers", e);
        }
        IScriptMod.LOGGER.info("Loaded {} triggers", triggers.size());
    }

    public static void save(ServerLevel level, TriggerData data) {
        if (data.getId() == null || data.getId().isEmpty()) return;
        ScriptFileManager.ensureDir(level, "triggers");
        Path dir = ScriptFileManager.getWorldDir(level, "triggers");
        try {
            String json = GSON.toJson(data.toJson());
            Path file = dir.resolve(safeFileName(data.getId()) + ".json");
            Files.writeString(file, json);
            triggers.put(data.getId(), data);
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to save trigger {}", data.getId(), e);
        }
    }

    public static void delete(ServerLevel level, String id) {
        triggers.remove(id);
        Path dir = ScriptFileManager.getWorldDir(level, "triggers");
        try {
            Files.deleteIfExists(dir.resolve(safeFileName(id) + ".json"));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to delete trigger {}", id, e);
        }
    }

    public static TriggerData get(String id) { return triggers.get(id); }
    public static Collection<TriggerData> getAll() { return Collections.unmodifiableCollection(triggers.values()); }
    public static List<TriggerData> getByType(TriggerType type) {
        List<TriggerData> result = new ArrayList<>();
        for (TriggerData t : triggers.values()) {
            if (t.isEnabled() && t.getType() == type) result.add(t);
        }
        return result;
    }

    private static String safeFileName(String id) {
        return id.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}