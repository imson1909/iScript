package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.quest.QuestProgress;
import com.iscript.iscript.data.quest.QuestStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class PlayerQuestData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_player_quests";
    private final Map<UUID, Map<String, QuestProgress>> playerQuests = new HashMap<>();
    private final Map<UUID, Set<String>> completedQuests = new HashMap<>();

    public PlayerQuestData() {}

    public static PlayerQuestData load(CompoundTag tag) {
        PlayerQuestData data = new PlayerQuestData();
        CompoundTag players = tag.getCompound("Players");
        for (String key : players.getAllKeys()) {
            UUID uuid = UUID.fromString(key);
            CompoundTag playerTag = players.getCompound(key);
            Map<String, QuestProgress> quests = new HashMap<>();
            ListTag activeList = playerTag.getList("Active", 10);
            for (int i = 0; i < activeList.size(); i++) {
                QuestProgress progress = new QuestProgress();
                progress.load(activeList.getCompound(i));
                quests.put(progress.getQuestId(), progress);
            }
            data.playerQuests.put(uuid, quests);
            Set<String> completed = new HashSet<>();
            ListTag completedList = playerTag.getList("Completed", 8);
            for (int i = 0; i < completedList.size(); i++) {
                completed.add(completedList.getString(i));
            }
            data.completedQuests.put(uuid, completed);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, Map<String, QuestProgress>> entry : playerQuests.entrySet()) {
            UUID uuid = entry.getKey();
            CompoundTag playerTag = new CompoundTag();
            ListTag activeList = new ListTag();
            for (QuestProgress progress : entry.getValue().values()) {
                CompoundTag t = new CompoundTag();
                progress.save(t);
                activeList.add(t);
            }
            playerTag.put("Active", activeList);
            ListTag completedList = new ListTag();
            for (String id : completedQuests.getOrDefault(uuid, new HashSet<>())) {
                completedList.add(net.minecraft.nbt.StringTag.valueOf(id));
            }
            playerTag.put("Completed", completedList);
            players.put(uuid.toString(), playerTag);
        }
        tag.put("Players", players);
        return tag;
    }

    public Map<String, QuestProgress> getPlayerQuests(UUID playerId) {
        return playerQuests.computeIfAbsent(playerId, k -> new HashMap<>());
    }

    public Set<String> getCompletedQuests(UUID playerId) {
        return completedQuests.computeIfAbsent(playerId, k -> new HashSet<>());
    }

    public boolean hasCompletedQuest(UUID playerId, String questId) {
        return getCompletedQuests(playerId).contains(questId);
    }

    public boolean isQuestActive(UUID playerId, String questId) {
        QuestProgress progress = getPlayerQuests(playerId).get(questId);
        return progress != null && progress.getStatus() == QuestStatus.ACTIVE;
    }

    public void startQuest(UUID playerId, QuestProgress progress) {
        getPlayerQuests(playerId).put(progress.getQuestId(), progress);
        setDirty();
    }

    public void completeQuest(UUID playerId, String questId) {
        QuestProgress progress = getPlayerQuests(playerId).get(questId);
        if (progress != null) {
            progress.setStatus(QuestStatus.COMPLETED);
            setDirty();
        }
    }

    public void turnInQuest(UUID playerId, String questId) {
        getPlayerQuests(playerId).remove(questId);
        getCompletedQuests(playerId).add(questId);
        setDirty();
    }

    public void abandonQuest(UUID playerId, String questId) {
        getPlayerQuests(playerId).remove(questId);
        setDirty();
    }

    public static PlayerQuestData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PlayerQuestData::load, PlayerQuestData::new, DATA_NAME);
    }
}