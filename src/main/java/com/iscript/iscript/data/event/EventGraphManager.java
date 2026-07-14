package com.iscript.iscript.data.event;

import com.iscript.iscript.data.EventSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class EventGraphManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventGraphManager.class);
    private static final Map<ServerLevel, Map<String, EventGraphData>> SERVER_DATA = new WeakHashMap<>();
    private static final Map<String, EventGraphData> CLIENT_CACHE = new HashMap<>();

    private static ServerLevel getOverworld(ServerLevel level) {
        if (level == null) return null;
        MinecraftServer server = level.getServer();
        if (server == null) return null;
        return server.overworld();
    }

    public static EventGraphData get(ServerLevel level, String id) {
        if (id == null || id.isEmpty()) return null;
        ServerLevel ow = getOverworld(level);
        if (ow != null) {
            EventGraphData fromCache = SERVER_DATA.computeIfAbsent(ow, k -> new HashMap<>()).get(id);
            if (fromCache != null) {
                LOGGER.info("[EGM] get('{}') -> found in cache", id);
                return fromCache;
            }
        }
        EventSavedData savedData = EventSavedData.get(level);
        if (savedData != null) {
            EventGraphData fromDisk = savedData.getGraph(id);
            if (fromDisk != null && ow != null) {
                SERVER_DATA.computeIfAbsent(ow, k -> new HashMap<>()).put(id, fromDisk);
                LOGGER.info("[EGM] get('{}') -> found on disk, cached", id);
            } else {
                LOGGER.info("[EGM] get('{}') -> not found on disk", id);
            }
            return fromDisk;
        }
        LOGGER.info("[EGM] get('{}') -> EventSavedData is null", id);
        return null;
    }

    public static void add(ServerLevel level, EventGraphData graph) {
        if (graph == null || graph.getId().isEmpty()) {
            LOGGER.warn("[EGM] add() called with null or empty graph");
            return;
        }
        ServerLevel ow = getOverworld(level);
        if (ow != null) {
            SERVER_DATA.computeIfAbsent(ow, k -> new HashMap<>()).put(graph.getId(), graph);
        }
        EventSavedData savedData = EventSavedData.get(level);
        if (savedData != null) {
            savedData.setGraph(graph.getId(), graph);
            LOGGER.info("[EGM] add('{}') -> saved to Overworld", graph.getId());
        } else {
            LOGGER.warn("[EGM] add('{}') -> EventSavedData is null", graph.getId());
        }
    }

    public static void remove(ServerLevel level, String id) {
        if (id == null) {
            LOGGER.warn("[EGM] remove() called with null id");
            return;
        }
        ServerLevel ow = getOverworld(level);
        if (ow != null) {
            Map<String, EventGraphData> map = SERVER_DATA.get(ow);
            if (map != null) map.remove(id);
        }
        EventSavedData savedData = EventSavedData.get(level);
        if (savedData != null) {
            savedData.removeGraph(id);
            LOGGER.info("[EGM] remove('{}') -> removed from Overworld", id);
        } else {
            LOGGER.warn("[EGM] remove('{}') -> EventSavedData is null", id);
        }
    }

    public static Map<String, EventGraphData> getAll(ServerLevel level) {
        Map<String, EventGraphData> result = new HashMap<>();
        ServerLevel ow = getOverworld(level);
        if (ow != null) {
            Map<String, EventGraphData> cached = SERVER_DATA.get(ow);
            if (cached != null) result.putAll(cached);
        }
        EventSavedData savedData = EventSavedData.get(level);
        if (savedData != null) {
            result.putAll(savedData.getAllGraphs());
        }
        LOGGER.info("[EGM] getAll() -> {} graphs (cache: {}, saved: {})",
                result.size(),
                ow != null && SERVER_DATA.get(ow) != null ? SERVER_DATA.get(ow).size() : 0,
                savedData != null ? savedData.getAllGraphs().size() : 0);
        return Collections.unmodifiableMap(result);
    }

    public static void updateClientCache(Map<String, EventGraphData> graphs) {
        CLIENT_CACHE.clear();
        if (graphs != null) CLIENT_CACHE.putAll(graphs);
        LOGGER.info("[EGM] updateClientCache() -> {} graphs", CLIENT_CACHE.size());
    }

    public static Map<String, EventGraphData> getClientCache() {
        return Collections.unmodifiableMap(CLIENT_CACHE);
    }

    public static void putClientCache(String id, EventGraphData graph) {
        if (id != null && !id.isEmpty() && graph != null) CLIENT_CACHE.put(id, graph);
    }

    public static void removeClientCache(String id) {
        if (id != null) {
            CLIENT_CACHE.remove(id);
            LOGGER.info("[EGM] removeClientCache('{}')", id);
        }
    }
}