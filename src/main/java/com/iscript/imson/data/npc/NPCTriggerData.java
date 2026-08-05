package com.iscript.imson.data.npc;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class NPCTriggerData {
    public enum TriggerType { INTERACT, TICK, WALK, SPAWN, HURT }
    public enum ActionType { SCRIPT, SOUND, COMMAND, DIALOG, TRADE, EVENT }

    private TriggerType triggerType = TriggerType.INTERACT;
    private ActionType actionType = ActionType.SCRIPT;
    private String actionValue = "";
    private boolean enabled = true;

    public NPCTriggerData() {}

    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType t) { this.triggerType = t; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType t) { this.actionType = t; }
    public String getActionValue() { return actionValue; }
    public void setActionValue(String v) { this.actionValue = v != null ? v : ""; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("triggerType", triggerType.name());
        json.addProperty("actionType", actionType.name());
        json.addProperty("actionValue", actionValue);
        json.addProperty("enabled", enabled);
        return json;
    }

    public void fromJson(JsonObject json) {
        try { triggerType = TriggerType.valueOf(json.get("triggerType").getAsString()); } catch (Exception e) { triggerType = TriggerType.INTERACT; }
        try { actionType = ActionType.valueOf(json.get("actionType").getAsString()); } catch (Exception e) { actionType = ActionType.SCRIPT; }
        actionValue = json.has("actionValue") ? json.get("actionValue").getAsString() : "";
        enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("TriggerType", triggerType.name());
        tag.putString("ActionType", actionType.name());
        tag.putString("ActionValue", actionValue);
        tag.putBoolean("Enabled", enabled);
        return tag;
    }

    public void load(CompoundTag tag) {
        try { triggerType = TriggerType.valueOf(tag.getString("TriggerType")); } catch (Exception e) { triggerType = TriggerType.INTERACT; }
        try { actionType = ActionType.valueOf(tag.getString("ActionType")); } catch (Exception e) { actionType = ActionType.SCRIPT; }
        actionValue = tag.getString("ActionValue");
        enabled = tag.getBoolean("Enabled");
    }

    public static ListTag saveList(List<NPCTriggerData> list) {
        ListTag tags = new ListTag();
        for (NPCTriggerData t : list) tags.add(t.save());
        return tags;
    }

    public static List<NPCTriggerData> loadList(ListTag tags) {
        List<NPCTriggerData> list = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            NPCTriggerData t = new NPCTriggerData();
            t.load(tags.getCompound(i));
            list.add(t);
        }
        return list;
    }
}