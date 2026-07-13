package com.iscript.iscript.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

public class StateMachine {
    String id;
    String name;
    String entryNode;
    final Map<String, StateNode> nodes = new LinkedHashMap<>();

    public StateMachine() {}

    public StateMachine(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putString("entryNode", entryNode != null ? entryNode : "");

        ListTag nodesTag = new ListTag();
        for (StateNode n : nodes.values()) {
            nodesTag.add(n.save());
        }
        tag.put("nodes", nodesTag);
        return tag;
    }

    public static StateMachine load(CompoundTag tag) {
        StateMachine m = new StateMachine();
        m.id = tag.getString("id");
        m.name = tag.getString("name");
        m.entryNode = tag.getString("entryNode");
        if (m.entryNode.isEmpty()) m.entryNode = null;

        ListTag nodesTag = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodesTag.size(); i++) {
            StateNode n = StateNode.load(nodesTag.getCompound(i));
            m.nodes.put(n.id, n);
        }
        return m;
    }
}