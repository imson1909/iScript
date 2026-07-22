package com.iscript.iscript.api.states;

import net.minecraftforge.eventbus.api.Event;

public class StateChangedEvent extends Event {
    public final States states;
    public final String key;
    public final Object previous;
    public final Object current;
    public final boolean global;

    public StateChangedEvent(States states, String key, Object previous, Object current, boolean global) {
        this.states = states;
        this.key = key;
        this.previous = previous;
        this.current = current;
        this.global = global;
    }
}