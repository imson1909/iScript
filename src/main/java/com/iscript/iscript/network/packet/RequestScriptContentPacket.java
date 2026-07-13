package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.data.script.ScriptNodeData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.script.ScriptFileManager;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestScriptContentPacket {
    private final String scriptId;

    public RequestScriptContentPacket(String scriptId) {
        this.scriptId = scriptId != null ? scriptId : "";
    }

    public RequestScriptContentPacket(FriendlyByteBuf buf) {
        this.scriptId = buf.readUtf(32767);
    }

    public static RequestScriptContentPacket decode(FriendlyByteBuf buf) {
        return new RequestScriptContentPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(scriptId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.level() instanceof ServerLevel serverLevel)) return;

            ScriptGraphData graph = ScriptGraphManager.get(serverLevel, scriptId);
            String jsText = ScriptFileManager.loadScriptJs(serverLevel, scriptId);

            if (graph == null || !ScriptFileManager.scriptExists(serverLevel, scriptId)) {
                graph = new ScriptGraphData();
                graph.setId(scriptId);
                graph.setName(scriptId);
                ScriptNodeData start = new ScriptNodeData();
                start.setId("start");
                start.setType(ScriptNodeType.START);
                start.setX(100);
                start.setY(100);
                graph.addNode(start);
                ScriptGraphManager.add(serverLevel, graph, "");
                jsText = "";
            }

            IScriptNetwork.sendToPlayer(new ScriptGraphContentPacket(scriptId, jsText, graph), player);
        });
        ctx.get().setPacketHandled(true);
    }
}