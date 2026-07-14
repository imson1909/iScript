package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.gui.screen.NPCEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenNPCEditGuiPacket {
    private final int entityId;
    private final NPCData data;

    public OpenNPCEditGuiPacket(int entityId, NPCData data) {
        this.entityId = entityId;
        this.data = data;
    }

    public OpenNPCEditGuiPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.data = new NPCData();
        CompoundTag tag = buf.readNbt();
        if (tag != null) data.load(tag);
    }

    public static OpenNPCEditGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenNPCEditGuiPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        CompoundTag tag = new CompoundTag();
        data.save(tag);
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new NPCEditScreen(entityId, data));
        });
        ctx.get().setPacketHandled(true);
    }
}