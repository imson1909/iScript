package com.iscript.iscript.data;

import com.iscript.iscript.data.npc.NPCData;
import java.nio.file.Path;
import java.util.Map;

public class NPCManager {
    private static DataContainer<NPCData> container;

    public static void init(Path basePath) {
        container = new DataContainer<>(basePath.resolve("npcs"), NPCData::new);
        container.load();
    }

    public static NPCData get(String id) { return container != null ? container.get(id) : null; }
    public static Map<String, NPCData> all() { return container != null ? container.all() : Map.of(); }
    public static void put(NPCData npc) { if (container != null) container.put(npc); }
    public static void remove(String id) { if (container != null) container.remove(id); }
    public static boolean has(String id) { return container != null && container.has(id); }
    public static void save() { if (container != null) container.saveAndCleanup(); }


    private static final java.util.List<NPCData> clientCache = new java.util.ArrayList<>();

    public static java.util.List<NPCData> getClientCache() {
        return clientCache;
    }

    public static void updateClientCache(java.util.List<NPCData> list) {
        clientCache.clear();
        if (list != null) clientCache.addAll(list);
    }
}