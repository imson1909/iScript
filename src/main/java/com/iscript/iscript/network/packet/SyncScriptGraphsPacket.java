package com.iscript.iscript.network.packet;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncScriptGraphsPacket {
    private final Map<String, ScriptGraphData> graphs;

    public SyncScriptGraphsPacket(Map<String, ScriptGraphData> graphs) {
        this.graphs = graphs != null ? graphs : new HashMap<>();
    }

    public SyncScriptGraphsPacket(FriendlyByteBuf buf) {
        this.graphs = new HashMap<>();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf(32767);
            CompoundTag tag = buf.readNbt();
            if (tag != null) {
                ScriptGraphData graph = new ScriptGraphData();
                graph.load(tag);
                graphs.put(id, graph);
            }
        }
    }

    public static SyncScriptGraphsPacket decode(FriendlyByteBuf buf) {
        return new SyncScriptGraphsPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(graphs.size());
        for (Map.Entry<String, ScriptGraphData> entry : graphs.entrySet()) {
            buf.writeUtf(entry.getKey());
            CompoundTag tag = new CompoundTag();
            entry.getValue().save(tag);
            buf.writeNbt(tag);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            IScriptMod.LOGGER.info("[CLIENT] SyncScriptGraphsPacket received, graphs: {}", graphs.size());
            ScriptGraphManager.updateClientCache(graphs);
        });
        ctx.get().setPacketHandled(true);
    }
}