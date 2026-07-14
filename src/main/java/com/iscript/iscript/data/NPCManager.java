package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.npc.NPCData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class NPCManager {
    private static final Path getDir(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("iscript").resolve("npc");
    }

    private static void ensureDir(ServerLevel level) {
        try {
            Files.createDirectories(getDir(level));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to create npc directory", e);
        }
    }

    public static void save(ServerLevel level, String id, NPCData data) {
        if (id == null || id.isEmpty()) return;
        ensureDir(level);
        Path file = getDir(level).resolve(safeFileName(id) + ".dat");
        try {
            CompoundTag tag = new CompoundTag();
            data.save(tag);
            NbtIo.write(tag, file.toFile());
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to save npc {}", id, e);
        }
    }

    public static NPCData load(ServerLevel level, String id) {
        if (id == null || id.isEmpty()) return null;
        Path file = getDir(level).resolve(safeFileName(id) + ".dat");
        if (!Files.exists(file)) return null;
        try {
            CompoundTag tag = NbtIo.read(file.toFile());
            if (tag == null) return null;
            NPCData data = new NPCData();
            data.load(tag);
            return data;
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to load npc {}", id, e);
            return null;
        }
    }

    public static List<String> listIds(ServerLevel level) {
        List<String> result = new ArrayList<>();
        Path dir = getDir(level);
        if (!Files.exists(dir)) return result;
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(p -> p.toString().endsWith(".dat")).forEach(p -> {
                String name = p.getFileName().toString();
                result.add(name.substring(0, name.length() - 4));
            });
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to list npcs", e);
        }
        Collections.sort(result);
        return result;
    }

    public static void delete(ServerLevel level, String id) {
        if (id == null || id.isEmpty()) return;
        try {
            Files.deleteIfExists(getDir(level).resolve(safeFileName(id) + ".dat"));
            Files.deleteIfExists(getDir(level).resolve(safeFileName(id) + ".json"));
        } catch (IOException e) {
            IScriptMod.LOGGER.error("Failed to delete npc {}", id, e);
        }
    }

    public static boolean exists(ServerLevel level, String id) {
        if (id == null || id.isEmpty()) return false;
        return Files.exists(getDir(level).resolve(safeFileName(id) + ".dat"));
    }

    public static void rename(ServerLevel level, String oldId, String newId, NPCData data) {
        if (oldId == null || oldId.isEmpty() || newId == null || newId.isEmpty() || oldId.equals(newId)) return;
        delete(level, oldId);
        save(level, newId, data);
    }

    private static String safeFileName(String id) {
        return id.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private static final List<NPCData> CLIENT_CACHE = Collections.synchronizedList(new ArrayList<>());

    public static void updateClientCache(List<NPCData> list) {
        CLIENT_CACHE.clear();
        if (list != null) CLIENT_CACHE.addAll(list);
    }

    public static List<NPCData> getClientCache() {
        synchronized (CLIENT_CACHE) {
            return Collections.unmodifiableList(new ArrayList<>(CLIENT_CACHE));
        }
    }

    public static void clearClientCache() {
        CLIENT_CACHE.clear();
    }
}