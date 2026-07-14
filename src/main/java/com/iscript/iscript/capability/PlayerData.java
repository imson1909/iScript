package com.iscript.iscript.capability;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class PlayerData {
    private final Map<String, String> stringData = new HashMap<>();
    private final Map<String, Integer> intData = new HashMap<>();
    private final Map<String, Double> doubleData = new HashMap<>();
    private final Map<String, Boolean> boolData = new HashMap<>();
    private String faction = "neutral";
    private int reputation = 0;

    public void setString(String key, String value) { stringData.put(key, value); }
    public String getString(String key) { return stringData.getOrDefault(key, ""); }
    public void setInt(String key, int value) { intData.put(key, value); }
    public int getInt(String key) { return intData.getOrDefault(key, 0); }
    public void setDouble(String key, double value) { doubleData.put(key, value); }
    public double getDouble(String key) { return doubleData.getOrDefault(key, 0.0); }
    public void setBool(String key, boolean value) { boolData.put(key, value); }
    public boolean getBool(String key) { return boolData.getOrDefault(key, false); }

    public String getFaction() { return faction; }
    public void setFaction(String faction) { this.faction = faction; }
    public int getReputation() { return reputation; }
    public void setReputation(int reputation) { this.reputation = reputation; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag strings = new CompoundTag();
        stringData.forEach(strings::putString);
        tag.put("Strings", strings);
        CompoundTag ints = new CompoundTag();
        intData.forEach(ints::putInt);
        tag.put("Ints", ints);
        CompoundTag doubles = new CompoundTag();
        doubleData.forEach(doubles::putDouble);
        tag.put("Doubles", doubles);
        CompoundTag bools = new CompoundTag();
        boolData.forEach(bools::putBoolean);
        tag.put("Bools", bools);
        tag.putString("Faction", faction);
        tag.putInt("Reputation", reputation);
        return tag;
    }

    public void load(CompoundTag tag) {
        stringData.clear();
        intData.clear();
        doubleData.clear();
        boolData.clear();
        CompoundTag strings = tag.getCompound("Strings");
        for (String key : strings.getAllKeys()) stringData.put(key, strings.getString(key));
        CompoundTag ints = tag.getCompound("Ints");
        for (String key : ints.getAllKeys()) intData.put(key, ints.getInt(key));
        CompoundTag doubles = tag.getCompound("Doubles");
        for (String key : doubles.getAllKeys()) doubleData.put(key, doubles.getDouble(key));
        CompoundTag bools = tag.getCompound("Bools");
        for (String key : bools.getAllKeys()) boolData.put(key, bools.getBoolean(key));
        faction = tag.getString("Faction");
        reputation = tag.getInt("Reputation");
    }
}