package com.iscript.iscript.api.triggers;

import com.google.gson.JsonObject;

public class TriggerData {
    private String id = "";
    private TriggerType type = TriggerType.PLAYER_TICK;
    private String scriptId = "";
    private String functionName = "";
    private boolean enabled = true;

    public TriggerData() {}

    public TriggerData(String id, TriggerType type, String scriptId, String functionName) {
        this.id = id;
        this.type = type;
        this.scriptId = scriptId;
        this.functionName = functionName;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("type", type.name());
        obj.addProperty("scriptId", scriptId);
        obj.addProperty("functionName", functionName);
        obj.addProperty("enabled", enabled);
        return obj;
    }

    public void fromJson(JsonObject obj) {
        if (obj.has("id")) this.id = obj.get("id").getAsString();
        if (obj.has("type")) this.type = TriggerType.valueOf(obj.get("type").getAsString());
        if (obj.has("scriptId")) this.scriptId = obj.get("scriptId").getAsString();
        if (obj.has("functionName")) this.functionName = obj.get("functionName").getAsString();
        if (obj.has("enabled")) this.enabled = obj.get("enabled").getAsBoolean();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public TriggerType getType() { return type; }
    public void setType(TriggerType type) { this.type = type; }

    public String getScriptId() { return scriptId; }
    public void setScriptId(String scriptId) { this.scriptId = scriptId; }

    public String getFunctionName() { return functionName; }
    public void setFunctionName(String functionName) { this.functionName = functionName; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}