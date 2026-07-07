package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.event.EventTrigger;
import com.iscript.iscript.event.EventType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventSavedData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_events";
    private final Map<EventType, List<EventTrigger>> triggers = new HashMap<>();

    public EventSavedData() {
        for (EventType type : EventType.values()) {
            triggers.put(type, new ArrayList<>());
        }
    }

    public static EventSavedData load(CompoundTag tag) {
        EventSavedData data = new EventSavedData();
        for (EventType type : EventType.values()) {
            String key = "Triggers_" + type.name();
            if (tag.contains(key)) {
                ListTag list = tag.getList(key, 10);
                for (int i = 0; i < list.size(); i++) {
                    EventTrigger trigger = new EventTrigger();
                    trigger.load(list.getCompound(i));
                    data.triggers.get(type).add(trigger);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (EventType type : EventType.values()) {
            ListTag list = new ListTag();
            for (EventTrigger trigger : triggers.get(type)) {
                CompoundTag t = new CompoundTag();
                trigger.save(t);
                list.add(t);
            }
            tag.put("Triggers_" + type.name(), list);
        }
        return tag;
    }

    public List<EventTrigger> getTriggers(EventType type) {
        return triggers.getOrDefault(type, new ArrayList<>());
    }

    public void addTrigger(EventTrigger trigger) {
        triggers.computeIfAbsent(trigger.getType(), k -> new ArrayList<>()).add(trigger);
        setDirty();
    }

    public void removeTrigger(EventType type, int index) {
        List<EventTrigger> list = triggers.get(type);
        if (list != null && index >= 0 && index < list.size()) {
            list.remove(index);
            setDirty();
        }
    }

    public static EventSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(EventSavedData::load, EventSavedData::new, DATA_NAME);
    }
}