package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.CameraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UnfreezePlayerPacket {
    public UnfreezePlayerPacket() {}
    public UnfreezePlayerPacket(FriendlyByteBuf buf) {}

    public static UnfreezePlayerPacket decode(FriendlyByteBuf buf) {
        return new UnfreezePlayerPacket();
    }

    public static void encode(UnfreezePlayerPacket packet, FriendlyByteBuf buf) {}

    public static void handle(UnfreezePlayerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CameraHandler.resetCamera();
        });
        ctx.get().setPacketHandled(true);
    }
}