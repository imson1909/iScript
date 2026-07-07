package com.iscript.iscript.event;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.capability.ModCapabilities;
import com.iscript.iscript.data.QuestManager;
import com.iscript.iscript.data.RegionManager;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.quest.QuestObjectiveType;
import com.iscript.iscript.script.ScriptGraphExecutor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.iscript.iscript.data.cutscene.CutscenePlayer;
import com.iscript.iscript.capability.PlayerDataProvider;
import com.iscript.iscript.capability.PlayerQuestProvider;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID)
public class ForgeEvents {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(ModCapabilities.PLAYER_QUESTS).isPresent()) {
                event.addCapability(new ResourceLocation(IScriptMod.MOD_ID, "player_quests"), new PlayerQuestProvider());
            }
            if (!event.getObject().getCapability(ModCapabilities.PLAYER_DATA).isPresent()) {
                event.addCapability(new ResourceLocation(IScriptMod.MOD_ID, "player_data"), new PlayerDataProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(ModCapabilities.PLAYER_QUESTS).ifPresent(oldData -> {
            event.getEntity().getCapability(ModCapabilities.PLAYER_QUESTS).ifPresent(newData -> {
                newData.load(oldData.save());
            });
        });
        event.getOriginal().getCapability(ModCapabilities.PLAYER_DATA).ifPresent(oldData -> {
            event.getEntity().getCapability(ModCapabilities.PLAYER_DATA).ifPresent(newData -> {
                newData.load(oldData.save());
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            EventManager.trigger(EventType.PLAYER_JOIN, event.getEntity(), level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            EventManager.trigger(EventType.PLAYER_LEAVE, event.getEntity(), level);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && player.level() instanceof ServerLevel level) {
            EventManager.trigger(EventType.PLAYER_DEATH, player, level);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer().level() instanceof ServerLevel level) {
            EventManager.trigger(EventType.BLOCK_BREAK, event.getPlayer(), level);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && player.level() instanceof ServerLevel level) {
            EventManager.trigger(EventType.BLOCK_PLACE, player, level);
        }
    }

    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            EventManager.trigger(EventType.PLAYER_INTERACT_ENTITY, event.getEntity(), level);
            if (event.getTarget() != null) {
                String targetDesc = event.getTarget().getType().getDescriptionId();
                Player player = event.getEntity();
                checkInteractQuest(player, level, targetDesc);
            }
        }
    }

    @SubscribeEvent
    public static void onInteractBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            EventManager.trigger(EventType.PLAYER_INTERACT_BLOCK, event.getEntity(), level);
            String blockId = event.getLevel().getBlockState(event.getPos()).getBlock().builtInRegistryHolder().key().location().toString();
            Player player = event.getEntity();
            checkInteractQuest(player, level, blockId);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide) {
            RegionManager.tick(event.level);
            CutscenePlayer.tick((ServerLevel) event.level);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player.level() instanceof ServerLevel level)) return;
        Player player = event.player;
        player.getCapability(ModCapabilities.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestData quest : QuestManager.getAll(level).values()) {
                if (data.isCompleted(quest.getId())) continue;
                if (quest.getObjectiveType() == QuestObjectiveType.REACH) {
                    String[] parts = quest.getTarget().split(" ");
                    if (parts.length >= 3) {
                        try {
                            double tx = Double.parseDouble(parts[0]);
                            double ty = Double.parseDouble(parts[1]);
                            double tz = Double.parseDouble(parts[2]);
                            if (player.distanceToSqr(tx, ty, tz) < 25.0) {
                                int progress = data.getProgress(quest.getId()) + 1;
                                data.setProgress(quest.getId(), progress);
                                if (progress >= quest.getRequiredCount()) {
                                    data.complete(quest.getId());
                                    executeReward(quest, player, level);
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        });
        ScriptGraphExecutor exec = ScriptGraphExecutor.getActive(player);
        if (exec != null) {
            exec.tick();
        }
    }

    private static void checkInteractQuest(Player player, ServerLevel level, String target) {
        player.getCapability(ModCapabilities.PLAYER_QUESTS).ifPresent(data -> {
            for (QuestData quest : QuestManager.getAll(level).values()) {
                if (data.isCompleted(quest.getId())) continue;
                if (quest.getObjectiveType() == QuestObjectiveType.INTERACT && target.equals(quest.getTarget())) {
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

    private static void executeReward(QuestData quest, Player player, ServerLevel level) {
        if (!quest.getRewardCommand().isEmpty()) {
            level.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withLevel(level).withPosition(player.position()),
                    quest.getRewardCommand().replace("@p", player.getGameProfile().getName())
            );
        }
    }
}