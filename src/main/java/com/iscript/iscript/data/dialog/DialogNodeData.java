package com.iscript.iscript.data.dialog;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class DialogNodeData {
    private String id = "";
    private String title = "";
    private String text = "";
    private String portrait = "";
    private String sound = "";
    private int x = 0;
    private int y = 0;
    private final List<NodeConnection> connections = new ArrayList<>();

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

    public void load(CompoundTag tag) {
        this.id = tag.getString("Id");
        this.title = tag.getString("Title");
        this.text = tag.getString("Text");
        this.portrait = tag.getString("Portrait");
        this.sound = tag.getString("Sound");
        this.x = tag.getInt("X");
        this.y = tag.getInt("Y");
        this.connections.clear();
        ListTag list = tag.getList("Connections", 10);
        for (int i = 0; i < list.size(); i++) {
            NodeConnection conn = new NodeConnection();
            conn.load(list.getCompound(i));
            this.connections.add(conn);
        }
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", this.id);
        tag.putString("Title", this.title);
        tag.putString("Text", this.text);
        tag.putString("Portrait", this.portrait);
        tag.putString("Sound", this.sound);
        tag.putInt("X", this.x);
        tag.putInt("Y", this.y);
        ListTag list = new ListTag();
        for (NodeConnection conn : this.connections) {
            CompoundTag t = new CompoundTag();
            conn.save(t);
            list.add(t);
        }
        tag.put("Connections", list);
    }

    public DialogNodeData copy() {
        DialogNodeData copy = new DialogNodeData();
        copy.id = this.id;
        copy.title = this.title;
        copy.text = this.text;
        copy.portrait = this.portrait;
        copy.sound = this.sound;
        copy.x = this.x;
        copy.y = this.y;
        for (NodeConnection conn : this.connections) {
            copy.connections.add(conn.copy());
        }
        return copy;
    }

    public static class NodeConnection {
        private String targetNodeId = "";
        private int sourceSlot = 0;
        private String optionText = "";
        private String conditionScript = "";

        public String getTargetNodeId() { return targetNodeId; }
        public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
        public int getSourceSlot() { return sourceSlot; }
        public void setSourceSlot(int sourceSlot) { this.sourceSlot = sourceSlot; }
        public String getOptionText() { return optionText; }
        public void setOptionText(String optionText) { this.optionText = optionText; }
        public String getConditionScript() { return conditionScript; }
        public void setConditionScript(String conditionScript) { this.conditionScript = conditionScript; }

        public void load(CompoundTag tag) {
            this.targetNodeId = tag.getString("TargetNodeId");
            this.sourceSlot = tag.getInt("SourceSlot");
            this.optionText = tag.getString("OptionText");
            this.conditionScript = tag.getString("ConditionScript");
        }

        public void save(CompoundTag tag) {
            tag.putString("TargetNodeId", this.targetNodeId);
            tag.putInt("SourceSlot", this.sourceSlot);
            tag.putString("OptionText", this.optionText);
            tag.putString("ConditionScript", this.conditionScript);
        }

        public NodeConnection copy() {
            NodeConnection copy = new NodeConnection();
            copy.targetNodeId = this.targetNodeId;
            copy.sourceSlot = this.sourceSlot;
            copy.optionText = this.optionText;
            copy.conditionScript = this.conditionScript;
            return copy;
        }
    }
}