package com.iscript.iscript.state;

import net.minecraft.nbt.CompoundTag;

public class StateCondition {
    ConditionType type;
    final CompoundTag params = new CompoundTag();
    boolean invert;

    public StateCondition() {}

    public StateCondition(ConditionType type) {
        this.type = type;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.put("params", params.copy());
        tag.putBoolean("invert", invert);
        return tag;
    }

    public static StateCondition load(CompoundTag tag) {
        StateCondition c = new StateCondition();
        try {
            c.type = ConditionType.valueOf(tag.getString("type"));
        } catch (IllegalArgumentException e) {
            c.type = ConditionType.SCRIPT;
        }
        c.params.merge(tag.getCompound("params"));
        c.invert = tag.getBoolean("invert");
        return c;
    }
}