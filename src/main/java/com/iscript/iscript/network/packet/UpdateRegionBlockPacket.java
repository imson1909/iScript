package com.iscript.iscript.network.packet;

import com.iscript.iscript.blockentities.RegionBlockEntity;
import com.iscript.iscript.data.region.RegionData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateRegionBlockPacket {
    private final BlockPos pos;
    private final CompoundTag data;

    public UpdateRegionBlockPacket(BlockPos pos, RegionData data) {
        this.pos = pos;
        this.data = data != null ? data.toNetworkTag() : new CompoundTag();
    }

    public static void encode(UpdateRegionBlockPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeNbt(msg.data);
    }

    public static UpdateRegionBlockPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        CompoundTag tag = buf.readNbt();
        return new UpdateRegionBlockPacket(pos, RegionData.fromNetworkTag(tag));
    }

    public static void handle(UpdateRegionBlockPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(msg.pos);
                if (be instanceof RegionBlockEntity rbe) {
                    RegionData d = RegionData.fromNetworkTag(msg.data);
                    rbe.setData(d);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}