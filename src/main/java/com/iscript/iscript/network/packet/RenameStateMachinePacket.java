package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.state.StateMachine;
import com.iscript.iscript.data.state.StateMachineData;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RenameStateMachinePacket {
    public final String oldId;
    public final String newId;
    public final String newName;

    public RenameStateMachinePacket(String oldId, String newId, String newName) {
        this.oldId = oldId;
        this.newId = newId;
        this.newName = newName;
    }

    public static void encode(RenameStateMachinePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.oldId);
        buf.writeUtf(msg.newId);
        buf.writeUtf(msg.newName);
    }

    public static RenameStateMachinePacket decode(FriendlyByteBuf buf) {
        return new RenameStateMachinePacket(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(RenameStateMachinePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            StateMachineData data = StateMachineData.get(player.serverLevel());
            data.renameMachine(msg.oldId, msg.newId);
            StateMachine m = data.getMachine(msg.newId);
            if (m != null) m.name = msg.newName;

            Map<String, String> cache = new HashMap<>();
            for (StateMachine sm : data.getMachines()) {
                cache.put(sm.id, sm.name);
            }
            IScriptNetwork.sendToAll(new SyncStateMachinesPacket(cache));
        });
        ctx.get().setPacketHandled(true);
    }
}