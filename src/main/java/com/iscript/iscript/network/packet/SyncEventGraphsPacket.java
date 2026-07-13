package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.event.EventGraphData;
import com.iscript.iscript.data.event.EventGraphManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncEventGraphsPacket {
    private final Map<String, EventGraphData> graphs;

    public SyncEventGraphsPacket(Map<String, EventGraphData> graphs) {
        this.graphs = graphs != null ? graphs : new HashMap<>();
    }

    public SyncEventGraphsPacket(FriendlyByteBuf buf) {
        this.graphs = new HashMap<>();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf(32767);
            CompoundTag tag = buf.readNbt();
            if (tag != null) {
                EventGraphData graph = new EventGraphData();
                graph.load(tag);
                graphs.put(id, graph);
            }
        }
    }

    public static SyncEventGraphsPacket decode(FriendlyByteBuf buf) {
        return new SyncEventGraphsPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(graphs.size());
        for (Map.Entry<String, EventGraphData> entry : graphs.entrySet()) {
            buf.writeUtf(entry.getKey());
            CompoundTag tag = new CompoundTag();
            entry.getValue().save(tag);
            buf.writeNbt(tag);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            EventGraphManager.updateClientCache(graphs);
        });
        ctx.get().setPacketHandled(true);
    }
}