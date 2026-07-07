package com.iscript.iscript.network.packet;

import com.iscript.iscript.gui.screen.NPCEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenNPCEditGuiPacket {
    private final int entityId;
    private final String name;
    private final String dialogId;
    private final String skin;
    private final String faction;

    public OpenNPCEditGuiPacket(int entityId, String name, String dialogId, String skin, String faction) {
        this.entityId = entityId;
        this.name = name;
        this.dialogId = dialogId;
        this.skin = skin;
        this.faction = faction;
    }

    public OpenNPCEditGuiPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.name = buf.readUtf(32767);
        this.dialogId = buf.readUtf(32767);
        this.skin = buf.readUtf(32767);
        this.faction = buf.readUtf(32767);
    }

    public static OpenNPCEditGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenNPCEditGuiPacket(buf);
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
            Minecraft.getInstance().setScreen(new NPCEditScreen(entityId, name, dialogId, skin, faction));
        });
        ctx.get().setPacketHandled(true);
    }
}
