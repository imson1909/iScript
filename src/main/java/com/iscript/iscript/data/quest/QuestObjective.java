package com.iscript.iscript.data.quest;

import com.google.gson.JsonObject;
import com.iscript.iscript.data.DataObject;
import net.minecraft.nbt.CompoundTag;

public class QuestObjective implements DataObject {
    private QuestObjectiveType type = QuestObjectiveType.NONE;
    private String target = "";
    private int requiredCount = 1;
    private int currentCount;
    private String description = "";

    public QuestObjectiveType getType() { return type; }
    public void setType(QuestObjectiveType type) { this.type = type; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public int getRequiredCount() { return requiredCount; }
    public void setRequiredCount(int c) { this.requiredCount = c; }
    public int getCurrentCount() { return currentCount; }
    public void setCurrentCount(int c) { this.currentCount = c; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }

    public String getId() { return target; }
    public void setId(String id) { this.target = id; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.addProperty("target", target);
        json.addProperty("requiredCount", requiredCount);
        json.addProperty("currentCount", currentCount);
        json.addProperty("description", description);
        return json;
    }

    public void fromJson(JsonObject json) {
        try { type = QuestObjectiveType.valueOf(json.get("type").getAsString()); }
        catch (Exception e) { type = QuestObjectiveType.NONE; }
        target = json.has("target") ? json.get("target").getAsString() : "";
        requiredCount = json.has("requiredCount") ? json.get("requiredCount").getAsInt() : 1;
        currentCount = json.has("currentCount") ? json.get("currentCount").getAsInt() : 0;
        description = json.has("description") ? json.get("description").getAsString() : "";
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putString("Target", target);
        tag.putInt("RequiredCount", requiredCount);
        tag.putInt("CurrentCount", currentCount);
        tag.putString("Description", description);
        return tag;
    }

    public void load(CompoundTag tag) {
        try { type = QuestObjectiveType.valueOf(tag.getString("Type")); }
        catch (IllegalArgumentException e) { type = QuestObjectiveType.NONE; }
        target = tag.getString("Target");
        requiredCount = tag.getInt("RequiredCount");
        currentCount = tag.getInt("CurrentCount");
        description = tag.getString("Description");
    }

    public QuestObjective copy() {
        QuestObjective o = new QuestObjective();
        o.fromJson(this.toJson());
        return o;
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