package com.iscript.iscript.data.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScriptGraphData {
    private String id = "";
    private String name = "Script Graph";
    private Map<String, ScriptNodeData> nodes = new HashMap<>();
    private String startNodeId = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id != null ? id : ""; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name != null && !name.isEmpty() ? name : "Script Graph"; }
    public Map<String, ScriptNodeData> getNodes() { return Collections.unmodifiableMap(nodes); }
    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String id) { this.startNodeId = id != null ? id : ""; }

    public ScriptNodeData getNode(String nodeId) { return nodes.get(nodeId); }

    public void addNode(ScriptNodeData node) {
        if (node == null || node.getId() == null || node.getId().isEmpty()) return;
        nodes.put(node.getId(), node);
        if (startNodeId.isEmpty() && node.getType() == ScriptNodeType.START) {
            startNodeId = node.getId();
        }
    }

    public void removeNode(String nodeId) {
        if (nodeId == null) return;
        nodes.remove(nodeId);
        if (startNodeId.equals(nodeId)) {
            startNodeId = "";
            for (ScriptNodeData n : nodes.values()) {
                if (n.getType() == ScriptNodeType.START) {
                    startNodeId = n.getId();
                    break;
                }
            }
        }
        for (ScriptNodeData n : nodes.values()) {
            n.getConnections().removeIf(c -> c.getTargetNodeId().equals(nodeId));
        }
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", id);
        tag.putString("Name", name);
        tag.putString("StartNodeId", startNodeId);
        ListTag list = new ListTag();
        for (ScriptNodeData node : nodes.values()) {
            CompoundTag t = new CompoundTag();
            node.save(t);
            list.add(t);
        }
        tag.put("Nodes", list);
    }

    public void load(CompoundTag tag) {
        id = tag.getString("Id");
        name = tag.getString("Name");
        if (name.isEmpty()) name = "Script Graph";
        startNodeId = tag.getString("StartNodeId");
        nodes.clear();
        ListTag list = tag.getList("Nodes", 10);
        for (int i = 0; i < list.size(); i++) {
            ScriptNodeData node = new ScriptNodeData();
            node.load(list.getCompound(i));
            if (node.getId() != null && !node.getId().isEmpty()) {
                nodes.put(node.getId(), node);
            }
        }
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("name", name);
        obj.addProperty("startNodeId", startNodeId);
        JsonArray nodesArr = new JsonArray();
        for (ScriptNodeData node : nodes.values()) {
            nodesArr.add(node.toJson());
        }
        obj.add("nodes", nodesArr);
        return obj;
    }

    public void fromJson(JsonObject obj) {
        id = obj.has("id") ? obj.get("id").getAsString() : "";
        name = obj.has("name") ? obj.get("name").getAsString() : "Script Graph";
        startNodeId = obj.has("startNodeId") ? obj.get("startNodeId").getAsString() : "";
        nodes.clear();
        if (obj.has("nodes")) {
            for (com.google.gson.JsonElement e : obj.getAsJsonArray("nodes")) {
                ScriptNodeData node = new ScriptNodeData();
                node.fromJson(e.getAsJsonObject());
                if (node.getId() != null && !node.getId().isEmpty()) {
                    nodes.put(node.getId(), node);
                }
            }
        }
    }
}