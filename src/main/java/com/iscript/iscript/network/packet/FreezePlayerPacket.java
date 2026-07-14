package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.CameraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FreezePlayerPacket {
    public FreezePlayerPacket() {}
    public FreezePlayerPacket(FriendlyByteBuf buf) {}

    public static FreezePlayerPacket decode(FriendlyByteBuf buf) {
        return new FreezePlayerPacket();
    }

    public static void encode(FreezePlayerPacket packet, FriendlyByteBuf buf) {}

    public static void handle(FreezePlayerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CameraHandler.setFrozen(true);
        });
        ctx.get().setPacketHandled(true);
    }
}