package com.iscript.iscript.data.script;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptGraphData {
    private String id = "";
    private String name = "Script Graph";
    private Map<String, ScriptNodeData> nodes = new HashMap<>();
    private String startNodeId = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, ScriptNodeData> getNodes() { return nodes; }
    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String id) { this.startNodeId = id; }

    public ScriptNodeData getNode(String nodeId) { return nodes.get(nodeId); }

    public void addNode(ScriptNodeData node) {
        nodes.put(node.getId(), node);
        if (startNodeId.isEmpty() && node.getType() == ScriptNodeType.START) {
            startNodeId = node.getId();
        }
    }

    public void removeNode(String nodeId) {
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
        startNodeId = tag.getString("StartNodeId");
        nodes.clear();
        ListTag list = tag.getList("Nodes", 10);
        for (int i = 0; i < list.size(); i++) {
            ScriptNodeData node = new ScriptNodeData();
            node.load(list.getCompound(i));
            nodes.put(node.getId(), node);
        }
    }
}