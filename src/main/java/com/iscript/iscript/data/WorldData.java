package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorldData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_world";
    private final Map<String, String> stringData = new HashMap<>();
    private final Map<String, Integer> intData = new HashMap<>();
    private final Map<String, Double> doubleData = new HashMap<>();
    private final Map<String, Boolean> boolData = new HashMap<>();

    public void setString(String key, String value) { stringData.put(key, value); setDirty(); }
    public String getString(String key) { return stringData.getOrDefault(key, ""); }
    public void setInt(String key, int value) { intData.put(key, value); setDirty(); }
    public int getInt(String key) { return intData.getOrDefault(key, 0); }
    public void setDouble(String key, double value) { doubleData.put(key, value); setDirty(); }
    public double getDouble(String key) { return doubleData.getOrDefault(key, 0.0); }
    public void setBool(String key, boolean value) { boolData.put(key, value); setDirty(); }
    public boolean getBool(String key) { return boolData.getOrDefault(key, false); }

    public void remove(String key) {
        stringData.remove(key);
        intData.remove(key);
        doubleData.remove(key);
        boolData.remove(key);
        setDirty();
    }

    public boolean hasKey(String key) {
        return stringData.containsKey(key) || intData.containsKey(key) || doubleData.containsKey(key) || boolData.containsKey(key);
    }

    public String getType(String key) {
        if (stringData.containsKey(key)) return "string";
        if (intData.containsKey(key)) return "int";
        if (doubleData.containsKey(key)) return "double";
        if (boolData.containsKey(key)) return "bool";
        return "none";
    }

    public Set<String> getAllKeys() {
        Set<String> keys = new java.util.HashSet<>();
        keys.addAll(stringData.keySet());
        keys.addAll(intData.keySet());
        keys.addAll(doubleData.keySet());
        keys.addAll(boolData.keySet());
        return keys;
    }

    public static WorldData load(CompoundTag tag) {
        WorldData data = new WorldData();
        CompoundTag strings = tag.getCompound("Strings");
        for (String key : strings.getAllKeys()) data.stringData.put(key, strings.getString(key));
        CompoundTag ints = tag.getCompound("Ints");
        for (String key : ints.getAllKeys()) data.intData.put(key, ints.getInt(key));
        CompoundTag doubles = tag.getCompound("Doubles");
        for (String key : doubles.getAllKeys()) data.doubleData.put(key, doubles.getDouble(key));
        CompoundTag bools = tag.getCompound("Bools");
        for (String key : bools.getAllKeys()) data.boolData.put(key, bools.getBoolean(key));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
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
        return tag;
    }

    public static WorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WorldData::load, WorldData::new, DATA_NAME);
    }
}