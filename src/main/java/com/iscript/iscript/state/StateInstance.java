package com.iscript.iscript.state;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class StateInstance {
    String instanceId;
    String machineId;
    String scope;
    String targetId;
    String currentNode;
    int ticksInState;
    final Map<String, Object> variables = new HashMap<>();

    public StateInstance() {}

    public StateInstance(String instanceId, String machineId, String scope, String targetId, String currentNode) {
        this.instanceId = instanceId;
        this.machineId = machineId;
        this.scope = scope;
        this.targetId = targetId;
        this.currentNode = currentNode;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("instanceId", instanceId);
        tag.putString("machineId", machineId);
        tag.putString("scope", scope);
        tag.putString("targetId", targetId);
        tag.putString("currentNode", currentNode != null ? currentNode : "");
        tag.putInt("ticksInState", ticksInState);

        CompoundTag vars = new CompoundTag();
        for (Map.Entry<String, Object> e : variables.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Integer) vars.putInt(e.getKey(), (Integer) v);
            else if (v instanceof Double) vars.putDouble(e.getKey(), (Double) v);
            else if (v instanceof Boolean) vars.putBoolean(e.getKey(), (Boolean) v);
            else vars.putString(e.getKey(), v.toString());
        }
        tag.put("variables", vars);

        return tag;
    }

    public static StateInstance load(CompoundTag tag) {
        StateInstance i = new StateInstance();
        i.instanceId = tag.getString("instanceId");
        i.machineId = tag.getString("machineId");
        i.scope = tag.getString("scope");
        i.targetId = tag.getString("targetId");
        i.currentNode = tag.getString("currentNode");
        if (i.currentNode.isEmpty()) i.currentNode = null;
        i.ticksInState = tag.getInt("ticksInState");

        CompoundTag vars = tag.getCompound("variables");
        for (String key : vars.getAllKeys()) {
            if (vars.contains(key, 3)) i.variables.put(key, vars.getInt(key));
            else if (vars.contains(key, 6)) i.variables.put(key, vars.getDouble(key));
            else if (vars.contains(key, 1)) i.variables.put(key, vars.getBoolean(key));
            else i.variables.put(key, vars.getString(key));
        }

        return i;
    }
}