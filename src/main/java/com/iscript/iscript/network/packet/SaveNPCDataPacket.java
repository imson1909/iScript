package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.NPCManager;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
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
            if (player == null) return;

            if (data.getHealth() <= 0) data.setHealth(Math.max(data.getMaxHealth(), 20.0f));
            if (data.getMaxHealth() <= 0) data.setMaxHealth(20.0f);

            NPCManager.save((ServerLevel) player.level(), data.getId(), data);

            if (entityId >= 0) {
                for (ServerLevel sl : player.server.getAllLevels()) {
                    net.minecraft.world.entity.Entity entity = sl.getEntity(entityId);
                    if (entity instanceof IScriptNPCEntity npc) {
                        npc.setNPCData(data);
                        break;
                    }
                }
            }

            List<NPCData> list = new ArrayList<>();
            for (String id : NPCManager.listIds((ServerLevel) player.level())) {
                NPCData d = NPCManager.load((ServerLevel) player.level(), id);
                if (d != null) list.add(d);
            }
            com.iscript.iscript.network.IScriptNetwork.sendToPlayer(new SyncNPCListPacket(list), player);
        });
        ctx.get().setPacketHandled(true);
    }
}