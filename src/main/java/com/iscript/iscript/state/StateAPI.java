package com.iscript.iscript.state;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class StateAPI {

    public static String startMachine(ServerLevel level, String machineId, String scope, String targetId) {
        StateMachineData data = StateMachineData.get(level);
        if (!data.hasMachine(machineId)) return null;
        StateInstance inst = data.createInstance(machineId, scope, targetId);
        return inst != null ? inst.instanceId : null;
    }

    public static boolean stopMachine(ServerLevel level, String instanceId) {
        StateMachineData data = StateMachineData.get(level);
        if (data.getInstance(instanceId) == null) return false;
        data.removeInstance(instanceId);
        return true;
    }

    public static boolean triggerTransition(ServerLevel level, String instanceId, String triggerId) {
        StateMachineData data = StateMachineData.get(level);
        StateInstance inst = data.getInstance(instanceId);
        if (inst == null) return false;

        StateMachine machine = data.getMachine(inst.machineId);
        if (machine == null) return false;

        StateNode node = machine.nodes.get(inst.currentNode);
        if (node == null) return false;

        for (StateTransition transition : node.transitions) {
            if (transition.auto) continue;
            if (!transition.targetNode.equals(triggerId) && !triggerId.equals("manual")) continue;

            boolean allPass = true;
            for (StateCondition condition : transition.conditions) {
                if (!com.iscript.iscript.state.runtime.ConditionEvaluator.evaluate(condition, buildContext(inst, level))) {
                    allPass = false;
                    break;
                }
            }

            if (allPass) {
                for (StateAction action : transition.actions) {
                    com.iscript.iscript.state.runtime.ActionExecutor.execute(action, buildContext(inst, level));
                }
                inst.currentNode = transition.targetNode;
                inst.ticksInState = 0;
                data.setDirty();
                return true;
            }
        }
        return false;
    }

    public static boolean setVariable(ServerLevel level, String instanceId, String key, Object value) {
        StateMachineData data = StateMachineData.get(level);
        StateInstance inst = data.getInstance(instanceId);
        if (inst == null) return false;
        inst.variables.put(key, value);
        data.setDirty();
        return true;
    }

    public static Object getVariable(ServerLevel level, String instanceId, String key) {
        StateMachineData data = StateMachineData.get(level);
        StateInstance inst = data.getInstance(instanceId);
        if (inst == null) return null;
        return inst.variables.get(key);
    }

    public static String getCurrentNode(ServerLevel level, String instanceId) {
        StateMachineData data = StateMachineData.get(level);
        StateInstance inst = data.getInstance(instanceId);
        return inst != null ? inst.currentNode : null;
    }

    private static com.iscript.iscript.state.runtime.StateContext buildContext(StateInstance inst, ServerLevel level) {
        Entity target = null;
        if (inst.scope.equals("player")) {
            target = level.getServer().getPlayerList().getPlayerByName(inst.targetId);
        } else if (inst.scope.equals("npc") || inst.scope.equals("entity")) {
            try {
                target = level.getEntity(java.util.UUID.fromString(inst.targetId));
            } catch (IllegalArgumentException ignored) {}
        }
        com.iscript.iscript.state.runtime.StateContext ctx = new com.iscript.iscript.state.runtime.StateContext(level, target, inst.machineId, inst.instanceId);
        ctx.localVars.putAll(inst.variables);
        ctx.localVars.put("_ticksInState", inst.ticksInState);
        return ctx;
    }
}