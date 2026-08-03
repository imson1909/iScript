package com.iscript.iscript.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;

public class Node implements DataObject {
    private String id = "";
    private String name = "";
    private String type = "";
    private int x, y;
    private final Map<String, String> params = new HashMap<>();
    private final List<Connection> connections = new ArrayList<>();
    private final Class<? extends Enum<?>> typeEnum;

    public Node() { this(null); }
    public Node(Class<? extends Enum<?>> typeEnum) { this.typeEnum = typeEnum; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id != null ? id : ""; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name : ""; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type != null ? type : ""; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public Map<String, String> getParams() { return Collections.unmodifiableMap(params); }
    public String getParam(String key) { return params.getOrDefault(key, ""); }
    public String getParamOrDefault(String key, String def) { return params.getOrDefault(key, def); }
    public void setParam(String key, String value) { if (key != null) params.put(key, value != null ? value : ""); }
    public List<Connection> getConnections() { return connections; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("type", type);
        json.addProperty("x", x);
        json.addProperty("y", y);
        JsonObject p = new JsonObject();
        params.forEach(p::addProperty);
        json.add("params", p);
        JsonArray arr = new JsonArray();
        for (Connection c : connections) arr.add(c.toJson());
        json.add("connections", arr);
        return json;
    }

    public void fromJson(JsonObject json) {
        id = json.has("id") ? json.get("id").getAsString() : "";
        name = json.has("name") ? json.get("name").getAsString() : "";
        type = json.has("type") ? json.get("type").getAsString() : "";
        if (type.isEmpty() && typeEnum != null) type = typeEnum.getEnumConstants()[0].name();
        x = json.has("x") ? json.get("x").getAsInt() : 0;
        y = json.has("y") ? json.get("y").getAsInt() : 0;
        params.clear();
        if (json.has("params")) {
            JsonObject p = json.getAsJsonObject("params");
            for (Map.Entry<String, JsonElement> e : p.entrySet()) params.put(e.getKey(), e.getValue().getAsString());
        }
        connections.clear();
        if (json.has("connections")) {
            JsonArray arr = json.getAsJsonArray("connections");
            for (JsonElement e : arr) {
                Connection c = new Connection();
                c.fromJson(e.getAsJsonObject());
                connections.add(c);
            }
        }
    }

    public static class Connection {
        private String target = "";
        private String condition = "";
        private String sourceNode = "";
        private int sourceSlot;
        private String conditionValue = "";

        public String getTarget() { return target; }
        public void setTarget(String t) { this.target = t != null ? t : ""; }
        public String getCondition() { return condition; }
        public void setCondition(String c) { this.condition = c != null ? c : ""; }
        public String getSourceNode() { return sourceNode; }
        public void setSourceNode(String s) { this.sourceNode = s != null ? s : ""; }
        public int getSourceSlot() { return sourceSlot; }
        public void setSourceSlot(int s) { this.sourceSlot = s; }
        public String getConditionValue() { return conditionValue; }
        public void setConditionValue(String v) { this.conditionValue = v != null ? v : ""; }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("target", target);
            json.addProperty("condition", condition);
            json.addProperty("sourceNode", sourceNode);
            json.addProperty("sourceSlot", sourceSlot);
            json.addProperty("conditionValue", conditionValue);
            return json;
        }

        public void fromJson(JsonObject json) {
            target = json.has("target") ? json.get("target").getAsString() : "";
            condition = json.has("condition") ? json.get("condition").getAsString() : "";
            sourceNode = json.has("sourceNode") ? json.get("sourceNode").getAsString() : "";
            sourceSlot = json.has("sourceSlot") ? json.get("sourceSlot").getAsInt() : 0;
            conditionValue = json.has("conditionValue") ? json.get("conditionValue").getAsString() : "";
        }
    }
}