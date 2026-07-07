package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.gui.screen.ScriptGraphEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenScriptGraphEditorPacket {
    private final String graphId;
    private final ScriptGraphData graph;

    public OpenScriptGraphEditorPacket(String graphId, ScriptGraphData graph) {
        this.graphId = graphId;
        this.graph = graph;
    }

    public OpenScriptGraphEditorPacket(FriendlyByteBuf buf) {
        this.graphId = buf.readUtf(32767);
        this.graph = new ScriptGraphData();
        this.graph.load(buf.readNbt());
    }

    public static OpenScriptGraphEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenScriptGraphEditorPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(graphId);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        graph.save(tag);
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new ScriptGraphEditorScreen(graphId, graph));
        });
        ctx.get().setPacketHandled(true);
    }
}