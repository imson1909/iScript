package com.iscript.iscript.event;

import com.iscript.iscript.capability.ModCapabilities;
import com.iscript.iscript.data.QuestManager;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.quest.QuestObjectiveType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class QuestEvents {

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource() == null || event.getSource().getEntity() == null) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        String target = event.getEntity().getType().getDescriptionId();
        player.getCapability(ModCapabilities.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestData quest : QuestManager.getAll(level).values()) {
                if (data.isCompleted(quest.getId())) continue;
                if (quest.getObjectiveType() == QuestObjectiveType.KILL && target.equals(quest.getTarget())) {
                    int progress = data.getProgress(quest.getId()) + 1;
                    data.setProgress(quest.getId(), progress);
                    if (progress >= quest.getRequiredCount()) {
                        data.complete(quest.getId());
                        executeReward(quest, player, level);
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public static void onPickup(PlayerEvent.ItemPickupEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        ItemStack stack = event.getStack();
        String target = stack.getItem().getDescriptionId();
        player.getCapability(ModCapabilities.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestData quest : QuestManager.getAll(level).values()) {
                if (data.isCompleted(quest.getId())) continue;
                if (quest.getObjectiveType() == QuestObjectiveType.COLLECT && target.equals(quest.getTarget())) {
                    int progress = data.getProgress(quest.getId()) + stack.getCount();
                    data.setProgress(quest.getId(), progress);
                    if (progress >= quest.getRequiredCount()) {
                        data.complete(quest.getId());
                        executeReward(quest, player, level);
                    }
                }
            }
        });
    }

    private static void executeReward(QuestData quest, Player player, ServerLevel level) {
        if (!quest.getRewardCommand().isEmpty()) {
            level.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withLevel(level).withPosition(player.position()),
                    quest.getRewardCommand().replace("@p", player.getGameProfile().getName())
            );
        }
    }
}