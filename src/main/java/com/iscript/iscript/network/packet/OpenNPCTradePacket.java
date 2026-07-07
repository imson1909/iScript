package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.npc.NPCTradeData;
import com.iscript.iscript.gui.screen.NPCTradeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenNPCTradePacket {
    private final int entityId;
    private final NPCTradeData tradeData;

    public OpenNPCTradePacket(int entityId, NPCTradeData tradeData) {
        this.entityId = entityId;
        this.tradeData = tradeData;
    }

    public OpenNPCTradePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.tradeData = new NPCTradeData();
        CompoundTag tag = buf.readNbt();
        if (tag != null) tradeData.load(tag);
    }

    public static OpenNPCTradePacket decode(FriendlyByteBuf buf) {
        return new OpenNPCTradePacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        CompoundTag tag = new CompoundTag();
        tradeData.save(tag);
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                Minecraft.getInstance().setScreen(new NPCTradeScreen(entityId, tradeData));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
