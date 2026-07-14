package com.iscript.iscript.data.state;

import net.minecraft.nbt.CompoundTag;

public class StateAction {
    public ActionType type;
    public final CompoundTag params = new CompoundTag();

    public StateAction() {}

    public StateAction(ActionType type) {
        this.type = type;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.put("params", params.copy());
        return tag;
    }

    public static StateAction load(CompoundTag tag) {
        StateAction a = new StateAction();
        try {
            a.type = ActionType.valueOf(tag.getString("type"));
        } catch (IllegalArgumentException e) {
            a.type = ActionType.RUN_SCRIPT;
        }
        a.params.merge(tag.getCompound("params"));
        return a;
    }
}