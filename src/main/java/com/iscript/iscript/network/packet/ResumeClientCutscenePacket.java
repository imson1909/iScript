package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.camera.CutsceneCameraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ResumeClientCutscenePacket {
    public final float speed;
    public final int startTick;

    public ResumeClientCutscenePacket(float speed, int startTick) {
        this.speed = speed;
        this.startTick = startTick;
    }

    public static void encode(ResumeClientCutscenePacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.speed);
        buf.writeInt(pkt.startTick);
    }

    public static ResumeClientCutscenePacket decode(FriendlyByteBuf buf) {
        return new ResumeClientCutscenePacket(buf.readFloat(), buf.readInt());
    }

    public static void handle(ResumeClientCutscenePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CutsceneCameraHandler.resume(pkt.speed, pkt.startTick);
        });
        ctx.get().setPacketHandled(true);
    }
}