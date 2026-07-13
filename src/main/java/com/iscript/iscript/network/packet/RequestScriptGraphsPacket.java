package com.iscript.iscript.network.packet;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.script.ScriptGraphManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestScriptGraphsPacket {

    public RequestScriptGraphsPacket() {}

    public RequestScriptGraphsPacket(FriendlyByteBuf buf) {}

    public static RequestScriptGraphsPacket decode(FriendlyByteBuf buf) {
        return new RequestScriptGraphsPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.level() instanceof ServerLevel serverLevel)) return;

            IScriptMod.LOGGER.info("[SERVER] RequestScriptGraphsPacket from {}", player.getName().getString());
            IScriptNetwork.sendToPlayer(new SyncScriptGraphsPacket(ScriptGraphManager.getAll(serverLevel)), player);
        });
        ctx.get().setPacketHandled(true);
    }
}