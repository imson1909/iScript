package com.iscript.iscript.data.state.runtime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class StateContext {
    public final ServerLevel level;
    public final Entity target;
    public final String machineId;
    public final String instanceId;
    public final Map<String, Object> localVars = new HashMap<>();

    public StateContext(ServerLevel level, Entity target, String machineId, String instanceId) {
        this.level = level;
        this.target = target;
        this.machineId = machineId;
        this.instanceId = instanceId;
    }

    public Player getPlayer() {
        return target instanceof Player ? (Player) target : null;
    }
}