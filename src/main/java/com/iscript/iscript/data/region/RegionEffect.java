package com.iscript.iscript.data.region;

import net.minecraft.nbt.CompoundTag;

public class RegionEffect {
    private EffectType type = EffectType.NONE;
    private String value = "";
    private int duration = 200;
    private int amplifier = 0;

    public enum EffectType {
        NONE, POTION, COMMAND, MESSAGE, SOUND, TELEPORT, DAMAGE, HEAL
    }

    public EffectType getType() { return type; }
    public void setType(EffectType type) { this.type = type; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public int getAmplifier() { return amplifier; }
    public void setAmplifier(int amplifier) { this.amplifier = amplifier; }

    public void save(CompoundTag tag) {
        tag.putString("Type", type.name());
        tag.putString("Value", value);
        tag.putInt("Duration", duration);
        tag.putInt("Amplifier", amplifier);
    }

    public void load(CompoundTag tag) {
        try {
            type = EffectType.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException e) {
            type = EffectType.NONE;
        }
        value = tag.getString("Value");
        duration = tag.getInt("Duration");
        amplifier = tag.getInt("Amplifier");
    }
}
