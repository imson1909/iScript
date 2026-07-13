package com.iscript.iscript.network.packet;

import com.iscript.iscript.gui.screen.DashboardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenDashboardPacket {
    public OpenDashboardPacket() {}
    public OpenDashboardPacket(FriendlyByteBuf buf) {}

    public static OpenDashboardPacket decode(FriendlyByteBuf buf) {
        return new OpenDashboardPacket();
    }

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new DashboardScreen());
        });
        ctx.get().setPacketHandled(true);
    }
}