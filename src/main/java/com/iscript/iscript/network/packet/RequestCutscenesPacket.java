package com.iscript.iscript.network.packet;

import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestCutscenesPacket {
    public RequestCutscenesPacket() {}

    public static void encode(RequestCutscenesPacket packet, FriendlyByteBuf buf) {}

    public static RequestCutscenesPacket decode(FriendlyByteBuf buf) {
        return new RequestCutscenesPacket();
    }

    public static void handle(RequestCutscenesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                var cutscenes = com.iscript.iscript.data.CutsceneManager.getAll((ServerLevel) player.level());
                IScriptNetwork.sendToPlayer(new SyncCutscenesPacket(cutscenes), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}