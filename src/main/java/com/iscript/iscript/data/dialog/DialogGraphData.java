package com.iscript.iscript.data.dialog;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.HashMap;
import java.util.Map;

public class DialogGraphData {
    private String id = "";
    private String name = "";
    private String startNodeId = "";
    private final Map<String, DialogNodeData> nodes = new HashMap<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String startNodeId) { this.startNodeId = startNodeId; }
    public Map<String, DialogNodeData> getNodes() { return nodes; }
    public DialogNodeData getNode(String id) { return nodes.get(id); }
    public void addNode(DialogNodeData node) { nodes.put(node.getId(), node); }
    public void removeNode(String id) { nodes.remove(id); }

    public void load(CompoundTag tag) {
        this.id = tag.getString("Id");
        this.name = tag.getString("Name");
        this.startNodeId = tag.getString("StartNodeId");
        this.nodes.clear();
        ListTag list = tag.getList("Nodes", 10);
        for (int i = 0; i < list.size(); i++) {
            DialogNodeData node = new DialogNodeData();
            node.load(list.getCompound(i));
            this.nodes.put(node.getId(), node);
        }
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", this.id);
        tag.putString("Name", this.name);
        tag.putString("StartNodeId", this.startNodeId);
        ListTag list = new ListTag();
        for (DialogNodeData node : this.nodes.values()) {
            CompoundTag t = new CompoundTag();
            node.save(t);
            list.add(t);
        }
        tag.put("Nodes", list);
    }

    public DialogGraphData copy() {
        DialogGraphData copy = new DialogGraphData();
        copy.id = this.id;
        copy.name = this.name;
        copy.startNodeId = this.startNodeId;
        for (DialogNodeData node : this.nodes.values()) {
            copy.nodes.put(node.getId(), node.copy());
        }
        return copy;
    }
}