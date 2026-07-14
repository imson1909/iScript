package com.iscript.iscript.data.state;

import java.util.*;

public class StateMachineManager {
    private static final Map<String, String> clientCache = new LinkedHashMap<>();
    private static final Map<String, ClientMachineData> clientMachineData = new HashMap<>();

    public static Map<String, String> getClientCache() {
        return clientCache;
    }

    public static void setClientCache(Map<String, String> cache) {
        clientCache.clear();
        clientCache.putAll(cache);
    }

    public static void putClientCache(String id, String name) {
        clientCache.put(id, name);
    }

    public static void removeClientCache(String id) {
        clientCache.remove(id);
    }

    public static ClientMachineData getClientMachineData(String id) {
        return clientMachineData.get(id);
    }

    public static void setClientMachineData(String id, ClientMachineData data) {
        clientMachineData.put(id, data);
    }

    public static void clearClientMachineData(String id) {
        clientMachineData.remove(id);
    }
}