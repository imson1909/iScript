package com.iscript.iscript.data.state.runtime;

import com.iscript.iscript.data.state.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class StateRuntime {
    private static final int MAX_PER_TICK = 100;
    private static final long BUDGET_NANOS = 5_000_000L;
    private int tickIndex = 0;

    public void onServerTick(MinecraftServer server) {
        List<StateInstance> active = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            StateMachineData data = StateMachineData.get(level);
            active.addAll(data.getInstances());
        }
        if (active.isEmpty()) return;

        int perTick = Math.min(active.size(), MAX_PER_TICK);
        long deadline = System.nanoTime() + BUDGET_NANOS;

        for (int i = 0; i < perTick; i++) {
            if (System.nanoTime() > deadline) break;
            StateInstance inst = active.get(tickIndex % active.size());
            tickInstance(inst, server);
            tickIndex++;
        }
    }

    private void tickInstance(StateInstance inst, MinecraftServer server) {
        StateMachineData data = null;
        for (ServerLevel level : server.getAllLevels()) {
            StateMachineData d = StateMachineData.get(level);
            if (d.getMachine(inst.machineId) != null) {
                data = d;
                break;
            }
        }
        if (data == null) return;

        StateMachine machine = data.getMachine(inst.machineId);
        if (machine == null) {
            data.removeInstance(inst.instanceId);
            return;
        }

        StateNode node = machine.nodes.get(inst.currentNode);
        if (node == null) {
            if (machine.entryNode != null && machine.nodes.containsKey(machine.entryNode)) {
                inst.currentNode = machine.entryNode;
                inst.ticksInState = 0;
                node = machine.nodes.get(inst.currentNode);
            } else {
                data.removeInstance(inst.instanceId);
                return;
            }
        }

        inst.ticksInState++;

        Entity target = resolveTarget(inst, server);
        if (target == null && !inst.scope.equals("global")) {
            data.removeInstance(inst.instanceId);
            return;
        }

        ServerLevel level = null;
        for (ServerLevel l : server.getAllLevels()) {
            if (l.dimension().location().toString().equals(inst.targetId.contains(":") ? inst.targetId.substring(0, inst.targetId.indexOf(":")) : "minecraft:overworld")) {
                level = l;
                break;
            }
        }
        if (level == null) level = server.overworld();

        StateContext ctx = new StateContext(level, target, inst.machineId, inst.instanceId);
        ctx.localVars.putAll(inst.variables);
        ctx.localVars.put("_ticksInState", inst.ticksInState);

        if (inst.ticksInState == 1) {
            for (StateAction action : node.onEnter) {
                ActionExecutor.execute(action, ctx);
            }
        }

        for (StateAction action : node.onTick) {
            ActionExecutor.execute(action, ctx);
        }

        for (StateTransition transition : node.transitions) {
            if (!transition.auto) continue;

            boolean allPass = true;
            for (StateCondition condition : transition.conditions) {
                if (!ConditionEvaluator.evaluate(condition, ctx)) {
                    allPass = false;
                    break;
                }
            }

            if (allPass) {
                for (StateAction action : transition.actions) {
                    ActionExecutor.execute(action, ctx);
                }
                inst.currentNode = transition.targetNode;
                inst.ticksInState = 0;
                inst.variables.clear();
                inst.variables.putAll(ctx.localVars);
                inst.variables.remove("_ticksInState");
                data.setDirty();
                break;
            }
        }

        inst.variables.putAll(ctx.localVars);
        inst.variables.remove("_ticksInState");
    }

    private Entity resolveTarget(StateInstance inst, MinecraftServer server) {
        return switch (inst.scope) {
            case "player" -> server.getPlayerList().getPlayerByName(inst.targetId);
            case "npc", "entity" -> {
                for (ServerLevel level : server.getAllLevels()) {
                    try {
                        Entity e = level.getEntity(java.util.UUID.fromString(inst.targetId));
                        if (e != null) yield e;
                    } catch (IllegalArgumentException ignored) {}
                }
                yield null;
            }
            case "global", "world", "region" -> null;
            default -> null;
        };
    }
}