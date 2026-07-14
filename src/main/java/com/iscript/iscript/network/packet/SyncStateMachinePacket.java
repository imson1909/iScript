package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.state.ClientMachineData;
import com.iscript.iscript.data.state.ClientNodeData;
import com.iscript.iscript.data.state.ClientTransitionData;
import com.iscript.iscript.data.state.StateMachineManager;
import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.screen.StateListSubScreen;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncStateMachinePacket {
    public final String id;
    public final String name;
    public final String entryNode;
    public final String nodesJson;

    public SyncStateMachinePacket(String id, String name, String entryNode, String nodesJson) {
        this.id = id;
        this.name = name;
        this.entryNode = entryNode;
        this.nodesJson = nodesJson;
    }

    public static void encode(SyncStateMachinePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.id);
        buf.writeUtf(msg.name);
        buf.writeUtf(msg.entryNode != null ? msg.entryNode : "");
        buf.writeUtf(msg.nodesJson);
    }

    public static SyncStateMachinePacket decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String name = buf.readUtf();
        String entry = buf.readUtf();
        String nodes = buf.readUtf();
        return new SyncStateMachinePacket(id, name, entry.isEmpty() ? null : entry, nodes);
    }

    public static void handle(SyncStateMachinePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientMachineData data = parseData(msg);
            StateMachineManager.setClientMachineData(msg.id, data);

            if (Minecraft.getInstance().screen instanceof DashboardScreen dash) {
                if (dash.currentSubScreen instanceof StateListSubScreen sub) {
                    sub.onMachineReceived(msg.id, msg.name, msg.entryNode, msg.nodesJson);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static ClientMachineData parseData(SyncStateMachinePacket msg) {
        ClientMachineData data = new ClientMachineData();
        data.id = msg.id;
        data.name = msg.name;
        data.entryNode = msg.entryNode;

        if (msg.nodesJson != null && !msg.nodesJson.isEmpty()) {
            try {
                JsonArray arr = JsonParser.parseString(msg.nodesJson).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    ClientNodeData node = new ClientNodeData();
                    node.id = obj.get("id").getAsString();
                    node.name = obj.has("name") ? obj.get("name").getAsString() : node.id;
                    node.color = obj.has("color") ? obj.get("color").getAsInt() : 0xFF4488AA;
                    node.posX = obj.has("posX") ? obj.get("posX").getAsInt() : 0;
                    node.posY = obj.has("posY") ? obj.get("posY").getAsInt() : 0;

                    if (obj.has("transitions")) {
                        JsonArray tarr = obj.getAsJsonArray("transitions");
                        for (JsonElement tel : tarr) {
                            JsonObject tobj = tel.getAsJsonObject();
                            ClientTransitionData trans = new ClientTransitionData();
                            trans.targetNode = tobj.get("targetNode").getAsString();
                            trans.auto = obj.has("auto") && tobj.get("auto").getAsBoolean();
                            node.transitions.add(trans);
                        }
                    }
                    data.nodes.add(node);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return data;
    }
}