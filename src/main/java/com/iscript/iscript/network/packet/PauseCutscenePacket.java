package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.cutscene.ServerCutsceneHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PauseCutscenePacket {
    public PauseCutscenePacket() {}

    public static void encode(PauseCutscenePacket pkt, FriendlyByteBuf buf) {}

    public static PauseCutscenePacket decode(FriendlyByteBuf buf) {
        return new PauseCutscenePacket();
    }

    public static void handle(PauseCutscenePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerCutsceneHandler.pause(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}