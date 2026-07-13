package com.iscript.iscript.network.packet;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.data.script.ScriptNodeData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveScriptTextPacket {
    private final String scriptId;
    private final String text;

    public SaveScriptTextPacket(String scriptId, String text) {
        this.scriptId = scriptId;
        this.text = text;
    }

    public SaveScriptTextPacket(FriendlyByteBuf buf) {
        this.scriptId = buf.readUtf(32767);
        this.text = buf.readUtf(32767);
    }

    public static SaveScriptTextPacket decode(FriendlyByteBuf buf) {
        return new SaveScriptTextPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(scriptId);
        buf.writeUtf(text);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.level() instanceof ServerLevel serverLevel)) return;

            if (text == null) return;

            ScriptGraphData existing = ScriptGraphManager.get(serverLevel, scriptId);
            String name = existing != null ? existing.getName() : scriptId;

            ScriptGraphData parsed = parseText(scriptId, name, text);
            ScriptGraphManager.add(serverLevel, parsed, text);

            IScriptMod.LOGGER.info("[SERVER] Script saved: {} ({} chars)", scriptId, text.length());
        });
        ctx.get().setPacketHandled(true);
    }

    private ScriptGraphData parseText(String id, String name, String text) {
        ScriptGraphData graph = new ScriptGraphData();
        graph.setId(id);
        graph.setName(name);

        ScriptNodeData currentNode = null;
        int[] counter = {0};

        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;

            if (line.endsWith("{")) {
                String header = line.substring(0, line.length() - 1).trim();
                String[] parts = header.split("\\s+", 2);
                String currentType = parts[0];
                String currentNodeId = parts.length > 1 ? parts[1] : "node_" + (counter[0]++);
                currentNode = new ScriptNodeData();
                currentNode.setId(currentNodeId);
                try {
                    currentNode.setType(ScriptNodeType.valueOf(currentType));
                } catch (IllegalArgumentException e) {
                    currentNode.setType(ScriptNodeType.SCRIPT_JS);
                }
            } else if (line.equals("}")) {
                if (currentNode != null) {
                    graph.addNode(currentNode);
                    currentNode = null;
                }
            } else if (currentNode != null && line.contains("=")) {
                int eq = line.indexOf("=");
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                currentNode.setParam(key, value);
            }
        }

        if (currentNode != null) {
            graph.addNode(currentNode);
        }

        for (var node : graph.getNodes().values()) {
            if (node.getType() == ScriptNodeType.START) {
                graph.setStartNodeId(node.getId());
                break;
            }
        }

        return graph;
    }
}