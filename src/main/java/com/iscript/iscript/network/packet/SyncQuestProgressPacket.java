package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.quest.QuestProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class SyncQuestProgressPacket {
    private final CompoundTag data;

    public SyncQuestProgressPacket(Map<String, QuestProgress> active, java.util.Set<String> completed) {
        this.data = new CompoundTag();
        ListTag activeList = new ListTag();
        for (QuestProgress progress : active.values()) {
            CompoundTag t = new CompoundTag();
            progress.save(t);
            activeList.add(t);
        }
        this.data.put("Active", activeList);
        ListTag completedList = new ListTag();
        for (String id : completed) {
            completedList.add(net.minecraft.nbt.StringTag.valueOf(id));
        }
        this.data.put("Completed", completedList);
    }

    public SyncQuestProgressPacket(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public static SyncQuestProgressPacket decode(FriendlyByteBuf buf) {
        return new SyncQuestProgressPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.iscript.iscript.client.ClientQuestCache.update(data);
        });
        ctx.get().setPacketHandled(true);
    }
}