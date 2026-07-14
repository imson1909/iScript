package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.CutsceneManager;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.data.cutscene.ServerCutsceneHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayCutscenePacket {
    public final String cutsceneId;
    public final float speed;
    public final int startTick;

    public PlayCutscenePacket(String cutsceneId) {
        this(cutsceneId, 1.0f, 0);
    }

    public PlayCutscenePacket(String cutsceneId, float speed) {
        this(cutsceneId, speed, 0);
    }

    public PlayCutscenePacket(String cutsceneId, float speed, int startTick) {
        this.cutsceneId = cutsceneId;
        this.speed = speed;
        this.startTick = startTick;
    }

    public static void encode(PlayCutscenePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.cutsceneId);
        buf.writeFloat(packet.speed);
        buf.writeInt(packet.startTick);
    }

    public static PlayCutscenePacket decode(FriendlyByteBuf buf) {
        return new PlayCutscenePacket(buf.readUtf(), buf.readFloat(), buf.readInt());
    }

    public static void handle(PlayCutscenePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            CutsceneData data = CutsceneManager.get(level, packet.cutsceneId);
            if (data == null) {
                System.out.println("[Server] Cutscene not found: " + packet.cutsceneId);
                return;
            }

            ServerCutsceneHandler.play(player, data, packet.speed, packet.startTick);
        });
        context.get().setPacketHandled(true);
    }
}