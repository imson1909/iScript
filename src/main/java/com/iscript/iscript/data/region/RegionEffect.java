package com.iscript.iscript.data.region;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import com.iscript.iscript.data.DataObject;

public class RegionEffect implements DataObject {
    private EffectType type = EffectType.NONE;
    private String value = "";
    private int duration = 200;
    private int amplifier;

    public enum EffectType {
        NONE, POTION, COMMAND, MESSAGE, SOUND, TELEPORT, DAMAGE, HEAL, CUTSCENE
    }

    public EffectType getType() { return type; }
    public void setType(EffectType type) { this.type = type; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value != null ? value : ""; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public int getAmplifier() { return amplifier; }
    public void setAmplifier(int amplifier) { this.amplifier = amplifier; }

    public String getId() { return value; }
    public void setId(String id) { this.value = id; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.addProperty("value", value);
        json.addProperty("duration", duration);
        json.addProperty("amplifier", amplifier);
        return json;
    }

    public void fromJson(JsonObject json) {
        try { type = EffectType.valueOf(json.get("type").getAsString()); }
        catch (Exception e) { type = EffectType.NONE; }
        value = json.has("value") ? json.get("value").getAsString() : "";
        duration = json.has("duration") ? json.get("duration").getAsInt() : 200;
        amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
    }


    public CompoundTag save(CompoundTag tag) {
        tag.putString("Type", type.name());
        tag.putString("Value", value);
        tag.putInt("Duration", duration);
        tag.putInt("Amplifier", amplifier);
        return tag;
    }

    public void load(CompoundTag tag) {
        try { type = EffectType.valueOf(tag.getString("Type")); }
        catch (IllegalArgumentException e) { type = EffectType.NONE; }
        value = tag.getString("Value");
        duration = tag.getInt("Duration");
        amplifier = tag.getInt("Amplifier");
    }
}