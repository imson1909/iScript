package com.iscript.iscript.data.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class QuestProgress {
    private String questId = "";
    private QuestStatus status = QuestStatus.ACTIVE;
    private final List<QuestStage> stages = new ArrayList<>();
    private int currentStageIndex = 0;
    private long startTimestamp = 0;

    public String getQuestId() { return questId; }
    public void setQuestId(String questId) { this.questId = questId; }
    public QuestStatus getStatus() { return status; }
    public void setStatus(QuestStatus status) { this.status = status; }
    public List<QuestStage> getStages() { return stages; }
    public int getCurrentStageIndex() { return currentStageIndex; }
    public void setCurrentStageIndex(int currentStageIndex) { this.currentStageIndex = currentStageIndex; }
    public long getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(long startTimestamp) { this.startTimestamp = startTimestamp; }

    public QuestStage getCurrentStage() {
        if (currentStageIndex >= 0 && currentStageIndex < stages.size()) {
            return stages.get(currentStageIndex);
        }
        return null;
    }

    public boolean isStageComplete() {
        QuestStage stage = getCurrentStage();
        if (stage == null) return true;
        for (QuestObjective obj : stage.getObjectives()) {
            if (obj.getCurrentCount() < obj.getRequiredCount()) return false;
        }
        return true;
    }

    public boolean advanceStage() {
        if (!isStageComplete()) return false;
        currentStageIndex++;
        if (currentStageIndex >= stages.size()) {
            status = QuestStatus.COMPLETED;
            currentStageIndex = stages.size() - 1;
            return true;
        }
        return false;
    }

    public void load(CompoundTag tag) {
        this.questId = tag.getString("QuestId");
        try {
            this.status = QuestStatus.valueOf(tag.getString("Status"));
        } catch (IllegalArgumentException e) {
            this.status = QuestStatus.ACTIVE;
        }
        this.currentStageIndex = tag.getInt("CurrentStageIndex");
        this.startTimestamp = tag.getLong("StartTimestamp");
        this.stages.clear();
        ListTag list = tag.getList("Stages", 10);
        for (int i = 0; i < list.size(); i++) {
            QuestStage stage = new QuestStage();
            stage.load(list.getCompound(i));
            this.stages.add(stage);
        }
    }

    public void save(CompoundTag tag) {
        tag.putString("QuestId", this.questId);
        tag.putString("Status", this.status.name());
        tag.putInt("CurrentStageIndex", this.currentStageIndex);
        tag.putLong("StartTimestamp", this.startTimestamp);
        ListTag list = new ListTag();
        for (QuestStage stage : this.stages) {
            CompoundTag t = new CompoundTag();
            stage.save(t);
            list.add(t);
        }
        tag.put("Stages", list);
    }

    public static QuestProgress fromTemplate(QuestData template) {
        QuestProgress progress = new QuestProgress();
        progress.questId = template.getId();
        progress.status = QuestStatus.ACTIVE;
        progress.currentStageIndex = 0;
        progress.startTimestamp = System.currentTimeMillis();
        for (QuestStage stage : template.getStages()) {
            progress.stages.add(stage.copyTemplate());
        }
        return progress;
    }
}