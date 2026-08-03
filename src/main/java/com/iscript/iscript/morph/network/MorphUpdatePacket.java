package com.iscript.iscript.morph.network;

import com.iscript.iscript.morph.MorphData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MorphUpdatePacket {
    private final CompoundTag data;

    public MorphUpdatePacket(CompoundTag data) {
        this.data = data;
    }

    public static void encode(MorphUpdatePacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static MorphUpdatePacket decode(FriendlyByteBuf buf) {
        return new MorphUpdatePacket(buf.readNbt());
    }

    public static void handle(MorphUpdatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
                    data.deserialize(packet.data);
                    MorphSyncPacket.syncToTracking(player);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}