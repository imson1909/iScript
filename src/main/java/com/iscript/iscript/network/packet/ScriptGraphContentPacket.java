package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.screen.ScriptListSubScreen;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScriptGraphContentPacket {
    private final String id;
    private final String jsText;
    private final ScriptGraphData graph;

    public ScriptGraphContentPacket(String id, String jsText, ScriptGraphData graph) {
        this.id = id != null ? id : "";
        this.jsText = jsText != null ? jsText : "";
        this.graph = graph != null ? graph : new ScriptGraphData();
    }

    public ScriptGraphContentPacket(FriendlyByteBuf buf) {
        this.id = buf.readUtf(32767);
        this.jsText = buf.readUtf(32767);
        this.graph = new ScriptGraphData();
        CompoundTag tag = buf.readNbt();
        if (tag != null) this.graph.load(tag);
    }

    public static ScriptGraphContentPacket decode(FriendlyByteBuf buf) {
        return new ScriptGraphContentPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeUtf(jsText);
        CompoundTag tag = new CompoundTag();
        graph.save(tag);
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ScriptGraphManager.putClientCache(id, graph);
            ScriptGraphManager.updateClientJsCache(id, jsText);

            if (Minecraft.getInstance().screen instanceof DashboardScreen dash) {
                if (dash.currentSubScreen instanceof ScriptListSubScreen sub) {
                    sub.onContentReceived(id, jsText);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}