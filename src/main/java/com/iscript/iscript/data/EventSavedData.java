package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.event.EventGraphData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class EventSavedData extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSavedData.class);
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_events";
    private final Map<String, EventGraphData> graphs = new HashMap<>();

    public EventSavedData() {}

    public static EventSavedData load(CompoundTag tag) {
        EventSavedData data = new EventSavedData();
        if (tag == null) {
            LOGGER.info("[ESD] load() -> tag is null, returning empty");
            return data;
        }
        int loaded = 0;
        for (String key : tag.getAllKeys()) {
            if (key.startsWith("Graph_")) {
                try {
                    EventGraphData graph = new EventGraphData();
                    graph.load(tag.getCompound(key));
                    if (!graph.getId().isEmpty()) {
                        data.graphs.put(graph.getId(), graph);
                        loaded++;
                    }
                } catch (Exception e) {
                    LOGGER.error("[ESD] load() -> failed to load graph from key '{}': {}", key, e.getMessage());
                }
            }
        }
        LOGGER.info("[ESD] load() -> loaded {} graphs from NBT", loaded);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        int saved = 0;
        for (Map.Entry<String, EventGraphData> entry : graphs.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            try {
                CompoundTag t = new CompoundTag();
                entry.getValue().save(t);
                tag.put("Graph_" + entry.getKey(), t);
                saved++;
            } catch (Exception e) {
                LOGGER.error("[ESD] save() -> failed to save graph '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        LOGGER.info("[ESD] save() -> saved {} graphs to NBT", saved);
        return tag;
    }

    public EventGraphData getGraph(String id) {
        return graphs.get(id);
    }

    public void setGraph(String id, EventGraphData graph) {
        if (id != null && !id.isEmpty() && graph != null) {
            graphs.put(id, graph);
            setDirty();
            LOGGER.info("[ESD] setGraph('{}') -> marked dirty, total graphs: {}", id, graphs.size());
        }
    }

    public void removeGraph(String id) {
        if (graphs.remove(id) != null) {
            setDirty();
            LOGGER.info("[ESD] removeGraph('{}') -> removed, total graphs: {}", id, graphs.size());
        } else {
            LOGGER.warn("[ESD] removeGraph('{}') -> graph not found", id);
        }
    }

    public Map<String, EventGraphData> getAllGraphs() {
        return new HashMap<>(graphs);
    }

    public static EventSavedData get(ServerLevel level) {
        if (level == null) {
            LOGGER.warn("[ESD] get() -> level is null");
            return null;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            LOGGER.warn("[ESD] get() -> server is null");
            return null;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            LOGGER.warn("[ESD] get() -> overworld is null");
            return null;
        }
        EventSavedData data = overworld.getDataStorage().computeIfAbsent(EventSavedData::load, EventSavedData::new, DATA_NAME);
        LOGGER.info("[ESD] get() -> retrieved from Overworld, graphs: {}", data != null ? data.graphs.size() : 0);
        return data;
    }
}