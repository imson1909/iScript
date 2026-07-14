package com.iscript.iscript.event;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.EventSavedData;
import com.iscript.iscript.data.event.EventGraphData;
import com.iscript.iscript.data.script.ScriptNodeData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.script.ScriptEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventManager {

    public static void trigger(EventType type, Player player, ServerLevel level) {
        Map<String, EventGraphData> allGraphs = EventSavedData.get(level).getAllGraphs();
        for (EventGraphData graph : allGraphs.values()) {
            for (ScriptNodeData node : graph.getNodes().values()) {
                if (node.getType() == ScriptNodeType.START || node.getType() == ScriptNodeType.TRIGGER) {
                    String graphEventType = node.getParam("eventType");
                    if (type.name().equals(graphEventType)) {
                        executeNode(node, graph, player, level, new ArrayList<>());
                    }
                }
            }
        }
    }

    private static void executeNode(ScriptNodeData node, EventGraphData graph, Player player, ServerLevel level, List<String> visited) {
        if (node == null || visited.contains(node.getId())) return;
        visited.add(node.getId());

        switch (node.getType()) {
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
            case DELAY -> {
            }
            case STOP -> {
                return;
            }
            default -> {
            }
        }

        for (var conn : node.getConnections()) {
            ScriptNodeData next = graph.getNode(conn.getTargetNodeId());
            if (next != null) {
                executeNode(next, graph, player, level, visited);
            }
        }
    }

    public static void addGraph(ServerLevel level, EventGraphData graph) {
        if (graph != null && !graph.getId().isEmpty()) {
            EventSavedData.get(level).setGraph(graph.getId(), graph);
        }
    }

    public static void removeGraph(ServerLevel level, String id) {
        EventSavedData.get(level).removeGraph(id);
    }

    public static EventGraphData getGraph(ServerLevel level, String id) {
        return EventSavedData.get(level).getGraph(id);
    }

    public static Map<String, EventGraphData> getAllGraphs(ServerLevel level) {
        return EventSavedData.get(level).getAllGraphs();
    }
}