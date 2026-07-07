package com.iscript.iscript.data.quest;

import net.minecraft.nbt.CompoundTag;

public class QuestData {
    private String id = "";
    private String title = "New Quest";
    private String description = "";
    private QuestObjectiveType objectiveType = QuestObjectiveType.NONE;
    private String target = "";
    private int requiredCount = 1;
    private String rewardCommand = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc; }

    public QuestObjectiveType getObjectiveType() { return objectiveType; }
    public void setObjectiveType(QuestObjectiveType type) { this.objectiveType = type; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public int getRequiredCount() { return requiredCount; }
    public void setRequiredCount(int count) { this.requiredCount = count; }

    public String getRewardCommand() { return rewardCommand; }
    public void setRewardCommand(String cmd) { this.rewardCommand = cmd; }

    public void save(CompoundTag tag) {
        tag.putString("Id", this.id);
        tag.putString("Title", this.title);
        tag.putString("Description", this.description);
        tag.putString("ObjectiveType", this.objectiveType.name());
        tag.putString("Target", this.target);
        tag.putInt("RequiredCount", this.requiredCount);
        tag.putString("RewardCommand", this.rewardCommand);
    }

    public void load(CompoundTag tag) {
        this.id = tag.getString("Id");
        this.title = tag.getString("Title");
        this.description = tag.getString("Description");
        try {
            this.objectiveType = QuestObjectiveType.valueOf(tag.getString("ObjectiveType"));
        } catch (IllegalArgumentException e) {
            this.objectiveType = QuestObjectiveType.NONE;
        }
        this.target = tag.getString("Target");
        this.requiredCount = tag.getInt("RequiredCount");
        this.rewardCommand = tag.getString("RewardCommand");
    }
}