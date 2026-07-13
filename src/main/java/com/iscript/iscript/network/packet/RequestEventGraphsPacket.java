package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.event.EventGraphManager;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestEventGraphsPacket {
    public RequestEventGraphsPacket() {}
    public RequestEventGraphsPacket(FriendlyByteBuf buf) {}

    public static RequestEventGraphsPacket decode(FriendlyByteBuf buf) {
        return new RequestEventGraphsPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.level() instanceof ServerLevel serverLevel)) return;
            IScriptNetwork.sendToPlayer(new SyncEventGraphsPacket(EventGraphManager.getAll(serverLevel)), player);
        });
        ctx.get().setPacketHandled(true);
    }
}