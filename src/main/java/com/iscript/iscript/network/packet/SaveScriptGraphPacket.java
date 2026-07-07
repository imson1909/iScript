package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveScriptGraphPacket {
    private ScriptGraphData graph;

    public SaveScriptGraphPacket(ScriptGraphData graph) {
        this.graph = graph;
    }

    public SaveScriptGraphPacket(FriendlyByteBuf buf) {
        this.graph = new ScriptGraphData();
        this.graph.load(buf.readNbt());
    }

    public static SaveScriptGraphPacket decode(FriendlyByteBuf buf) {
        return new SaveScriptGraphPacket(buf);
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
                ScriptGraphManager.add(serverLevel, graph);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}