package com.iscript.iscript.data.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class StateNode {
    public String id;
    public String name;
    public int color;
    public int posX;
    public int posY;
    public final List<StateAction> onEnter = new ArrayList<>();
    public final List<StateAction> onTick = new ArrayList<>();
    public final List<StateTransition> transitions = new ArrayList<>();

    public StateNode() {}

    public StateNode(String id, String name, int color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        tag.putInt("color", color);
        tag.putInt("posX", posX);
        tag.putInt("posY", posY);

        ListTag enterTag = new ListTag();
        for (StateAction a : onEnter) enterTag.add(a.save());
        tag.put("onEnter", enterTag);

        ListTag tickTag = new ListTag();
        for (StateAction a : onTick) tickTag.add(a.save());
        tag.put("onTick", tickTag);

        ListTag transTag = new ListTag();
        for (StateTransition t : transitions) transTag.add(t.save());
        tag.put("transitions", transTag);

        return tag;
    }

    public static StateNode load(CompoundTag tag) {
        StateNode n = new StateNode();
        n.id = tag.getString("id");
        n.name = tag.getString("name");
        n.color = tag.getInt("color");
        n.posX = tag.getInt("posX");
        n.posY = tag.getInt("posY");

        ListTag enterTag = tag.getList("onEnter", Tag.TAG_COMPOUND);
        for (int i = 0; i < enterTag.size(); i++) {
            n.onEnter.add(StateAction.load(enterTag.getCompound(i)));
        }

        ListTag tickTag = tag.getList("onTick", Tag.TAG_COMPOUND);
        for (int i = 0; i < tickTag.size(); i++) {
            n.onTick.add(StateAction.load(tickTag.getCompound(i)));
        }

        ListTag transTag = tag.getList("transitions", Tag.TAG_COMPOUND);
        for (int i = 0; i < transTag.size(); i++) {
            n.transitions.add(StateTransition.load(transTag.getCompound(i)));
        }

        return n;
    }
}