package com.iscript.iscript.network.packet;

import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NPCAnimationPacket {
    private final int entityId;
    private final String animation;

    public NPCAnimationPacket(int entityId, String animation) {
        this.entityId = entityId;
        this.animation = animation;
    }

    public NPCAnimationPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.animation = buf.readUtf(32767);
    }

    public static NPCAnimationPacket decode(FriendlyByteBuf buf) {
        return new NPCAnimationPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(animation);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                var entity = Minecraft.getInstance().level.getEntity(entityId);
                if (entity instanceof IScriptNPCEntity npc) {
                    npc.playAnimation(animation);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}