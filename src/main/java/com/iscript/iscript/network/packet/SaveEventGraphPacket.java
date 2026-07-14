package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.event.EventGraphData;
import com.iscript.iscript.data.event.EventGraphManager;
import com.iscript.iscript.data.EventSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveEventGraphPacket {
    private final EventGraphData graph;

    public SaveEventGraphPacket(EventGraphData graph) {
        this.graph = graph;
    }

    public SaveEventGraphPacket(FriendlyByteBuf buf) {
        this.graph = new EventGraphData();
        this.graph.load(buf.readNbt());
    }

    public static SaveEventGraphPacket decode(FriendlyByteBuf buf) {
        return new SaveEventGraphPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        graph.save(tag);
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level() instanceof ServerLevel serverLevel) {
                EventGraphManager.add(serverLevel, graph);
                EventSavedData.get(serverLevel).setGraph(graph.getId(), graph);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}