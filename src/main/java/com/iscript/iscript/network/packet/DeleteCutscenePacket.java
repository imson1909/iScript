package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.CutsceneManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeleteCutscenePacket {
    private final String id;

    public DeleteCutscenePacket(String id) {
        this.id = id;
    }

    public static void encode(DeleteCutscenePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.id);
    }

    public static DeleteCutscenePacket decode(FriendlyByteBuf buf) {
        return new DeleteCutscenePacket(buf.readUtf(32767));
    }

    public static void handle(DeleteCutscenePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                CutsceneManager.remove((ServerLevel) player.level(), packet.id);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}