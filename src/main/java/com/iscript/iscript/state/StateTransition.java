package com.iscript.iscript.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class StateTransition {
    String targetNode;
    boolean auto;
    final List<StateCondition> conditions = new ArrayList<>();
    final List<StateAction> actions = new ArrayList<>();

    public StateTransition() {}

    public StateTransition(String targetNode, boolean auto) {
        this.targetNode = targetNode;
        this.auto = auto;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("targetNode", targetNode);
        tag.putBoolean("auto", auto);

        ListTag condTag = new ListTag();
        for (StateCondition c : conditions) condTag.add(c.save());
        tag.put("conditions", condTag);

        ListTag actTag = new ListTag();
        for (StateAction a : actions) actTag.add(a.save());
        tag.put("actions", actTag);

        return tag;
    }

    public static StateTransition load(CompoundTag tag) {
        StateTransition t = new StateTransition();
        t.targetNode = tag.getString("targetNode");
        t.auto = tag.getBoolean("auto");

        ListTag condTag = tag.getList("conditions", Tag.TAG_COMPOUND);
        for (int i = 0; i < condTag.size(); i++) {
            t.conditions.add(StateCondition.load(condTag.getCompound(i)));
        }

        ListTag actTag = tag.getList("actions", Tag.TAG_COMPOUND);
        for (int i = 0; i < actTag.size(); i++) {
            t.actions.add(StateAction.load(actTag.getCompound(i)));
        }

        return t;
    }
}