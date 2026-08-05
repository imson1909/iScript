package com.iscript.imson.data.region;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import com.iscript.imson.data.DataObject;

public class RegionTrigger implements DataObject {
    public enum TriggerType { ENTER, EXIT, TICK }

    private TriggerType triggerType = TriggerType.ENTER;
    private RegionEffect effect = new RegionEffect();

    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType type) { this.triggerType = type; }
    public RegionEffect getEffect() { return effect; }
    public void setEffect(RegionEffect effect) { this.effect = effect; }
    public String getId() { return effect.getId(); }
    public void setId(String id) { effect.setId(id); }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("triggerType", triggerType.name());
        json.add("effect", effect.toJson());
        return json;
    }

    public void fromJson(JsonObject json) {
        try { triggerType = TriggerType.valueOf(json.get("triggerType").getAsString()); }
        catch (Exception e) { triggerType = TriggerType.ENTER; }
        if (json.has("effect") && json.get("effect").isJsonObject()) {
            effect.fromJson(json.getAsJsonObject("effect"));
        }
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putString("TriggerType", triggerType.name());
        tag.put("Effect", effect.save(new CompoundTag()));
        return tag;
    }

    public void load(CompoundTag tag) {
        try { triggerType = TriggerType.valueOf(tag.getString("TriggerType")); }
        catch (IllegalArgumentException e) { triggerType = TriggerType.ENTER; }
        effect.load(tag.getCompound("Effect"));
    }
}