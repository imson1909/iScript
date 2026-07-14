package com.iscript.iscript.data.script;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;

public class ScriptNodeConnection {
    private String targetNodeId = "";
    private String condition = "";
    private String sourceNodeId = "";
    private int sourceSlot = 0;
    private String conditionValue = "";

    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId != null ? targetNodeId : ""; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition != null ? condition : ""; }

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId != null ? sourceNodeId : ""; }
    public int getSourceSlot() { return sourceSlot; }
    public void setSourceSlot(int sourceSlot) { this.sourceSlot = sourceSlot; }
    public String getConditionValue() { return conditionValue; }
    public void setConditionValue(String conditionValue) { this.conditionValue = conditionValue != null ? conditionValue : ""; }

    public void save(CompoundTag tag) {
        tag.putString("Target", targetNodeId);
        tag.putString("Condition", condition);
        tag.putString("SourceNodeId", sourceNodeId);
        tag.putInt("SourceSlot", sourceSlot);
        tag.putString("ConditionValue", conditionValue);
    }

    public void load(CompoundTag tag) {
        targetNodeId = tag.getString("Target");
        condition = tag.getString("Condition");
        sourceNodeId = tag.getString("SourceNodeId");
        sourceSlot = tag.getInt("SourceSlot");
        conditionValue = tag.getString("ConditionValue");
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("targetNodeId", targetNodeId);
        obj.addProperty("condition", condition);
        obj.addProperty("sourceNodeId", sourceNodeId);
        obj.addProperty("sourceSlot", sourceSlot);
        obj.addProperty("conditionValue", conditionValue);
        return obj;
    }

    public void fromJson(JsonObject obj) {
        targetNodeId = obj.has("targetNodeId") ? obj.get("targetNodeId").getAsString() : "";
        condition = obj.has("condition") ? obj.get("condition").getAsString() : "";
        sourceNodeId = obj.has("sourceNodeId") ? obj.get("sourceNodeId").getAsString() : "";
        sourceSlot = obj.has("sourceSlot") ? obj.get("sourceSlot").getAsInt() : 0;
        conditionValue = obj.has("conditionValue") ? obj.get("conditionValue").getAsString() : "";
    }
}