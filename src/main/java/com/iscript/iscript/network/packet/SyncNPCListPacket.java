package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.NPCManager;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.screen.NPCListSubScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncNPCListPacket {
    private final List<NPCData> npcs;

    public SyncNPCListPacket(List<NPCData> npcs) {
        this.npcs = npcs;
    }

    public SyncNPCListPacket(FriendlyByteBuf buf) {
        this.npcs = new ArrayList<>();
        ListTag list = buf.readNbt().getList("NPCs", 10);
        for (int i = 0; i < list.size(); i++) {
            NPCData data = new NPCData();
            data.load(list.getCompound(i));
            npcs.add(data);
        }
    }

    public static SyncNPCListPacket decode(FriendlyByteBuf buf) {
        return new SyncNPCListPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (NPCData data : npcs) {
            CompoundTag t = new CompoundTag();
            data.save(t);
            list.add(t);
        }
        tag.put("NPCs", list);
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            NPCManager.updateClientCache(npcs);
            if (Minecraft.getInstance().screen instanceof DashboardScreen ds && ds.currentSubScreen instanceof NPCListSubScreen sub) {
                sub.receiveList(npcs);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}