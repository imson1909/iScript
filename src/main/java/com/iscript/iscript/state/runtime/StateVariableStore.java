package com.iscript.iscript.state.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class StateVariableStore {
    private static final String DATA_NAME = "iscript_variables";
    private static final Map<Player, CompoundTag> playerCache = new HashMap<>();

    public static Object getGlobal(ServerLevel level, String key) {
        VariableData data = VariableData.get(level);
        return data.get(key);
    }

    public static void setGlobal(ServerLevel level, String key, Object value) {
        VariableData data = VariableData.get(level);
        data.set(key, value);
        data.setDirty();
    }

    public static Object getPlayer(Player player, String key) {
        CompoundTag tag = player.getPersistentData().getCompound("iscript_vars");
        if (tag.contains(key, 3)) return tag.getInt(key);
        if (tag.contains(key, 6)) return tag.getDouble(key);
        if (tag.contains(key, 1)) return tag.getBoolean(key);
        return tag.getString(key);
    }

    public static void setPlayer(Player player, String key, Object value) {
        CompoundTag root = player.getPersistentData();
        CompoundTag vars = root.getCompound("iscript_vars");
        if (value instanceof Integer) vars.putInt(key, (Integer) value);
        else if (value instanceof Double) vars.putDouble(key, (Double) value);
        else if (value instanceof Boolean) vars.putBoolean(key, (Boolean) value);
        else vars.putString(key, value.toString());
        root.put("iscript_vars", vars);
    }

    public static class VariableData extends SavedData {
        private final CompoundTag values = new CompoundTag();

        public static VariableData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    new Factory<>(VariableData::new, VariableData::load, null),
                    DATA_NAME
            );
        }

        public Object get(String key) {
            if (values.contains(key, 3)) return values.getInt(key);
            if (values.contains(key, 6)) return values.getDouble(key);
            if (values.contains(key, 1)) return values.getBoolean(key);
            return values.getString(key);
        }

        public void set(String key, Object value) {
            if (value instanceof Integer) values.putInt(key, (Integer) value);
            else if (value instanceof Double) values.putDouble(key, (Double) value);
            else if (value instanceof Boolean) values.putBoolean(key, (Boolean) value);
            else values.putString(key, value.toString());
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.merge(values);
            return tag;
        }

        public static VariableData load(CompoundTag tag) {
            VariableData data = new VariableData();
            data.values.merge(tag);
            return data;
        }
    }
}