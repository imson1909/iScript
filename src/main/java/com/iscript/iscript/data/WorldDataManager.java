package com.iscript.iscript.data;

import net.minecraft.server.level.ServerLevel;

import java.util.Set;

public class WorldDataManager {
    public static void setString(ServerLevel level, String key, String value) { WorldData.get(level).setString(key, value); }
    public static String getString(ServerLevel level, String key) { return WorldData.get(level).getString(key); }
    public static void setInt(ServerLevel level, String key, int value) { WorldData.get(level).setInt(key, value); }
    public static int getInt(ServerLevel level, String key) { return WorldData.get(level).getInt(key); }
    public static void setDouble(ServerLevel level, String key, double value) { WorldData.get(level).setDouble(key, value); }
    public static double getDouble(ServerLevel level, String key) { return WorldData.get(level).getDouble(key); }
    public static void setBool(ServerLevel level, String key, boolean value) { WorldData.get(level).setBool(key, value); }
    public static boolean getBool(ServerLevel level, String key) { return WorldData.get(level).getBool(key); }
    public static void remove(ServerLevel level, String key) { WorldData.get(level).remove(key); }
    public static String getType(ServerLevel level, String key) { return WorldData.get(level).getType(key); }
    public static Set<String> getKeys(ServerLevel level) { return WorldData.get(level).getAllKeys(); }
}