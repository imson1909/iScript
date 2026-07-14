package com.iscript.iscript.data.quest;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class QuestChainData {
    private String id = "";
    private String title = "Quest Chain";
    private List<String> questIds = new ArrayList<>();
    private boolean sequential = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<String> getQuestIds() { return questIds; }
    public boolean isSequential() { return sequential; }
    public void setSequential(boolean seq) { this.sequential = seq; }

    public void save(CompoundTag tag) {
        tag.putString("Id", id);
        tag.putString("Title", title);
        tag.putBoolean("Sequential", sequential);
        CompoundTag quests = new CompoundTag();
        for (int i = 0; i < questIds.size(); i++) {
            quests.putString(String.valueOf(i), questIds.get(i));
        }
        tag.put("QuestIds", quests);
    }

    public void load(CompoundTag tag) {
        id = tag.getString("Id");
        title = tag.getString("Title");
        sequential = tag.getBoolean("Sequential");
        questIds.clear();
        CompoundTag quests = tag.getCompound("QuestIds");
        for (String key : quests.getAllKeys()) {
            questIds.add(quests.getString(key));
        }
    }
}
