package com.iscript.iscript.data.dialog;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.HashMap;
import java.util.Map;

public class DialogGraphData {
    private String id = "";
    private String name = "Dialog Graph";
    private String startNodeId = "";
    private Map<String, DialogNodeData> nodes = new HashMap<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String id) { this.startNodeId = id; }
    public Map<String, DialogNodeData> getNodes() { return nodes; }

    public DialogNodeData getNode(String nodeId) { return nodes.get(nodeId); }

    public void addNode(DialogNodeData node) {
        nodes.put(node.getId(), node);
        if (startNodeId.isEmpty()) startNodeId = node.getId();
    }

    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
        if (startNodeId.equals(nodeId)) {
            startNodeId = nodes.isEmpty() ? "" : nodes.keySet().iterator().next();
        }
        for (DialogNodeData node : nodes.values()) {
            node.getConnections().removeIf(c -> c.getTargetNodeId().equals(nodeId));
        }
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", id);
        tag.putString("Name", name);
        tag.putString("StartNodeId", startNodeId);
        ListTag list = new ListTag();
        for (DialogNodeData node : nodes.values()) {
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
            DialogNodeData node = new DialogNodeData();
            node.load(list.getCompound(i));
            nodes.put(node.getId(), node);
        }
    }
}