package com.iscript.iscript.api.states;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.*;

public class States {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public void setNumber(String id, double value) {
        if (Double.isNaN(value)) return;
        values.put(id, value);
    }

    public void setString(String id, String value) {
        values.put(id, value);
    }

    public void add(String id, double delta) {
        Object prev = values.get(id);
        double base = (prev instanceof Number) ? ((Number) prev).doubleValue() : 0;
        values.put(id, base + delta);
    }

    public double getNumber(String id) {
        Object o = values.get(id);
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    public String getString(String id) {
        Object o = values.get(id);
        return o instanceof String ? (String) o : "";
    }

    public boolean isNumber(String id) {
        return values.get(id) instanceof Number;
    }

    public boolean isString(String id) {
        return values.get(id) instanceof String;
    }

    public void setBoolean(String id, boolean value) {
        values.put(id, value ? 1.0 : 0.0);
    }

    public boolean getBoolean(String id) {
        Object o = values.get(id);
        if (o instanceof Number n) return n.doubleValue() != 0;
        if (o instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }

    public boolean isBoolean(String id) {
        Object o = values.get(id);
        return o instanceof Number || (o instanceof String && (((String) o).equalsIgnoreCase("true") || ((String) o).equalsIgnoreCase("false")));
    }

    public boolean has(String id) {
        return values.containsKey(id);
    }

    public boolean remove(String id) {
        return values.remove(id) != null;
    }

    public void clear() {
        values.clear();
    }

    public void copyFrom(States other) {
        values.clear();
        values.putAll(other.values);
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<String, Object> e : values.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("k", e.getKey());
            if (e.getValue() instanceof Number n) {
                entry.putString("t", "n");
                entry.putDouble("v", n.doubleValue());
            } else {
                entry.putString("t", "s");
                entry.putString("v", e.getValue().toString());
            }
            list.add(entry);
        }
        tag.put("entries", list);
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        values.clear();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String key = entry.getString("k");
            String type = entry.getString("t");
            if ("n".equals(type)) {
                values.put(key, entry.getDouble("v"));
            } else {
                values.put(key, entry.getString("v"));
            }
        }
    }
}