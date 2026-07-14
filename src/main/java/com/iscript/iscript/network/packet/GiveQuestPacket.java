package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.QuestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GiveQuestPacket {
    private final String questId;
    private final String targetPlayerName;

    public GiveQuestPacket(String questId, String targetPlayerName) {
        this.questId = questId;
        this.targetPlayerName = targetPlayerName;
    }

    public GiveQuestPacket(FriendlyByteBuf buf) {
        this.questId = buf.readUtf();
        this.targetPlayerName = buf.readUtf();
    }

    public static GiveQuestPacket decode(FriendlyByteBuf buf) {
        return new GiveQuestPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(questId);
        buf.writeUtf(targetPlayerName);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || sender.getServer() == null) return;
            if (targetPlayerName.isEmpty() || targetPlayerName.equals(sender.getGameProfile().getName())) {
                QuestManager.startQuest(sender.getServer().overworld(), sender.getUUID(), questId);
            } else {
                ServerPlayer target = sender.getServer().getPlayerList().getPlayerByName(targetPlayerName);
                if (target != null) {
                    QuestManager.startQuest(sender.getServer().overworld(), target.getUUID(), questId);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}