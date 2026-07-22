package com.iscript.iscript.morph.network;

import com.iscript.iscript.morph.MorphData;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MorphSyncPacket {
    private final CompoundTag data;
    private final int playerId;

    public MorphSyncPacket(int playerId, CompoundTag data) {
        this.playerId = playerId;
        this.data = data;
    }

    public static void encode(MorphSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.playerId);
        buf.writeNbt(packet.data);
    }

    public static MorphSyncPacket decode(FriendlyByteBuf buf) {
        return new MorphSyncPacket(buf.readInt(), buf.readNbt());
    }

    public static void handle(MorphSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                if (Minecraft.getInstance().level.getEntity(packet.playerId) instanceof Player player) {
                    player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
                        data.deserialize(packet.data);
                    });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static MorphSyncPacket create(Player player) {
        CompoundTag tag = player.getCapability(MorphData.CAPABILITY)
                .map(MorphData::serialize)
                .orElse(new CompoundTag());
        return new MorphSyncPacket(player.getId(), tag);
    }

    public static void syncToTracking(Player player) {
        IScriptNetwork.sendToTracking(player, create(player));
    }
}