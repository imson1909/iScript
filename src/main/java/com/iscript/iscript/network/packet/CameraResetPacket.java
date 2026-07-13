package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.camera.CutsceneCameraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CameraResetPacket {
    public CameraResetPacket() {}

    public static void encode(CameraResetPacket packet, FriendlyByteBuf buf) {}

    public static CameraResetPacket decode(FriendlyByteBuf buf) {
        return new CameraResetPacket();
    }

    public static void handle(CameraResetPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CutsceneCameraHandler.stop();
        });
        ctx.get().setPacketHandled(true);
    }
}