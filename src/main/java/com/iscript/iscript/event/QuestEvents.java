package com.iscript.iscript.event;

import com.iscript.iscript.data.QuestManager;
import com.iscript.iscript.data.PlayerQuestData;
import com.iscript.iscript.data.quest.*;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.QuestObjectiveUpdatePacket;
import com.iscript.iscript.network.packet.SyncQuestProgressPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class QuestEvents {

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource() == null || event.getSource().getEntity() == null) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        String target = event.getEntity().getType().getDescriptionId();
        UUID playerId = player.getUUID();
        processObjective(level, playerId, QuestObjectiveType.KILL, target, 1, null, null);
    }

    @SubscribeEvent
    public static void onPickup(PlayerEvent.ItemPickupEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        ItemStack stack = event.getStack();
        String target = stack.getItem().getDescriptionId();
        UUID playerId = player.getUUID();
        processObjective(level, playerId, QuestObjectiveType.COLLECT, target, stack.getCount(), null, null);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.level().getGameTime() % 20 != 0) return;

        UUID playerId = player.getUUID();
        Vec3 pos = player.position();
        BlockPos blockPos = player.blockPosition();

        Map<String, QuestProgress> quests = QuestManager.getPlayerQuests(level, playerId);
        for (QuestProgress progress : quests.values()) {
            if (progress.getStatus() != QuestStatus.ACTIVE) continue;
            QuestStage stage = progress.getCurrentStage();
            if (stage == null) continue;
            for (int i = 0; i < stage.getObjectives().size(); i++) {
                QuestObjective obj = stage.getObjectives().get(i);
                if (obj.getType() != QuestObjectiveType.GO_TO) continue;
                if (obj.getCurrentCount() >= obj.getRequiredCount()) continue;

                String[] parts = obj.getTarget().split(",");
                if (parts.length >= 3) {
                    try {
                        double tx = Double.parseDouble(parts[0].trim());
                        double ty = Double.parseDouble(parts[1].trim());
                        double tz = Double.parseDouble(parts[2].trim());
                        double dist = pos.distanceTo(new Vec3(tx, ty, tz));
                        if (dist <= 3.0) {
                            obj.setCurrentCount(obj.getRequiredCount());
                            checkStageComplete(level, playerId, progress, stage, i);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        Entity target = event.getTarget();
        if (target == null) return;

        UUID playerId = player.getUUID();
        String targetId = target.getType().getDescriptionId();
        processObjective(level, playerId, QuestObjectiveType.TALK_TO, targetId, 1, null, null);
    }

    private static void processObjective(ServerLevel level, UUID playerId, QuestObjectiveType type, String target, int amount, BlockPos pos, ItemStack item) {
        Map<String, QuestProgress> quests = QuestManager.getPlayerQuests(level, playerId);
        for (QuestProgress progress : quests.values()) {
            if (progress.getStatus() != QuestStatus.ACTIVE) continue;
            QuestStage stage = progress.getCurrentStage();
            if (stage == null) continue;
            for (int i = 0; i < stage.getObjectives().size(); i++) {
                QuestObjective obj = stage.getObjectives().get(i);
                if (obj.getType() != type) continue;
                if (!obj.getTarget().equals(target)) continue;
                if (obj.getCurrentCount() >= obj.getRequiredCount()) continue;

                int newCount = Math.min(obj.getCurrentCount() + amount, obj.getRequiredCount());
                obj.setCurrentCount(newCount);
                checkStageComplete(level, playerId, progress, stage, i);
            }
        }
    }

    private static void checkStageComplete(ServerLevel level, UUID playerId, QuestProgress progress, QuestStage stage, int objIndex) {
        boolean allComplete = true;
        for (QuestObjective obj : stage.getObjectives()) {
            if (obj.getCurrentCount() < obj.getRequiredCount()) {
                allComplete = false;
                break;
            }
        }

        int stageIdx = progress.getCurrentStageIndex();
        QuestObjective obj = stage.getObjectives().get(objIndex);
        boolean questComplete = false;

        if (allComplete) {
            boolean wasLast = progress.advanceStage();
            if (wasLast) {
                questComplete = true;
            }
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            IScriptNetwork.sendToPlayer(new QuestObjectiveUpdatePacket(
                    progress.getQuestId(), stageIdx, objIndex,
                    obj.getCurrentCount(), obj.getRequiredCount(),
                    allComplete, questComplete
            ), player);

            if (questComplete) {
                IScriptNetwork.sendToPlayer(new SyncQuestProgressPacket(
                        QuestManager.getPlayerQuests(level, playerId),
                        QuestManager.getCompletedQuests(level, playerId)
                ), player);
            }
        }

        PlayerQuestData.get(level).setDirty();
    }
}