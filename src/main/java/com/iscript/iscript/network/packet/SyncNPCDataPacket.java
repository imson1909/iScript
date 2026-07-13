package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncNPCDataPacket {
    private final int entityId;
    private final NPCData data;

    public SyncNPCDataPacket(int entityId, NPCData data) {
        this.entityId = entityId;
        this.data = data;
    }

    public SyncNPCDataPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.data = new NPCData();
        net.minecraft.nbt.CompoundTag tag = buf.readNbt();
        if (tag != null) data.load(tag);
    }

    public static SyncNPCDataPacket decode(FriendlyByteBuf buf) {
        return new SyncNPCDataPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        data.save(tag);
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                net.minecraft.world.entity.Entity entity = Minecraft.getInstance().level.getEntity(entityId);
                if (entity instanceof IScriptNPCEntity npc) {
                    npc.applyNPCDataClient(data);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
