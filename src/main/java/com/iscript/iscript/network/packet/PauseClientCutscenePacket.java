package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.camera.CutsceneCameraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PauseClientCutscenePacket {
    public PauseClientCutscenePacket() {}

    public static void encode(PauseClientCutscenePacket pkt, FriendlyByteBuf buf) {}

    public static PauseClientCutscenePacket decode(FriendlyByteBuf buf) {
        return new PauseClientCutscenePacket();
    }

    public static void handle(PauseClientCutscenePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CutsceneCameraHandler.pause();
        });
        ctx.get().setPacketHandled(true);
    }
}