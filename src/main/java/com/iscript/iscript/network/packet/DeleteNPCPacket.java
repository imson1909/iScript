package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.NPCManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeleteNPCPacket {
    private final String id;

    public DeleteNPCPacket(String id) { this.id = id; }
    public DeleteNPCPacket(FriendlyByteBuf buf) { this.id = buf.readUtf(64); }
    public static DeleteNPCPacket decode(FriendlyByteBuf buf) { return new DeleteNPCPacket(buf); }
    public void encode(FriendlyByteBuf buf) { buf.writeUtf(id); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            var sl = player.server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (sl != null) NPCManager.delete(sl, id);
        });
        ctx.get().setPacketHandled(true);
    }
}