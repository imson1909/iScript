package com.iscript.iscript.data.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iscript.iscript.data.DataObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

public class QuestProgress implements DataObject {
    private String questId = "";
    private QuestStatus status = QuestStatus.ACTIVE;
    private final List<QuestStage> stages = new ArrayList<>();
    private int currentStageIndex;
    private long startTimestamp;

    public String getQuestId() { return questId; }
    public void setQuestId(String id) { this.questId = id; }
    public QuestStatus getStatus() { return status; }
    public void setStatus(QuestStatus status) { this.status = status; }
    public List<QuestStage> getStages() { return stages; }
    public int getCurrentStageIndex() { return currentStageIndex; }
    public void setCurrentStageIndex(int i) { this.currentStageIndex = i; }
    public long getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(long t) { this.startTimestamp = t; }

    public QuestStage getCurrentStage() {
        return currentStageIndex >= 0 && currentStageIndex < stages.size() ? stages.get(currentStageIndex) : null;
    }

    public boolean isStageComplete() {
        QuestStage s = getCurrentStage();
        if (s == null) return true;
        for (QuestObjective obj : s.getObjectives()) if (obj.getCurrentCount() < obj.getRequiredCount()) return false;
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

    public String getId() { return questId; }
    public void setId(String id) { this.questId = id; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("questId", questId);
        json.addProperty("status", status.name());
        json.addProperty("currentStageIndex", currentStageIndex);
        json.addProperty("startTimestamp", startTimestamp);
        JsonArray arr = new JsonArray();
        for (QuestStage s : stages) arr.add(s.toJson());
        json.add("stages", arr);
        return json;
    }

    public void fromJson(JsonObject json) {
        questId = json.has("questId") ? json.get("questId").getAsString() : "";
        try { status = QuestStatus.valueOf(json.get("status").getAsString()); }
        catch (Exception e) { status = QuestStatus.ACTIVE; }
        currentStageIndex = json.has("currentStageIndex") ? json.get("currentStageIndex").getAsInt() : 0;
        startTimestamp = json.has("startTimestamp") ? json.get("startTimestamp").getAsLong() : 0;
        stages.clear();
        if (json.has("stages")) {
            for (JsonElement e : json.getAsJsonArray("stages")) {
                QuestStage s = new QuestStage();
                s.fromJson(e.getAsJsonObject());
                stages.add(s);
            }
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("QuestId", questId);
        tag.putString("Status", status.name());
        tag.putInt("CurrentStageIndex", currentStageIndex);
        tag.putLong("StartTimestamp", startTimestamp);
        ListTag list = new ListTag();
        for (QuestStage s : stages) list.add(s.save());
        tag.put("Stages", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        questId = tag.getString("QuestId");
        try { status = QuestStatus.valueOf(tag.getString("Status")); }
        catch (IllegalArgumentException e) { status = QuestStatus.ACTIVE; }
        currentStageIndex = tag.getInt("CurrentStageIndex");
        startTimestamp = tag.getLong("StartTimestamp");
        stages.clear();
        ListTag list = tag.getList("Stages", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            QuestStage s = new QuestStage();
            s.load(list.getCompound(i));
            stages.add(s);
        }
    }

    public static QuestProgress fromTemplate(QuestData template) {
        QuestProgress p = new QuestProgress();
        p.questId = template.getId();
        p.status = QuestStatus.ACTIVE;
        p.currentStageIndex = 0;
        p.startTimestamp = System.currentTimeMillis();
        for (QuestStage s : template.getStages()) p.stages.add(s.copyTemplate());
        return p;
    }
}