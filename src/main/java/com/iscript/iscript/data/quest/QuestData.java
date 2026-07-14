package com.iscript.iscript.data.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class QuestData {
    private String id = "";
    private String title = "";
    private String description = "";
    private final List<QuestStage> stages = new ArrayList<>();
    private QuestReward reward = new QuestReward();
    private final List<String> prerequisites = new ArrayList<>();
    private String giverNpcId = "";
    private String turnInNpcId = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<QuestStage> getStages() { return stages; }
    public QuestReward getReward() { return reward; }
    public void setReward(QuestReward reward) { this.reward = reward; }
    public List<String> getPrerequisites() { return prerequisites; }
    public String getGiverNpcId() { return giverNpcId; }
    public void setGiverNpcId(String giverNpcId) { this.giverNpcId = giverNpcId; }
    public String getTurnInNpcId() { return turnInNpcId; }
    public void setTurnInNpcId(String turnInNpcId) { this.turnInNpcId = turnInNpcId; }

    public void load(CompoundTag tag) {
        this.id = tag.getString("Id");
        this.title = tag.getString("Title");
        this.description = tag.getString("Description");
        this.stages.clear();
        ListTag stageList = tag.getList("Stages", 10);
        for (int i = 0; i < stageList.size(); i++) {
            QuestStage stage = new QuestStage();
            stage.load(stageList.getCompound(i));
            this.stages.add(stage);
        }
        this.reward = new QuestReward();
        this.reward.load(tag.getCompound("Reward"));
        this.prerequisites.clear();
        ListTag prereqList = tag.getList("Prerequisites", 8);
        for (int i = 0; i < prereqList.size(); i++) {
            this.prerequisites.add(prereqList.getString(i));
        }
        this.giverNpcId = tag.getString("GiverNpcId");
        this.turnInNpcId = tag.getString("TurnInNpcId");
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", this.id);
        tag.putString("Title", this.title);
        tag.putString("Description", this.description);
        ListTag stageList = new ListTag();
        for (QuestStage stage : this.stages) {
            CompoundTag t = new CompoundTag();
            stage.save(t);
            stageList.add(t);
        }
        tag.put("Stages", stageList);
        CompoundTag rewardTag = new CompoundTag();
        this.reward.save(rewardTag);
        tag.put("Reward", rewardTag);
        ListTag prereqList = new ListTag();
        for (String prereq : this.prerequisites) {
            prereqList.add(net.minecraft.nbt.StringTag.valueOf(prereq));
        }
        tag.put("Prerequisites", prereqList);
        tag.putString("GiverNpcId", this.giverNpcId);
        tag.putString("TurnInNpcId", this.turnInNpcId);
    }

    public QuestData copy() {
        QuestData copy = new QuestData();
        copy.id = this.id;
        copy.title = this.title;
        copy.description = this.description;
        for (QuestStage stage : this.stages) {
            copy.stages.add(stage.copy());
        }
        copy.reward = this.reward.copy();
        copy.prerequisites.addAll(this.prerequisites);
        copy.giverNpcId = this.giverNpcId;
        copy.turnInNpcId = this.turnInNpcId;
        return copy;
    }
}