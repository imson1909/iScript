package com.iscript.imson.script.api;

import com.iscript.imson.data.GlobalStates;
import com.iscript.imson.data.ModData;
import org.graalvm.polyglot.HostAccess;

public class StateAPI {
    private final ScriptAPI root;

    public StateAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public void set(String key, Object value) {
        if (value instanceof Number n) {
            GlobalStates.get().setNumber(key, n.doubleValue());
        } else {
            GlobalStates.get().setString(key, value.toString());
        }
        GlobalStates.save();
        ModData.setDirty();
    }

    @HostAccess.Export
    public String getString(String key) {
        return GlobalStates.get().getString(key);
    }

    @HostAccess.Export
    public double getNumber(String key) {
        return GlobalStates.get().getNumber(key);
    }

    @HostAccess.Export
    public boolean getBool(String key) {
        return Boolean.parseBoolean(GlobalStates.get().getString(key));
    }

    @HostAccess.Export
    public int getInt(String key) {
        return (int) GlobalStates.get().getNumber(key);
    }

    @HostAccess.Export
    public double getDouble(String key) {
        return GlobalStates.get().getNumber(key);
    }

    @HostAccess.Export
    public boolean has(String key) {
        return GlobalStates.get().has(key);
    }

    @HostAccess.Export
    public void add(String key, double delta) {
        GlobalStates.get().add(key, delta);
        GlobalStates.save();
        ModData.setDirty();
    }

    @HostAccess.Export
    public void increment(String key, double delta) {
        add(key, delta);
    }

    @HostAccess.Export
    public void remove(String key) {
        GlobalStates.get().remove(key);
        GlobalStates.save();
        ModData.setDirty();
    }

    @HostAccess.Export
    public String toString(String key) {
        return getString(key);
    }

    @HostAccess.Export
    public double toNumber(String key) {
        return getNumber(key);
    }

    @HostAccess.Export
    public boolean toBool(String key) {
        return getBool(key);
    }

    @HostAccess.Export
    public int toInt(String key) {
        return getInt(key);
    }

    @HostAccess.Export
    public double toDouble(String key) {
        return getDouble(key);
    }

    @HostAccess.Export
    public int length(String key) {
        return getString(key).length();
    }
}