package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.state.StateMachine;
import com.iscript.iscript.data.state.StateMachineData;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DeleteStateMachinePacket {
    public final String machineId;

    public DeleteStateMachinePacket(String machineId) {
        this.machineId = machineId;
    }

    public static void encode(DeleteStateMachinePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.machineId);
    }

    public static DeleteStateMachinePacket decode(FriendlyByteBuf buf) {
        return new DeleteStateMachinePacket(buf.readUtf());
    }

    public static void handle(DeleteStateMachinePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            StateMachineData data = StateMachineData.get(player.serverLevel());
            data.removeMachine(msg.machineId);

            Map<String, String> cache = new HashMap<>();
            for (StateMachine m : data.getMachines()) {
                cache.put(m.id, m.name);
            }
            IScriptNetwork.sendToAll(new SyncStateMachinesPacket(cache));
        });
        ctx.get().setPacketHandled(true);
    }
}