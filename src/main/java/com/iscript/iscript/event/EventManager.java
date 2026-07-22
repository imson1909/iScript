package com.iscript.iscript.event;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.data.Graph;
import com.iscript.iscript.data.Node;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.script.ScriptEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventManager {

    public static void trigger(EventType type, Player player, ServerLevel level) {
        Map<String, Graph> allGraphs = DataAccess.eventGraphs();
        for (Graph graph : allGraphs.values()) {
            for (Node node : graph.getNodes().values()) {
                if ("START".equals(node.getType()) || "TRIGGER".equals(node.getType())) {
                    String graphEventType = node.getParam("eventType");
                    if (type.name().equals(graphEventType)) {
                        executeNode(node, graph, player, level, new ArrayList<>());
                    }
                }
            }
        }
    }

    private static void executeNode(Node node, Graph graph, Player player, ServerLevel level, List<String> visited) {
        if (node == null || visited.contains(node.getId())) return;
        visited.add(node.getId());

        ScriptNodeType type;
        try { type = ScriptNodeType.valueOf(node.getType()); } catch (Exception e) { type = ScriptNodeType.SCRIPT_JS; }

        switch (type) {
            case SCRIPT_JS -> {
                String script = node.getParam("script");
                if (!script.isEmpty() && ScriptEngine.getInstance().isAvailable()) {
                    try {
                        ScriptEngine.getInstance().execute(script, player, level);
                    } catch (Exception e) {
                        IScriptMod.LOGGER.error("Event script error: {}", e.getMessage());
                    }
                }
            }
            case DELAY -> {}
            case STOP -> { return; }
            default -> {}
        }

        for (Node.Connection conn : node.getConnections()) {
            Node next = graph.getNode(conn.getTarget());
            if (next != null) {
                executeNode(next, graph, player, level, visited);
            }
        }
    }

    public static void addGraph(ServerLevel level, Graph graph) {
        if (graph != null && !graph.getId().isEmpty()) {
            DataAccess.putEventGraph(graph);
        }
    }

    public static void removeGraph(ServerLevel level, String id) {
        DataAccess.removeEventGraph(id);
    }

    public static Graph getGraph(ServerLevel level, String id) {
        return DataAccess.eventGraph(id);
    }

    public static Map<String, Graph> getAllGraphs(ServerLevel level) {
        return DataAccess.eventGraphs();
    }
}