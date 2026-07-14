package com.iscript.iscript.data.state.runtime;

import com.iscript.iscript.data.state.StateCondition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

public class ConditionEvaluator {
    private static final Random RANDOM = new Random();

    public static boolean evaluate(StateCondition condition, StateContext ctx) {
        boolean result = evaluateRaw(condition, ctx);
        return condition.invert ? !result : result;
    }

    private static boolean evaluateRaw(StateCondition condition, StateContext ctx) {
        CompoundTag p = condition.params;

        return switch (condition.type) {
            case TIMER -> {
                int ticks = p.getInt("ticks");
                yield ctx.localVars.getOrDefault("_ticksInState", 0) instanceof Integer t && t >= ticks;
            }
            case HAS_ITEM -> {
                Player player = ctx.getPlayer();
                if (player == null) yield false;
                String itemId = p.getString("item");
                int count = p.getInt("count");
                if (count < 1) count = 1;
                int found = 0;
                for (ItemStack stack : player.getInventory().items) {
                    if (!stack.isEmpty() && stack.getItem().toString().equals(itemId)) {
                        found += stack.getCount();
                    }
                }
                yield found >= count;
            }
            case IN_REGION -> {
                String regionId = p.getString("regionId");
                yield false;
            }
            case QUEST_STAGE -> {
                String questId = p.getString("questId");
                String stage = p.getString("stage");
                yield false;
            }
            case NPC_ALIVE -> {
                String npcId = p.getString("npcId");
                yield false;
            }
            case RANDOM -> {
                double chance = p.getDouble("chance");
                yield RANDOM.nextDouble() < chance;
            }
            case SCRIPT -> {
                String scriptId = p.getString("scriptId");
                yield false;
            }
            case VARIABLE -> {
                String scope = p.getString("scope");
                String key = p.getString("key");
                String expected = p.getString("value");
                Object actual = switch (scope) {
                    case "local" -> ctx.localVars.get(key);
                    case "global" -> StateVariableStore.getGlobal(ctx.level, key);
                    case "player" -> ctx.getPlayer() != null ? StateVariableStore.getPlayer(ctx.getPlayer(), key) : null;
                    default -> null;
                };
                yield actual != null && actual.toString().equals(expected);
            }
        };
    }
}