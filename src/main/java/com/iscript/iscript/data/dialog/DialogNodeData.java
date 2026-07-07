package com.iscript.iscript.data.dialog;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class DialogNodeData {
    private String id = "";
    private String title = "Node";
    private String text = "Hello!";
    private String portrait = "";
    private String sound = "";
    private int x = 0, y = 0;
    private List<NodeConnection> connections = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getPortrait() { return portrait; }
    public void setPortrait(String portrait) { this.portrait = portrait; }
    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public List<NodeConnection> getConnections() { return connections; }

    public void save(CompoundTag tag) {
        tag.putString("Id", id);
        tag.putString("Title", title);
        tag.putString("Text", text);
        tag.putString("Portrait", portrait);
        tag.putString("Sound", sound);
        tag.putInt("X", x);
        tag.putInt("Y", y);
        ListTag list = new ListTag();
        for (NodeConnection c : connections) {
            CompoundTag t = new CompoundTag();
            c.save(t);
            list.add(t);
        }
        tag.put("Connections", list);
    }

    public void load(CompoundTag tag) {
        id = tag.getString("Id");
        title = tag.getString("Title");
        text = tag.getString("Text");
        portrait = tag.getString("Portrait");
        sound = tag.getString("Sound");
        x = tag.getInt("X");
        y = tag.getInt("Y");
        connections.clear();
        ListTag list = tag.getList("Connections", 10);
        for (int i = 0; i < list.size(); i++) {
            NodeConnection c = new NodeConnection();
            c.load(list.getCompound(i));
            connections.add(c);
        }
    }

    public static class NodeConnection {
        private String optionText = "Continue...";
        private String targetNodeId = "";
        private String conditionScript = "";
        private int sourceSlot = 0;

        public String getOptionText() { return optionText; }
        public void setOptionText(String text) { this.optionText = text; }
        public String getTargetNodeId() { return targetNodeId; }
        public void setTargetNodeId(String id) { this.targetNodeId = id; }
        public String getConditionScript() { return conditionScript; }
        public void setConditionScript(String script) { this.conditionScript = script; }
        public int getSourceSlot() { return sourceSlot; }
        public void setSourceSlot(int slot) { this.sourceSlot = slot; }

        public void save(CompoundTag tag) {
            tag.putString("OptionText", optionText);
            tag.putString("TargetNodeId", targetNodeId);
            tag.putString("ConditionScript", conditionScript);
            tag.putInt("SourceSlot", sourceSlot);
        }

        public void load(CompoundTag tag) {
            optionText = tag.getString("OptionText");
            targetNodeId = tag.getString("TargetNodeId");
            conditionScript = tag.getString("ConditionScript");
            sourceSlot = tag.getInt("SourceSlot");
        }
    }
}