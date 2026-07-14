package com.iscript.iscript.data.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptNodeData {
    private String id = "";
    private ScriptNodeType type = ScriptNodeType.DELAY;
    private int x = 0, y = 0;
    private Map<String, String> params = new HashMap<>();
    private List<ScriptNodeConnection> connections = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id != null ? id : ""; }
    public ScriptNodeType getType() { return type; }
    public void setType(ScriptNodeType type) { this.type = type != null ? type : ScriptNodeType.DELAY; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public Map<String, String> getParams() { return Collections.unmodifiableMap(params); }
    public List<ScriptNodeConnection> getConnections() { return connections; }

    public String getParam(String key) { return params.getOrDefault(key, ""); }
    public String getParamOrDefault(String key, String defaultValue) { return params.getOrDefault(key, defaultValue); }
    public void setParam(String key, String value) {
        if (key != null) params.put(key, value != null ? value : "");
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", id);
        tag.putString("Type", type.name());
        tag.putInt("X", x);
        tag.putInt("Y", y);
        CompoundTag paramsTag = new CompoundTag();
        for (Map.Entry<String, String> e : params.entrySet()) {
            paramsTag.putString(e.getKey(), e.getValue());
        }
        tag.put("Params", paramsTag);
        ListTag conns = new ListTag();
        for (ScriptNodeConnection c : connections) {
            CompoundTag t = new CompoundTag();
            c.save(t);
            conns.add(t);
        }
        tag.put("Connections", conns);
    }

    public void load(CompoundTag tag) {
        id = tag.getString("Id");
        try {
            type = ScriptNodeType.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException e) {
            type = ScriptNodeType.DELAY;
        }
        x = tag.getInt("X");
        y = tag.getInt("Y");
        params.clear();
        CompoundTag paramsTag = tag.getCompound("Params");
        for (String key : paramsTag.getAllKeys()) {
            params.put(key, paramsTag.getString(key));
        }
        connections.clear();
        ListTag conns = tag.getList("Connections", 10);
        for (int i = 0; i < conns.size(); i++) {
            ScriptNodeConnection c = new ScriptNodeConnection();
            c.load(conns.getCompound(i));
            connections.add(c);
        }
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("type", type.name());
        obj.addProperty("x", x);
        obj.addProperty("y", y);
        JsonObject paramsObj = new JsonObject();
        for (Map.Entry<String, String> e : params.entrySet()) {
            paramsObj.addProperty(e.getKey(), e.getValue());
        }
        obj.add("params", paramsObj);
        JsonArray conns = new JsonArray();
        for (ScriptNodeConnection c : connections) {
            conns.add(c.toJson());
        }
        obj.add("connections", conns);
        return obj;
    }

    public void fromJson(JsonObject obj) {
        id = obj.has("id") ? obj.get("id").getAsString() : "";
        try {
            type = ScriptNodeType.valueOf(obj.get("type").getAsString());
        } catch (Exception e) {
            type = ScriptNodeType.DELAY;
        }
        x = obj.has("x") ? obj.get("x").getAsInt() : 0;
        y = obj.has("y") ? obj.get("y").getAsInt() : 0;
        params.clear();
        if (obj.has("params")) {
            JsonObject pObj = obj.getAsJsonObject("params");
            for (Map.Entry<String, com.google.gson.JsonElement> e : pObj.entrySet()) {
                params.put(e.getKey(), e.getValue().getAsString());
            }
        }
        connections.clear();
        if (obj.has("connections")) {
            for (com.google.gson.JsonElement e : obj.getAsJsonArray("connections")) {
                ScriptNodeConnection c = new ScriptNodeConnection();
                c.fromJson(e.getAsJsonObject());
                connections.add(c);
            }
        }
    }
}