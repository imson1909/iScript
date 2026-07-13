package com.iscript.iscript.state.runtime;

import com.iscript.iscript.state.ActionType;
import com.iscript.iscript.state.StateAction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class ActionExecutor {

    public static void execute(StateAction action, StateContext ctx) {
        CompoundTag p = action.params;

        switch (action.type) {
            case RUN_SCRIPT -> {
                String scriptId = p.getString("scriptId");
            }
            case SEND_MESSAGE -> {
                Player player = ctx.getPlayer();
                if (player == null) return;
                String message = p.getString("message");
                player.sendSystemMessage(Component.literal(message));
            }
            case GIVE_ITEM -> {
                Player player = ctx.getPlayer();
                if (player == null) return;
                String itemId = p.getString("item");
                int count = p.getInt("count");
                if (count < 1) count = 1;
                ItemStack stack = new ItemStack(Items.STONE, count);
                player.getInventory().add(stack);
            }
            case SPAWN_ENTITY -> {
                String entityType = p.getString("entityType");
                double ex = p.getDouble("x");
                double ey = p.getDouble("y");
                double ez = p.getDouble("z");
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entityType));
                if (type != null) {
                    var entity = type.create(ctx.level);
                    if (entity != null) {
                        entity.setPos(ex, ey, ez);
                        ctx.level.addFreshEntity(entity);
                    }
                }
            }
            case PLAY_SOUND -> {
                String sound = p.getString("sound");
                float volume = p.getFloat("volume");
                if (volume <= 0) volume = 1.0f;
                float pitch = p.getFloat("pitch");
                if (pitch <= 0) pitch = 1.0f;
                SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(sound));
                if (event != null && ctx.target != null) {
                    ctx.level.playSound(null, ctx.target.getX(), ctx.target.getY(), ctx.target.getZ(), event, ctx.target.getSoundSource(), volume, pitch);
                }
            }
            case SET_VARIABLE -> {
                String scope = p.getString("scope");
                String key = p.getString("key");
                String value = p.getString("value");
                Object val = parseValue(value);
                switch (scope) {
                    case "local" -> ctx.localVars.put(key, val);
                    case "global" -> StateVariableStore.setGlobal(ctx.level, key, val);
                    case "player" -> {
                        if (ctx.getPlayer() != null) {
                            StateVariableStore.setPlayer(ctx.getPlayer(), key, val);
                        }
                    }
                }
            }
            case SET_STATE -> {
                String nodeId = p.getString("nodeId");
            }
            case START_QUEST -> {
                String questId = p.getString("questId");
            }
            case OPEN_DIALOG -> {
                String dialogId = p.getString("dialogId");
                String npcId = p.getString("npcId");
            }
            case START_CUTSCENE -> {
                String cutsceneId = p.getString("cutsceneId");
            }
        }
    }

    private static Object parseValue(String s) {
        if (s.equalsIgnoreCase("true")) return true;
        if (s.equalsIgnoreCase("false")) return false;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {}
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ignored) {}
        return s;
    }
}