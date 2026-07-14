package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.cutscene.ServerCutsceneHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CutsceneFinishedPacket {
    public CutsceneFinishedPacket() {}

    public static void encode(CutsceneFinishedPacket packet, FriendlyByteBuf buf) {}

    public static CutsceneFinishedPacket decode(FriendlyByteBuf buf) {
        return new CutsceneFinishedPacket();
    }

    public static void handle(CutsceneFinishedPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerCutsceneHandler.stop(player, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}