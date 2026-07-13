package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.cutscene.ServerCutsceneHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ResumeCutscenePacket {
    public final float speed;
    public final int startTick;

    public ResumeCutscenePacket(float speed, int startTick) {
        this.speed = speed;
        this.startTick = startTick;
    }

    public static void encode(ResumeCutscenePacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.speed);
        buf.writeInt(pkt.startTick);
    }

    public static ResumeCutscenePacket decode(FriendlyByteBuf buf) {
        return new ResumeCutscenePacket(buf.readFloat(), buf.readInt());
    }

    public static void handle(ResumeCutscenePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerCutsceneHandler.resume(player, pkt.speed, pkt.startTick);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}