package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.event.EventGraphData;
import com.iscript.iscript.data.event.EventGraphManager;
import com.iscript.iscript.data.script.ScriptNodeConnection;
import com.iscript.iscript.data.script.ScriptNodeData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.event.EventManager;
import com.iscript.iscript.event.EventType;
import com.iscript.iscript.script.ScriptEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public class RunEventGraphPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunEventGraphPacket.class);
    private final String graphId;

    public RunEventGraphPacket(String graphId) {
        this.graphId = graphId;
    }

    public RunEventGraphPacket(FriendlyByteBuf buf) {
        this.graphId = buf.readUtf(32767);
    }

    public static RunEventGraphPacket decode(FriendlyByteBuf buf) {
        return new RunEventGraphPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(graphId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;
            ServerLevel overworld = server.overworld();
            if (overworld == null) return;
            if (graphId == null || graphId.isEmpty()) return;
            EventGraphData graph = EventGraphManager.get(overworld, graphId);
            if (graph == null) {
                graph = com.iscript.iscript.data.EventSavedData.get(overworld).getGraph(graphId);
            }
            if (graph == null) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Graph '" + graphId + "' not found"));
                return;
            }
            new GraphExecutor(graph, player, overworld).execute();
        });
        ctx.get().setPacketHandled(true);
    }

    private static class GraphExecutor {
        private final EventGraphData graph;
        private final ServerPlayer player;
        private final ServerLevel level;
        private final Set<String> visited;
        private final Random random;

        GraphExecutor(EventGraphData graph, ServerPlayer player, ServerLevel level) {
            this.graph = graph;
            this.player = player;
            this.level = level;
            this.visited = new HashSet<>();
            this.random = new Random();
        }

        void execute() {
            String startId = graph.getStartNodeId();
            if (startId == null || startId.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] No start node set"));
                return;
            }
            ScriptNodeData startNode = graph.getNode(startId);
            if (startNode == null) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Start node not found"));
                return;
            }
            executeNode(startNode, 0);
        }

        private void executeNode(ScriptNodeData node, int depth) {
            if (node == null) return;
            if (depth > 1000) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Max depth exceeded at node '" + node.getId() + "'"));
                return;
            }
            if (visited.contains(node.getId())) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Cycle detected at node '" + node.getId() + "'"));
                return;
            }
            visited.add(node.getId());

            switch (node.getType()) {
                case START, TRIGGER -> executeStartTrigger(node);
                case SCRIPT_JS -> executeScript(node);
                case IF -> executeIf(node, depth);
                case DELAY -> executeDelay(node, depth);
                case RANDOM -> executeRandom(node, depth);
                case LOOP -> executeLoop(node, depth);
                case STOP -> {
                    player.sendSystemMessage(Component.literal("§7[EventGraph] STOP reached"));
                    return;
                }
                default -> player.sendSystemMessage(Component.literal("§c[EventGraph] Unknown node type: " + node.getType()));
            }

            if (node.getType() != ScriptNodeType.IF && node.getType() != ScriptNodeType.RANDOM &&
                    node.getType() != ScriptNodeType.LOOP && node.getType() != ScriptNodeType.STOP) {
                followConnections(node, 0, depth);
            }
        }

        private void executeStartTrigger(ScriptNodeData node) {
            String eventTypeStr = node.getParam("eventType");
            if (eventTypeStr == null || eventTypeStr.isEmpty()) return;
            try {
                EventType type = EventType.valueOf(eventTypeStr);
                EventManager.trigger(type, player, level);
            } catch (IllegalArgumentException e) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Invalid eventType: " + eventTypeStr));
            }
        }

        private void executeScript(ScriptNodeData node) {
            String script = node.getParam("script");
            if (script == null || script.isEmpty()) return;
            ScriptEngine engine = ScriptEngine.getInstance();
            if (!engine.isAvailable()) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] ScriptEngine not available"));
                return;
            }
            try {
                engine.execute(script, player, level);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                player.sendSystemMessage(Component.literal("§c[Script Error] " + msg));
            }
        }

        private void executeIf(ScriptNodeData node, int depth) {
            String condition = node.getParam("condition");
            boolean result = false;
            if (condition != null && !condition.isEmpty()) {
                result = evaluateCondition(condition);
            }
            followConnections(node, result ? 0 : 1, depth);
        }

        private void executeDelay(ScriptNodeData node, int depth) {
            followConnections(node, 0, depth);
        }

        private void executeRandom(ScriptNodeData node, int depth) {
            String branchesStr = node.getParam("branches");
            int branches = 2;
            try {
                if (branchesStr != null) branches = Math.max(2, Integer.parseInt(branchesStr));
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Invalid branches: " + branchesStr));
            }
            int slot = random.nextInt(branches);
            followConnections(node, slot, depth);
        }

        private void executeLoop(ScriptNodeData node, int depth) {
            String countStr = node.getParam("count");
            int count = 3;
            try {
                if (countStr != null) count = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Invalid count: " + countStr));
            }
            for (int i = 0; i < count; i++) {
                followConnections(node, 0, depth);
            }
            followConnections(node, 1, depth);
        }

        private void followConnections(ScriptNodeData node, int slot, int depth) {
            List<ScriptNodeConnection> connections = node.getConnections();
            for (ScriptNodeConnection conn : connections) {
                if (conn.getSourceSlot() == slot) {
                    ScriptNodeData target = graph.getNode(conn.getTargetNodeId());
                    if (target != null) {
                        executeNode(target, depth + 1);
                    }
                }
            }
        }

        private boolean evaluateCondition(String condition) {
            if (condition == null || condition.isEmpty()) return false;
            String lower = condition.trim().toLowerCase();
            if (lower.equals("true")) return true;
            if (lower.equals("false")) return false;
            ScriptEngine engine = ScriptEngine.getInstance();
            if (!engine.isAvailable()) return false;
            try {
                Object result = engine.execute(condition, player, level);
                if (result instanceof Boolean) return (Boolean) result;
                if (result instanceof Number) return ((Number) result).doubleValue() != 0;
                if (result instanceof String) return Boolean.parseBoolean((String) result);
                return result != null;
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                player.sendSystemMessage(Component.literal("§c[Condition Error] " + msg));
                return false;
            }
        }
    }
}