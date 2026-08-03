package com.iscript.iscript.event;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.api.triggers.TriggerData;
import com.iscript.iscript.api.triggers.TriggerManager;
import com.iscript.iscript.api.triggers.TriggerType;
import com.iscript.iscript.script.ScriptEngine;
import com.iscript.iscript.script.ScriptEventContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TriggerEventHandler {

    private static final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerState {
        boolean wasWalking = false;
        boolean wasSneaking = false;
        boolean wasGuiOpen = false;
        boolean wasSwinging = false;
        ItemStack lastMainHand = ItemStack.EMPTY;
        ItemStack lastOffHand = ItemStack.EMPTY;
    }

    /* ========== LIVING EVENTS ========== */

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        LivingEntity entity = event.getEntity();
        ServerPlayer player = entity instanceof ServerPlayer ? (ServerPlayer) entity : null;
        ServerLevel level = entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null;
        fire(TriggerType.LIVING_EQUIPMENT_CHANGE, player, entity, null, level);
        if (player != null) fire(TriggerType.PLAYER_EQUIPMENT_CHANGE, player, entity, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        ServerPlayer player = entity instanceof ServerPlayer ? (ServerPlayer) entity : null;
        ServerLevel level = entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null;
        fire(TriggerType.LIVING_JUMP, player, entity, null, level);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        ServerPlayer player = entity instanceof ServerPlayer ? (ServerPlayer) entity : null;
        ServerLevel level = entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null;
        fire(TriggerType.LIVING_FALL, player, entity, null, level);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        ServerPlayer player = entity instanceof ServerPlayer ? (ServerPlayer) entity : null;
        ServerLevel level = entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null;
        fire(TriggerType.LIVING_DEATH, player, entity, event.getSource().getEntity(), level);
        if (player != null) fire(TriggerType.PLAYER_DEATH, player, entity, event.getSource().getEntity(), level);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        ServerPlayer player = entity instanceof ServerPlayer ? (ServerPlayer) entity : null;
        ServerLevel level = entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null;
        fire(TriggerType.LIVING_HURT, player, entity, event.getSource().getEntity(), level);
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        ServerPlayer player = entity instanceof ServerPlayer ? (ServerPlayer) entity : null;
        ServerLevel level = entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null;
        fire(TriggerType.LIVING_HEAL, player, entity, null, level);
    }

    /* ========== PLAYER TICK (walk, sneak, GUI, morph, LKM air) ========== */

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        PlayerState state = playerStates.computeIfAbsent(player.getUUID(), k -> new PlayerState());

        boolean isWalking = player.getDeltaMovement().horizontalDistanceSqr() > 0.0001 && !player.isShiftKeyDown();
        if (isWalking && !state.wasWalking) {
            fire(TriggerType.LIVING_WALK, player, player, null, level);
        }
        state.wasWalking = isWalking;

        boolean isSneaking = player.isShiftKeyDown();
        if (isSneaking && !state.wasSneaking) {
            fire(TriggerType.PLAYER_SNEAK, player, player, null, level);
        }
        state.wasSneaking = isSneaking;

        boolean isGuiOpen = player.containerMenu != player.inventoryMenu;
        if (isGuiOpen && !state.wasGuiOpen) {
            fire(TriggerType.PLAYER_OPEN_GUI, player, player, null, level);
            fire(TriggerType.PLAYER_OPEN_CONTAINER, player, player, null, level);
        }
        if (!isGuiOpen && state.wasGuiOpen) {
            fire(TriggerType.PLAYER_CLOSE_GUI, player, player, null, level);
        }
        state.wasGuiOpen = isGuiOpen;

        boolean isSwinging = player.swinging;
        if (isSwinging && !state.wasSwinging) {
            fire(TriggerType.PLAYER_LEFT_CLICK_AIR, player, player, null, level);
        }
        state.wasSwinging = isSwinging;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        state.lastMainHand = mainHand.copy();
        state.lastOffHand = offHand.copy();
    }

    /* ========== PLAYER EVENTS ========== */

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        fire(TriggerType.PLAYER_CHANGED_DIMENSION, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        playerStates.put(player.getUUID(), new PlayerState());
        fire(TriggerType.PLAYER_LOGGED_IN, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        playerStates.remove(player.getUUID());
        fire(TriggerType.PLAYER_LOGGED_OUT, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        fire(TriggerType.PLAYER_RESPAWN, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item)) return;
        if (item.getOwner() instanceof ServerPlayer player) {
            fire(TriggerType.PLAYER_DROP_ITEM, player, player, item, player.serverLevel());
        }
    }

    @SubscribeEvent
    public static void onEntityItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        fire(TriggerType.PLAYER_PICKUP_ITEM, player, player, event.getItem(), player.serverLevel());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        fire(TriggerType.PLAYER_LEFT_CLICK_BLOCK, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        fire(TriggerType.PLAYER_RIGHT_CLICK_BLOCK, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        fire(TriggerType.PLAYER_RIGHT_CLICK_AIR, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onPlayerContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        fire(TriggerType.PLAYER_OPEN_CONTAINER, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onPlayerContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        fire(TriggerType.PLAYER_CLOSE_GUI, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;
        fire(TriggerType.PLAYER_CHAT, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        fire(TriggerType.PLAYER_BREAK_BLOCK, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        fire(TriggerType.PLAYER_PLACE_BLOCK, player, player, null, player.serverLevel());
    }

    @SubscribeEvent
    public static void onLivingEntityUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (event.getItem().getItem().getFoodProperties() != null) {
            fire(TriggerType.PLAYER_EAT, player, player, null, level);
        } else {
            fire(TriggerType.PLAYER_DRINK_POTION, player, player, null, level);
        }
    }

    /* ========== SERVER EVENTS ========== */

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        ServerLevel level = event.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        fireServer(TriggerType.SERVER_TICK, level);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (level != null) {
            TriggerManager.loadAll(level);
        }
        fireServer(TriggerType.SERVER_LOAD, level);
    }

    /* ========== HELPERS ========== */

    private static void fire(TriggerType type, ServerPlayer player, LivingEntity subject, Entity object, ServerLevel level) {
        ScriptEngine engine = ScriptEngine.getInstance();
        if (!engine.isAvailable()) return;

        for (TriggerData trigger : TriggerManager.getByType(type)) {
            try {
                ScriptEventContext ctx = new ScriptEventContext(player, subject, object, level);
                engine.executeTrigger(trigger.getScriptId(), trigger.getFunctionName(), ctx, player, level);
            } catch (Exception e) {
                IScriptMod.LOGGER.error("Trigger {} ({}): {}", trigger.getId(), type.name(), e.getMessage());
            }
        }
    }

    private static void fireServer(TriggerType type, ServerLevel level) {
        ScriptEngine engine = ScriptEngine.getInstance();
        if (!engine.isAvailable()) return;

        for (TriggerData trigger : TriggerManager.getByType(type)) {
            try {
                ScriptEventContext ctx = new ScriptEventContext(null, null, null, level);
                engine.executeTrigger(trigger.getScriptId(), trigger.getFunctionName(), ctx, null, level);
            } catch (Exception e) {
                IScriptMod.LOGGER.error("Trigger {} ({}): {}", trigger.getId(), type.name(), e.getMessage());
            }
        }
    }
}