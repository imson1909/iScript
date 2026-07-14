package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.state.StateMachine;
import com.iscript.iscript.data.state.StateMachineData;
import com.iscript.iscript.data.state.StateNode;
import com.iscript.iscript.data.state.StateTransition;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestStateMachinePacket {
    public final String machineId;

    public RequestStateMachinePacket(String machineId) {
        this.machineId = machineId;
    }

    public static void encode(RequestStateMachinePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.machineId);
    }

    public static RequestStateMachinePacket decode(FriendlyByteBuf buf) {
        return new RequestStateMachinePacket(buf.readUtf());
    }

    public static void handle(RequestStateMachinePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            StateMachineData data = StateMachineData.get(player.serverLevel());
            StateMachine machine = data.getMachine(msg.machineId);
            if (machine == null) return;

            StringBuilder nodesJson = new StringBuilder();
            nodesJson.append("[");
            boolean first = true;
            for (StateNode n : machine.nodes.values()) {
                if (!first) nodesJson.append(",");
                first = false;
                nodesJson.append("{");
                nodesJson.append("\"id\":\"").append(escape(n.id)).append("\",");
                nodesJson.append("\"name\":\"").append(escape(n.name)).append("\",");
                nodesJson.append("\"color\":").append(n.color).append(",");
                nodesJson.append("\"posX\":").append(n.posX).append(",");
                nodesJson.append("\"posY\":").append(n.posY).append(",");
                nodesJson.append("\"transitions\":[");
                boolean tfirst = true;
                for (StateTransition t : n.transitions) {
                    if (!tfirst) nodesJson.append(",");
                    tfirst = false;
                    nodesJson.append("{");
                    nodesJson.append("\"targetNode\":\"").append(escape(t.targetNode)).append("\",");
                    nodesJson.append("\"auto\":").append(t.auto);
                    nodesJson.append("}");
                }
                nodesJson.append("]}");
            }
            nodesJson.append("]");

            IScriptNetwork.sendToPlayer(new SyncStateMachinePacket(machine.id, machine.name, machine.entryNode, nodesJson.toString()), player);
        });
        ctx.get().setPacketHandled(true);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}