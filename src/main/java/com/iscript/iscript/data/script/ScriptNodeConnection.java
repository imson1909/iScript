package com.iscript.iscript.data.script;

import net.minecraft.nbt.CompoundTag;

public class ScriptNodeConnection {
    private String sourceNodeId = "";
    private String targetNodeId = "";
    private int sourceSlot = 0;
    private String conditionValue = "";

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String id) { this.sourceNodeId = id; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String id) { this.targetNodeId = id; }
    public int getSourceSlot() { return sourceSlot; }
    public void setSourceSlot(int slot) { this.sourceSlot = slot; }
    public String getConditionValue() { return conditionValue; }
    public void setConditionValue(String v) { this.conditionValue = v; }

    public void save(CompoundTag tag) {
        tag.putString("SourceNodeId", sourceNodeId);
        tag.putString("TargetNodeId", targetNodeId);
        tag.putInt("SourceSlot", sourceSlot);
        tag.putString("ConditionValue", conditionValue);
    }

    public void load(CompoundTag tag) {
        sourceNodeId = tag.getString("SourceNodeId");
        targetNodeId = tag.getString("TargetNodeId");
        sourceSlot = tag.getInt("SourceSlot");
        conditionValue = tag.getString("ConditionValue");
    }
}