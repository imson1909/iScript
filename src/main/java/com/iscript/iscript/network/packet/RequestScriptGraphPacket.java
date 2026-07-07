package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestScriptGraphPacket {
    private final String graphId;

    public RequestScriptGraphPacket(String graphId) {
        this.graphId = graphId;
    }

    public RequestScriptGraphPacket(FriendlyByteBuf buf) {
        this.graphId = buf.readUtf(32767);
    }

    public static RequestScriptGraphPacket decode(FriendlyByteBuf buf) {
        return new RequestScriptGraphPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(graphId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ScriptGraphData graph = ScriptGraphManager.get((ServerLevel) player.level(), graphId);
            if (graph == null) {
                graph = new ScriptGraphData();
                graph.setId(graphId);
                graph.setName(graphId);
                com.iscript.iscript.data.script.ScriptNodeData start = new com.iscript.iscript.data.script.ScriptNodeData();
                start.setId("start");
                start.setType(com.iscript.iscript.data.script.ScriptNodeType.START);
                start.setX(100);
                start.setY(100);
                graph.addNode(start);
                ScriptGraphManager.add((ServerLevel) player.level(), graph);
            }
            IScriptNetwork.sendToPlayer(new OpenScriptGraphEditorPacket(graphId, graph), player);
        });
        ctx.get().setPacketHandled(true);
    }
}