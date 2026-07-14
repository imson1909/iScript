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

public class RequestStateMachinesPacket {
    public RequestStateMachinesPacket() {}

    public static void encode(RequestStateMachinesPacket msg, FriendlyByteBuf buf) {}

    public static RequestStateMachinesPacket decode(FriendlyByteBuf buf) {
        return new RequestStateMachinesPacket();
    }

    public static void handle(RequestStateMachinesPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            StateMachineData data = StateMachineData.get(player.serverLevel());
            Map<String, String> cache = new HashMap<>();
            for (StateMachine m : data.getMachines()) {
                cache.put(m.id, m.name);
            }
            IScriptNetwork.sendToPlayer(new SyncStateMachinesPacket(cache), player);
        });
        ctx.get().setPacketHandled(true);
    }
}