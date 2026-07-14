package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.state.StateMachine;
import com.iscript.iscript.data.state.StateMachineData;
import com.iscript.iscript.data.state.StateNode;
import com.iscript.iscript.data.state.StateTransition;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DuplicateStateMachinePacket {
    public final String sourceId;
    public final String newId;

    public DuplicateStateMachinePacket(String sourceId, String newId) {
        this.sourceId = sourceId;
        this.newId = newId;
    }

    public static void encode(DuplicateStateMachinePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.sourceId);
        buf.writeUtf(msg.newId);
    }

    public static DuplicateStateMachinePacket decode(FriendlyByteBuf buf) {
        return new DuplicateStateMachinePacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(DuplicateStateMachinePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            StateMachineData data = StateMachineData.get(player.serverLevel());
            StateMachine source = data.getMachine(msg.sourceId);
            if (source == null) return;

            StateMachine copy = new StateMachine(msg.newId, source.name + " (Copy)");
            copy.entryNode = source.entryNode;
            for (StateNode n : source.nodes.values()) {
                StateNode nc = new StateNode(n.id, n.name, n.color);
                nc.posX = n.posX + 20;
                nc.posY = n.posY + 20;
                for (StateTransition t : n.transitions) {
                    StateTransition tc = new StateTransition(t.targetNode, t.auto);
                    nc.transitions.add(tc);
                }
                copy.nodes.put(nc.id, nc);
            }
            data.putMachine(copy);

            Map<String, String> cache = new HashMap<>();
            for (StateMachine m : data.getMachines()) {
                cache.put(m.id, m.name);
            }
            IScriptNetwork.sendToAll(new SyncStateMachinesPacket(cache));
        });
        ctx.get().setPacketHandled(true);
    }
}