package com.iscript.iscript.event;

import com.iscript.iscript.data.EventSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class EventManager {

    public static void trigger(EventType type, Player player, ServerLevel level) {
        List<EventTrigger> triggers = EventSavedData.get(level).getTriggers(type);
        for (EventTrigger trigger : triggers) {
            if (trigger.isEnabled()) {
                trigger.execute(player, level);
            }
        }
    }

    public static void addTrigger(ServerLevel level, EventTrigger trigger) {
        EventSavedData.get(level).addTrigger(trigger);
    }

    public static void removeTrigger(ServerLevel level, EventType type, int index) {
        EventSavedData.get(level).removeTrigger(type, index);
    }

    public static List<EventTrigger> getTriggers(ServerLevel level, EventType type) {
        return EventSavedData.get(level).getTriggers(type);
    }
}