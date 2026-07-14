package com.iscript.iscript.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class QuestObjectiveUpdatePacket {
    private final String questId;
    private final int stageIndex;
    private final int objectiveIndex;
    private final int currentCount;
    private final int requiredCount;
    private final boolean stageComplete;
    private final boolean questComplete;

    public QuestObjectiveUpdatePacket(String questId, int stageIndex, int objectiveIndex, int currentCount, int requiredCount, boolean stageComplete, boolean questComplete) {
        this.questId = questId;
        this.stageIndex = stageIndex;
        this.objectiveIndex = objectiveIndex;
        this.currentCount = currentCount;
        this.requiredCount = requiredCount;
        this.stageComplete = stageComplete;
        this.questComplete = questComplete;
    }

    public QuestObjectiveUpdatePacket(FriendlyByteBuf buf) {
        this.questId = buf.readUtf();
        this.stageIndex = buf.readInt();
        this.objectiveIndex = buf.readInt();
        this.currentCount = buf.readInt();
        this.requiredCount = buf.readInt();
        this.stageComplete = buf.readBoolean();
        this.questComplete = buf.readBoolean();
    }

    public static QuestObjectiveUpdatePacket decode(FriendlyByteBuf buf) {
        return new QuestObjectiveUpdatePacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(questId);
        buf.writeInt(stageIndex);
        buf.writeInt(objectiveIndex);
        buf.writeInt(currentCount);
        buf.writeInt(requiredCount);
        buf.writeBoolean(stageComplete);
        buf.writeBoolean(questComplete);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.iscript.iscript.client.ClientQuestCache.updateObjective(questId, stageIndex, objectiveIndex, currentCount, requiredCount, stageComplete, questComplete);
        });
        ctx.get().setPacketHandled(true);
    }
}