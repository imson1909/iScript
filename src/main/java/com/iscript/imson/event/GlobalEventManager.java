package com.iscript.imson.event;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.data.DataAccess;
import com.iscript.imson.data.Graph;
import com.iscript.imson.script.ScriptGraphExecutor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Глобальный менеджер событий — запускает Event Graph'ы на действия ВСЕХ игроков.
 * В Mappet такое есть, но у тебя можно сделать лучше:
 * - Поддержка фильтров по миру, игроку, предмету
 * - Возможность отмены события из графа
 * - Передача контекста (какой предмет, какой блок и т.д.) в localVars графа
 */
@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlobalEventManager {

    private static final Map<EventType, List<GraphSubscription>> SUBSCRIPTIONS = new HashMap<>();

    public static void subscribe(String graphId, EventType type, String filter) {
        SUBSCRIPTIONS.computeIfAbsent(type, k -> new ArrayList<>()).add(new GraphSubscription(graphId, filter));
    }

    public static void unsubscribe(String graphId, EventType type) {
        List<GraphSubscription> list = SUBSCRIPTIONS.get(type);
        if (list != null) list.removeIf(s -> s.graphId.equals(graphId));
    }

    public static void clear() {
        SUBSCRIPTIONS.clear();
    }

    private static void trigger(EventType type, ServerPlayer player, ServerLevel level, Map<String, Object> context) {
        List<GraphSubscription> list = SUBSCRIPTIONS.get(type);
        if (list == null) return;
        for (GraphSubscription sub : list) {
            if (!sub.matches(player, context)) continue;
            Graph graph = DataAccess.eventGraph(sub.graphId);
            if (graph == null) continue;
            ScriptGraphExecutor exec = new ScriptGraphExecutor(graph, player, level);
            if (context != null) {
                for (Map.Entry<String, Object> e : context.entrySet()) {
                    // TODO: exec.setLocalVar(e.getKey(), e.getValue());
                }
            }
            exec.start();
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        Player p = event.getPlayer();
        if (!(p instanceof ServerPlayer player)) return;
        ItemStack stack = event.getEntity().getItem();
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("itemId", stack.getItem().builtInRegistryHolder().key().location().toString());
        ctx.put("itemCount", stack.getCount());
        trigger(EventType.ITEM_TOSS, player, player.serverLevel(), ctx);
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("containerType", event.getContainer().getClass().getSimpleName());
        trigger(EventType.PLAYER_OPEN_CONTAINER, player, player.serverLevel(), ctx);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide()) return;
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("block", event.getLevel().getBlockState(event.getPos()).getBlock().builtInRegistryHolder().key().location().toString());
        ctx.put("x", event.getPos().getX());
        ctx.put("y", event.getPos().getY());
        ctx.put("z", event.getPos().getZ());
        trigger(EventType.PLAYER_INTERACT_BLOCK, player, player.serverLevel(), ctx);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("block", event.getState().getBlock().builtInRegistryHolder().key().location().toString());
        ctx.put("x", event.getPos().getX());
        ctx.put("y", event.getPos().getY());
        ctx.put("z", event.getPos().getZ());
        trigger(EventType.BLOCK_BREAK, player, player.serverLevel(), ctx);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("block", event.getPlacedBlock().getBlock().builtInRegistryHolder().key().location().toString());
        ctx.put("x", event.getPos().getX());
        ctx.put("y", event.getPos().getY());
        ctx.put("z", event.getPos().getZ());
        trigger(EventType.BLOCK_PLACE, player, player.serverLevel(), ctx);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("targetType", BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString());
            ctx.put("targetName", event.getEntity().getName().getString());
            trigger(EventType.PLAYER_KILL_ENTITY, player, player.serverLevel(), ctx);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("killer", event.getSource().getEntity() != null ? event.getSource().getEntity().getName().getString() : "unknown");
            trigger(EventType.PLAYER_DEATH, player, player.serverLevel(), ctx);
        }
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("advancementId", event.getAdvancement().getId().toString());
        trigger(EventType.PLAYER_ADVANCEMENT, player, player.serverLevel(), ctx);
    }

    // ===== Подписка =====
    static class GraphSubscription {
        final String graphId;
        final String filter; // например "itemId=minecraft:diamond"

        GraphSubscription(String graphId, String filter) {
            this.graphId = graphId;
            this.filter = filter;
        }

        boolean matches(ServerPlayer player, Map<String, Object> context) {
            if (filter == null || filter.isEmpty()) return true;
            // Простой фильтр: key=value
            String[] parts = filter.split("=");
            if (parts.length != 2) return true;
            Object val = context != null ? context.get(parts[0]) : null;
            return val != null && val.toString().equals(parts[1]);
        }
    }
}