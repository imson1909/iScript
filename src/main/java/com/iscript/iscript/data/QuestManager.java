package com.iscript.iscript.data;

import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.quest.QuestProgress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class QuestManager {
    public static QuestData get(ServerLevel level, String id) {
        return QuestSavedData.get(level).getQuests().get(id);
    }

    public static void add(ServerLevel level, QuestData q) {
        QuestSavedData.get(level).addQuest(q);
    }

    public static void remove(ServerLevel level, String id) {
        QuestSavedData.get(level).removeQuest(id);
    }

    public static Map<String, QuestData> getAll(ServerLevel level) {
        return QuestSavedData.get(level).getQuests();
    }

    public static boolean canStartQuest(ServerLevel level, UUID playerId, String questId) {
        QuestData quest = get(level, questId);
        if (quest == null) return false;
        PlayerQuestData data = PlayerQuestData.get(level);
        if (data.isQuestActive(playerId, questId)) return false;
        if (data.hasCompletedQuest(playerId, questId)) return false;
        for (String prereq : quest.getPrerequisites()) {
            if (!data.hasCompletedQuest(playerId, prereq)) return false;
        }
        return true;
    }

    public static boolean startQuest(ServerLevel level, UUID playerId, String questId) {
        if (!canStartQuest(level, playerId, questId)) return false;
        QuestData quest = get(level, questId);
        if (quest == null) return false;
        QuestProgress progress = QuestProgress.fromTemplate(quest);
        PlayerQuestData.get(level).startQuest(playerId, progress);
        return true;
    }

    public static void completeQuest(ServerLevel level, UUID playerId, String questId) {
        PlayerQuestData.get(level).completeQuest(playerId, questId);
    }

    public static void turnInQuest(ServerLevel level, UUID playerId, String questId) {
        PlayerQuestData.get(level).turnInQuest(playerId, questId);
    }

    public static void abandonQuest(ServerLevel level, UUID playerId, String questId) {
        PlayerQuestData.get(level).abandonQuest(playerId, questId);
    }

    public static Map<String, QuestProgress> getPlayerQuests(ServerLevel level, UUID playerId) {
        return PlayerQuestData.get(level).getPlayerQuests(playerId);
    }

    public static Set<String> getCompletedQuests(ServerLevel level, UUID playerId) {
        return PlayerQuestData.get(level).getCompletedQuests(playerId);
    }

    public static boolean hasCompletedQuest(ServerLevel level, UUID playerId, String questId) {
        return PlayerQuestData.get(level).hasCompletedQuest(playerId, questId);
    }

    public static boolean isQuestActive(ServerLevel level, UUID playerId, String questId) {
        return PlayerQuestData.get(level).isQuestActive(playerId, questId);
    }

    public static QuestProgress getQuestProgress(ServerLevel level, UUID playerId, String questId) {
        return PlayerQuestData.get(level).getPlayerQuests(playerId).get(questId);
    }
}