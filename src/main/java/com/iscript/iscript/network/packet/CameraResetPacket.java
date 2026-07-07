package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.CameraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CameraResetPacket {
    public CameraResetPacket() {}

    public CameraResetPacket(FriendlyByteBuf buf) {}

    public static CameraResetPacket decode(FriendlyByteBuf buf) {
        return new CameraResetPacket();
    }

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(CameraHandler::resetCamera);
        ctx.get().setPacketHandled(true);
    }
}