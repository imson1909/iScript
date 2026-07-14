package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.state.StateMachineManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncStateMachinesPacket {
    public final Map<String, String> machines;

    public SyncStateMachinesPacket(Map<String, String> machines) {
        this.machines = machines != null ? machines : new HashMap<>();
    }

    public static void encode(SyncStateMachinesPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.machines.size());
        for (Map.Entry<String, String> e : msg.machines.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeUtf(e.getValue());
        }
    }

    public static SyncStateMachinesPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(), buf.readUtf());
        }
        return new SyncStateMachinesPacket(map);
    }

    public static void handle(SyncStateMachinesPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            StateMachineManager.setClientCache(msg.machines);
        });
        ctx.get().setPacketHandled(true);
    }
}