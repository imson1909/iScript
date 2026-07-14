package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.NPCManager;
import com.iscript.iscript.data.npc.NPCData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RequestNPCListPacket {
    public RequestNPCListPacket() {}
    public RequestNPCListPacket(FriendlyByteBuf buf) {}
    public static RequestNPCListPacket decode(FriendlyByteBuf buf) { return new RequestNPCListPacket(); }
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            var sl = player.server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (sl == null) return;

            List<NPCData> list = new ArrayList<>();
            for (String id : NPCManager.listIds(sl)) {
                NPCData data = NPCManager.load(sl, id);
                if (data != null) list.add(data);
            }

            com.iscript.iscript.network.IScriptNetwork.sendToPlayer(new SyncNPCListPacket(list), player);
        });
        ctx.get().setPacketHandled(true);
    }
}