package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.gui.screen.RegionEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenRegionScreenPacket {
    private final BlockPos pos;
    private final CompoundTag data;

    public OpenRegionScreenPacket(BlockPos pos, CompoundTag data) {
        this.pos = pos;
        this.data = data != null ? data : new CompoundTag();
    }

    public static void encode(OpenRegionScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeNbt(msg.data);
    }

    public static OpenRegionScreenPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        CompoundTag tag = buf.readNbt();
        return new OpenRegionScreenPacket(pos, tag);
    }

    public static void handle(OpenRegionScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance() != null) {
                RegionData d = new RegionData();
                d.load(msg.data);
                Minecraft.getInstance().setScreen(new RegionEditScreen(msg.pos, d));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}