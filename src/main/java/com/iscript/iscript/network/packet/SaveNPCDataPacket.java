package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveNPCDataPacket {
    private final int entityId;
    private final String name;
    private final String dialogId;
    private final String skin;
    private final String faction;

    public SaveNPCDataPacket(int entityId, NPCData data) {
        this.entityId = entityId;
        this.name = data.getName();
        this.dialogId = data.getDialogId();
        this.skin = data.getSkin();
        this.faction = data.getFaction();
    }

    public SaveNPCDataPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.name = buf.readUtf(32767);
        this.dialogId = buf.readUtf(32767);
        this.skin = buf.readUtf(32767);
        this.faction = buf.readUtf(32767);
    }

    public static SaveNPCDataPacket decode(FriendlyByteBuf buf) {
        return new SaveNPCDataPacket(buf);
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
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel) {
                net.minecraft.world.entity.Entity entity = player.level().getEntity(entityId);
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
