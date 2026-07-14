package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.cutscene.ServerCutsceneHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StopCutscenePacket {
    public final boolean resetPosition;

    public StopCutscenePacket() {
        this(false);
    }

    public StopCutscenePacket(boolean resetPosition) {
        this.resetPosition = resetPosition;
    }

    public static void encode(StopCutscenePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.resetPosition);
    }

    public static StopCutscenePacket decode(FriendlyByteBuf buf) {
        return new StopCutscenePacket(buf.readBoolean());
    }

    public static void handle(StopCutscenePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            if (packet.resetPosition) {
                ServerCutsceneHandler.stop(player, true);
            } else {
                ServerCutsceneHandler.pause(player);
            }
        });
        context.get().setPacketHandled(true);
    }
}