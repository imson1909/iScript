package com.iscript.iscript.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.HashMap;
import java.util.Map;

public class PlayerQuestData {
    private final Map<String, Integer> questProgress = new HashMap<>();
    private final Map<String, Boolean> questCompleted = new HashMap<>();

    public int getProgress(String questId) {
        return questProgress.getOrDefault(questId, 0);
    }

    public void setProgress(String questId, int progress) {
        questProgress.put(questId, progress);
    }

    public boolean isCompleted(String questId) {
        return questCompleted.getOrDefault(questId, false);
    }

    public void complete(String questId) {
        questCompleted.put(questId, true);
    }

    public void reset(String questId) {
        questProgress.remove(questId);
        questCompleted.remove(questId);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag progress = new CompoundTag();
        for (Map.Entry<String, Integer> e : questProgress.entrySet()) {
            progress.putInt(e.getKey(), e.getValue());
        }
        tag.put("Progress", progress);
        ListTag completed = new ListTag();
        for (String s : questCompleted.keySet()) {
            if (questCompleted.get(s)) {
                CompoundTag t = new CompoundTag();
                t.putString("Id", s);
                completed.add(t);
            }
        }
        tag.put("Completed", completed);
        return tag;
    }

    public void load(CompoundTag tag) {
        questProgress.clear();
        questCompleted.clear();
        CompoundTag progress = tag.getCompound("Progress");
        for (String key : progress.getAllKeys()) {
            questProgress.put(key, progress.getInt(key));
        }
        ListTag completed = tag.getList("Completed", 10);
        for (int i = 0; i < completed.size(); i++) {
            questCompleted.put(completed.getCompound(i).getString("Id"), true);
        }
    }
}
