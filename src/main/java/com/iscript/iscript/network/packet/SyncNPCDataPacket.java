package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncNPCDataPacket {
    private final int entityId;
    private final String name;
    private final String dialogId;
    private final String skin;
    private final String faction;

    public SyncNPCDataPacket(int entityId, NPCData data) {
        this.entityId = entityId;
        this.name = data.getName();
        this.dialogId = data.getDialogId();
        this.skin = data.getSkin();
        this.faction = data.getFaction();
    }

    public SyncNPCDataPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.name = buf.readUtf(32767);
        this.dialogId = buf.readUtf(32767);
        this.skin = buf.readUtf(32767);
        this.faction = buf.readUtf(32767);
    }

    public static SyncNPCDataPacket decode(FriendlyByteBuf buf) {
        return new SyncNPCDataPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(name);
        buf.writeUtf(dialogId);
        buf.writeUtf(skin);
        buf.writeUtf(faction);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                net.minecraft.world.entity.Entity entity = Minecraft.getInstance().level.getEntity(entityId);
                if (entity instanceof IScriptNPCEntity npc) {
                    NPCData data = new NPCData();
                    data.setName(name);
                    data.setDialogId(dialogId);
                    data.setSkin(skin);
                    data.setFaction(faction);
                    npc.setNPCData(data);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
