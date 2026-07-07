package com.iscript.iscript.network.packet;

import com.iscript.iscript.gui.screen.DialogGraphEditorScreen;
import com.iscript.iscript.data.dialog.DialogGraphData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenDialogGraphEditorPacket {
    private final String graphId;
    private final DialogGraphData graph;

    public OpenDialogGraphEditorPacket(String graphId, DialogGraphData graph) {
        this.graphId = graphId;
        this.graph = graph;
    }

    public OpenDialogGraphEditorPacket(FriendlyByteBuf buf) {
        this.graphId = buf.readUtf(32767);
        this.graph = new DialogGraphData();
        this.graph.load(buf.readNbt());
    }

    public static OpenDialogGraphEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenDialogGraphEditorPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(graphId);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        graph.save(tag);
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new DialogGraphEditorScreen(graphId, graph));
        });
        ctx.get().setPacketHandled(true);
    }
}