package com.iscript.iscript.data.quest;

import net.minecraft.nbt.CompoundTag;

public class QuestObjective {
    private QuestObjectiveType type = QuestObjectiveType.NONE;
    private String target = "";
    private int requiredCount = 1;
    private int currentCount = 0;
    private String description = "";

    public QuestObjectiveType getType() { return type; }
    public void setType(QuestObjectiveType type) { this.type = type; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public int getRequiredCount() { return requiredCount; }
    public void setRequiredCount(int requiredCount) { this.requiredCount = requiredCount; }
    public int getCurrentCount() { return currentCount; }
    public void setCurrentCount(int currentCount) { this.currentCount = currentCount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public void load(CompoundTag tag) {
        try {
            this.type = QuestObjectiveType.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException e) {
            this.type = QuestObjectiveType.NONE;
        }
        this.target = tag.getString("Target");
        this.requiredCount = tag.getInt("RequiredCount");
        this.currentCount = tag.getInt("CurrentCount");
        this.description = tag.getString("Description");
    }

    public void save(CompoundTag tag) {
        tag.putString("Type", this.type.name());
        tag.putString("Target", this.target);
        tag.putInt("RequiredCount", this.requiredCount);
        tag.putInt("CurrentCount", this.currentCount);
        tag.putString("Description", this.description);
    }

    public QuestObjective copy() {
        QuestObjective copy = new QuestObjective();
        copy.type = this.type;
        copy.target = this.target;
        copy.requiredCount = this.requiredCount;
        copy.currentCount = this.currentCount;
        copy.description = this.description;
        return copy;
    }

    public QuestObjective copyTemplate() {
        QuestObjective copy = new QuestObjective();
        copy.type = this.type;
        copy.target = this.target;
        copy.requiredCount = this.requiredCount;
        copy.description = this.description;
        return copy;
    }
}