package com.iscript.iscript.script;

import com.iscript.iscript.data.Graph;
import com.iscript.iscript.data.script.ScriptNodeType;
import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScriptGraphManager {
    private static final Map<String, Graph> CLIENT_CACHE = new HashMap<>();
    private static final Map<String, String> CLIENT_JS_CACHE = new HashMap<>();

    public static Graph get(ServerLevel level, String id) {
        if (level == null || id == null) return null;
        Graph graph = ScriptFileManager.loadScriptJson(level, id);
        if (graph == null) {
            graph = new Graph(ScriptNodeType.class);
            graph.setId(id);
            graph.setName(id);
        }
        return graph;
    }

    public static void add(ServerLevel level, Graph graph, String jsText) {
        if (level == null || graph == null || graph.getId() == null || graph.getId().isEmpty()) return;
        ScriptFileManager.saveScript(level, graph.getId(), jsText, graph);
        com.iscript.iscript.data.ModData md = com.iscript.iscript.data.ModData.get();
        if (md != null) md.scriptTexts.put(graph.getId(), jsText != null ? jsText : "");
    }

    public static void add(ServerLevel level, Graph graph) {
        if (level == null || graph == null || graph.getId() == null || graph.getId().isEmpty()) return;
        ScriptFileManager.saveScript(level, graph.getId(), "", graph);
    }

    public static void remove(ServerLevel level, String id) {
        if (level == null || id == null) return;
        ScriptFileManager.deleteScript(level, id);
    }

    public static Map<String, Graph> getAll(ServerLevel level) {
        if (level == null) return Collections.emptyMap();
        Map<String, Graph> result = new HashMap<>();
        for (String id : ScriptFileManager.listScriptIds(level)) {
            Graph graph = ScriptFileManager.loadScriptJson(level, id);
            if (graph == null) {
                graph = new Graph(ScriptNodeType.class);
                graph.setId(id);
                graph.setName(id);
            }
            result.put(id, graph);
        }
        return result;
    }

    public static void updateClientCache(Map<String, Graph> graphs) {
        CLIENT_CACHE.clear();
        if (graphs != null) CLIENT_CACHE.putAll(graphs);
    }

    public static void putClientCache(String id, Graph graph) {
        if (id != null && graph != null) CLIENT_CACHE.put(id, graph);
    }

    public static Map<String, Graph> getClientCache() {
        return Collections.unmodifiableMap(CLIENT_CACHE);
    }

    public static void clearClientCache() {
        CLIENT_CACHE.clear();
        CLIENT_JS_CACHE.clear();
    }

    public static void updateClientJsCache(String id, String jsText) {
        if (id != null) CLIENT_JS_CACHE.put(id, jsText != null ? jsText : "");
    }

    public static String getClientJsCache(String id) {
        return CLIENT_JS_CACHE.getOrDefault(id, "");
    }

    public static boolean hasClientJsCache(String id) {
        return CLIENT_JS_CACHE.containsKey(id);
    }
}