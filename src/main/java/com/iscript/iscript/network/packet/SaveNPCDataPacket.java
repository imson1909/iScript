package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveNPCDataPacket {
    private final int entityId;
    private final NPCData data;

    public SaveNPCDataPacket(int entityId, NPCData data) {
        this.entityId = entityId;
        this.data = data;
    }

    public SaveNPCDataPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.data = new NPCData();
        net.minecraft.nbt.CompoundTag tag = buf.readNbt();
        if (tag != null) data.load(tag);
    }

    public static SaveNPCDataPacket decode(FriendlyByteBuf buf) {
        return new SaveNPCDataPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        data.save(tag);
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel) {
                net.minecraft.world.entity.Entity entity = player.level().getEntity(entityId);
                if (entity instanceof IScriptNPCEntity npc) {
                    npc.setNPCData(data);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
