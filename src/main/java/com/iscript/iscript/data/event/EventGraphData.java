package com.iscript.iscript.data.event;

import com.iscript.iscript.data.script.ScriptNodeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class EventGraphData {
    private String id = "";
    private String name = "";
    private final Map<String, ScriptNodeData> nodes = new LinkedHashMap<>();
    private String startNodeId = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id != null ? id : ""; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name : ""; }
    public Map<String, ScriptNodeData> getNodes() { return Collections.unmodifiableMap(nodes); }
    public ScriptNodeData getNode(String id) { return nodes.get(id); }
    public void addNode(ScriptNodeData node) { if (node != null && node.getId() != null && !node.getId().isEmpty()) nodes.put(node.getId(), node); }
    public void removeNode(String id) { nodes.remove(id); }
    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String id) { this.startNodeId = id != null ? id : ""; }

    public void save(CompoundTag tag) {
        tag.putString("Id", id);
        tag.putString("Name", name);
        tag.putString("StartNodeId", startNodeId);
        ListTag list = new ListTag();
        for (ScriptNodeData node : nodes.values()) {
            if (node == null) continue;
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
            try {
                ScriptNodeData node = new ScriptNodeData();
                node.load(list.getCompound(i));
                if (node.getId() != null && !node.getId().isEmpty()) {
                    nodes.put(node.getId(), node);
                }
            } catch (Exception e) {
            }
        }
    }

    public EventGraphData copy() {
        EventGraphData copy = new EventGraphData();
        copy.id = this.id + "_copy";
        copy.name = this.name + " (Copy)";
        copy.startNodeId = this.startNodeId;
        for (ScriptNodeData node : this.nodes.values()) {
            if (node == null) continue;
            CompoundTag tag = new CompoundTag();
            node.save(tag);
            ScriptNodeData n = new ScriptNodeData();
            n.load(tag);
            copy.nodes.put(n.getId(), n);
        }
        return copy;
    }
}