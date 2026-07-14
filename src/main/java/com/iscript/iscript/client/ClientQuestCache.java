package com.iscript.iscript.client;

import com.iscript.iscript.data.quest.QuestProgress;
import com.iscript.iscript.data.quest.QuestStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClientQuestCache {
    private static final Map<String, QuestProgress> activeQuests = new HashMap<>();
    private static final Set<String> completedQuests = new HashSet<>();

    public static void update(CompoundTag tag) {
        activeQuests.clear();
        completedQuests.clear();
        ListTag activeList = tag.getList("Active", 10);
        for (int i = 0; i < activeList.size(); i++) {
            QuestProgress progress = new QuestProgress();
            progress.load(activeList.getCompound(i));
            activeQuests.put(progress.getQuestId(), progress);
        }
        ListTag completedList = tag.getList("Completed", 8);
        for (int i = 0; i < completedList.size(); i++) {
            completedQuests.add(completedList.getString(i));
        }
    }

    public static void updateObjective(String questId, int stageIndex, int objectiveIndex, int currentCount, int requiredCount, boolean stageComplete, boolean questComplete) {
        QuestProgress progress = activeQuests.get(questId);
        if (progress == null) return;
        if (stageIndex < 0 || stageIndex >= progress.getStages().size()) return;
        var stage = progress.getStages().get(stageIndex);
        if (objectiveIndex < 0 || objectiveIndex >= stage.getObjectives().size()) return;
        var obj = stage.getObjectives().get(objectiveIndex);
        obj.setCurrentCount(currentCount);
        if (stageComplete && !questComplete) {
            progress.setCurrentStageIndex(stageIndex + 1);
        }
        if (questComplete) {
            progress.setStatus(QuestStatus.COMPLETED);
        }
    }

    public static Map<String, QuestProgress> getActiveQuests() { return activeQuests; }
    public static Set<String> getCompletedQuests() { return completedQuests; }
    public static boolean isActive(String questId) {
        QuestProgress p = activeQuests.get(questId);
        return p != null && p.getStatus() == QuestStatus.ACTIVE;
    }
    public static boolean isCompleted(String questId) { return completedQuests.contains(questId); }
}