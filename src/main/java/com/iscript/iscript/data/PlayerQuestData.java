package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.quest.QuestProgress;
import com.iscript.iscript.data.quest.QuestStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public class PlayerQuestData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_player_quests";
    private final Map<UUID, PlayerEntry> players = new HashMap<>();

    private static class PlayerEntry {
        final Map<String, QuestProgress> active = new HashMap<>();
        final Set<String> completed = new HashSet<>();

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            ListTag activeList = new ListTag();
            active.values().forEach(p -> activeList.add(p.save()));
            tag.put("Active", activeList);
            ListTag completedList = new ListTag();
            completed.forEach(id -> completedList.add(StringTag.valueOf(id)));
            tag.put("Completed", completedList);
            return tag;
        }

        void load(CompoundTag tag) {
            active.clear();
            completed.clear();
            ListTag activeList = tag.getList("Active", Tag.TAG_COMPOUND);
            for (int i = 0; i < activeList.size(); i++) {
                QuestProgress p = new QuestProgress();
                p.load(activeList.getCompound(i));
                active.put(p.getQuestId(), p);
            }
            ListTag completedList = tag.getList("Completed", Tag.TAG_STRING);
            for (int i = 0; i < completedList.size(); i++) {
                completed.add(completedList.getString(i));
            }
        }
    }

    public static PlayerQuestData load(CompoundTag tag) {
        PlayerQuestData data = new PlayerQuestData();
        CompoundTag playersTag = tag.getCompound("Players");
        for (String key : playersTag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerEntry entry = new PlayerEntry();
                entry.load(playersTag.getCompound(key));
                data.players.put(uuid, entry);
            } catch (IllegalArgumentException ignored) {}
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag playersTag = new CompoundTag();
        players.forEach((uuid, entry) -> playersTag.put(uuid.toString(), entry.save()));
        tag.put("Players", playersTag);
        return tag;
    }

    public Map<String, QuestProgress> getActive(UUID playerId) {
        return players.computeIfAbsent(playerId, k -> new PlayerEntry()).active;
    }

    public Set<String> getCompleted(UUID playerId) {
        return players.computeIfAbsent(playerId, k -> new PlayerEntry()).completed;
    }

    public boolean hasCompleted(UUID playerId, String questId) {
        return getCompleted(playerId).contains(questId);
    }

    public boolean isActive(UUID playerId, String questId) {
        QuestProgress p = getActive(playerId).get(questId);
        return p != null && p.getStatus() == QuestStatus.ACTIVE;
    }

    public void startQuest(UUID playerId, QuestProgress progress) {
        getActive(playerId).put(progress.getQuestId(), progress);
        setDirty();
    }

    public void completeQuest(UUID playerId, String questId) {
        QuestProgress p = getActive(playerId).get(questId);
        if (p != null) {
            p.setStatus(QuestStatus.COMPLETED);
            setDirty();
        }
    }

    public void turnIn(UUID playerId, String questId) {
        getActive(playerId).remove(questId);
        getCompleted(playerId).add(questId);
        setDirty();
    }

    public void abandon(UUID playerId, String questId) {
        if (getActive(playerId).remove(questId) != null) setDirty();
    }

    public static PlayerQuestData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PlayerQuestData::load, PlayerQuestData::new, DATA_NAME);
    }
}
