package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.state.StateMachine;
import com.iscript.iscript.data.state.StateMachineData;
import com.iscript.iscript.data.state.StateNode;
import com.iscript.iscript.data.state.StateTransition;
import com.iscript.iscript.network.IScriptNetwork;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SaveStateMachinePacket {
    public final String machineId;
    public final String name;
    public final String entryNode;
    public final String nodesJson;

    public SaveStateMachinePacket(String machineId, String name, String entryNode, String nodesJson) {
        this.machineId = machineId;
        this.name = name;
        this.entryNode = entryNode;
        this.nodesJson = nodesJson;
    }

    public static void encode(SaveStateMachinePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.machineId);
        buf.writeUtf(msg.name);
        buf.writeUtf(msg.entryNode);
        buf.writeUtf(msg.nodesJson);
    }

    public static SaveStateMachinePacket decode(FriendlyByteBuf buf) {
        return new SaveStateMachinePacket(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(SaveStateMachinePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            StateMachineData data = StateMachineData.get(player.serverLevel());

            StateMachine machine = data.getMachine(msg.machineId);
            boolean isNew = false;
            if (machine == null) {
                machine = new StateMachine(msg.machineId, msg.name);
                isNew = true;
            } else {
                machine.name = msg.name;
            }
            machine.entryNode = msg.entryNode != null && !msg.entryNode.isEmpty() ? msg.entryNode : null;

            if (!msg.nodesJson.isEmpty()) {
                machine.nodes.clear();
                try {
                    JsonArray arr = JsonParser.parseString(msg.nodesJson).getAsJsonArray();
                    for (JsonElement el : arr) {
                        JsonObject obj = el.getAsJsonObject();
                        StateNode node = new StateNode();
                        node.id = obj.get("id").getAsString();
                        node.name = obj.has("name") ? obj.get("name").getAsString() : node.id;
                        node.color = obj.has("color") ? obj.get("color").getAsInt() : 0xFF4488AA;
                        node.posX = obj.has("posX") ? obj.get("posX").getAsInt() : 0;
                        node.posY = obj.has("posY") ? obj.get("posY").getAsInt() : 0;

                        if (obj.has("transitions")) {
                            JsonArray tarr = obj.getAsJsonArray("transitions");
                            for (JsonElement tel : tarr) {
                                JsonObject tobj = tel.getAsJsonObject();
                                StateTransition trans = new StateTransition();
                                trans.targetNode = tobj.get("targetNode").getAsString();
                                trans.auto = tobj.has("auto") && tobj.get("auto").getAsBoolean();
                                node.transitions.add(trans);
                            }
                        }
                        machine.nodes.put(node.id, node);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            data.putMachine(machine);

            Map<String, String> cache = new HashMap<>();
            for (StateMachine m : data.getMachines()) {
                cache.put(m.id, m.name);
            }
            IScriptNetwork.sendToAll(new SyncStateMachinesPacket(cache));
        });
        ctx.get().setPacketHandled(true);
    }
}