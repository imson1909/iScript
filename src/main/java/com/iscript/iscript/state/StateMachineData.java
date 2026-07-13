package com.iscript.iscript.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class StateMachineData extends SavedData {
    private static final String DATA_NAME = "iscript_statemachines";
    private static final int DATA_VERSION = 1;

    private final Map<String, StateMachine> machines = new LinkedHashMap<>();
    private final Map<String, StateInstance> instances = new HashMap<>();
    private int nextInstanceId = 1;

    public static StateMachineData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        StateMachineData::new,
                        StateMachineData::load,
                        null
                ),
                DATA_NAME
        );
    }

    public StateMachineData() {}

    public Collection<StateMachine> getMachines() {
        return machines.values();
    }

    public StateMachine getMachine(String id) {
        return machines.get(id);
    }

    public void putMachine(StateMachine machine) {
        machines.put(machine.id, machine);
        setDirty();
    }

    public void removeMachine(String id) {
        machines.remove(id);
        instances.values().removeIf(i -> i.machineId.equals(id));
        setDirty();
    }

    public boolean hasMachine(String id) {
        return machines.containsKey(id);
    }

    public void renameMachine(String oldId, String newId) {
        StateMachine m = machines.remove(oldId);
        if (m != null) {
            m.id = newId;
            machines.put(newId, m);
            for (StateInstance i : instances.values()) {
                if (i.machineId.equals(oldId)) i.machineId = newId;
            }
            setDirty();
        }
    }

    public StateInstance createInstance(String machineId, String scope, String targetId) {
        StateMachine machine = machines.get(machineId);
        if (machine == null) return null;
        String iid = machineId + "_" + scope + "_" + targetId + "_" + nextInstanceId++;
        StateInstance inst = new StateInstance(iid, machineId, scope, targetId, machine.entryNode);
        instances.put(iid, inst);
        setDirty();
        return inst;
    }

    public StateInstance getInstance(String iid) {
        return instances.get(iid);
    }

    public Collection<StateInstance> getInstances() {
        return instances.values();
    }

    public void removeInstance(String iid) {
        instances.remove(iid);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("version", DATA_VERSION);
        tag.putInt("nextInstanceId", nextInstanceId);

        ListTag machinesTag = new ListTag();
        for (StateMachine m : machines.values()) {
            machinesTag.add(m.save());
        }
        tag.put("machines", machinesTag);

        ListTag instancesTag = new ListTag();
        for (StateInstance i : instances.values()) {
            instancesTag.add(i.save());
        }
        tag.put("instances", instancesTag);

        return tag;
    }

    public static StateMachineData load(CompoundTag tag) {
        StateMachineData data = new StateMachineData();
        int version = tag.getInt("version");
        data.nextInstanceId = tag.getInt("nextInstanceId");
        if (data.nextInstanceId < 1) data.nextInstanceId = 1;

        ListTag machinesTag = tag.getList("machines", Tag.TAG_COMPOUND);
        for (int i = 0; i < machinesTag.size(); i++) {
            StateMachine m = StateMachine.load(machinesTag.getCompound(i));
            data.machines.put(m.id, m);
        }

        if (version < DATA_VERSION) {
            data = migrate(data, version);
        }

        ListTag instancesTag = tag.getList("instances", Tag.TAG_COMPOUND);
        for (int i = 0; i < instancesTag.size(); i++) {
            StateInstance inst = StateInstance.load(instancesTag.getCompound(i));
            data.instances.put(inst.instanceId, inst);
        }

        return data;
    }

    private static StateMachineData migrate(StateMachineData data, int fromVersion) {
        return data;
    }
}