package com.iscript.iscript.data;

import com.iscript.iscript.capability.ModCapabilities;
import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.data.region.RegionEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class RegionManager {
    private static final Map<UUID, Set<String>> playerRegions = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> tickCounters = new HashMap<>();

    public static void tick(Level level) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        RegionSavedData data = RegionSavedData.get(serverLevel);

        for (Player player : serverLevel.players()) {
            BlockPos pos = player.blockPosition();
            UUID uuid = player.getUUID();
            Set<String> current = new HashSet<>();
            Map<String, Integer> playerCounters = tickCounters.computeIfAbsent(uuid, k -> new HashMap<>());

            for (RegionData region : data.getRegions().values()) {
                if (region.isInside(pos)) {
                    // Check faction requirement
                    if (!region.getRequiredFaction().isEmpty()) {
                        String playerFaction = player.getCapability(ModCapabilities.PLAYER_DATA)
                                .map(com.iscript.iscript.capability.PlayerData::getFaction).orElse("neutral");
                        if (!region.getRequiredFaction().equalsIgnoreCase(playerFaction)) {
                            continue;
                        }
                    }

                    current.add(region.getId());

                    if (!playerRegions.getOrDefault(uuid, Collections.emptySet()).contains(region.getId())) {
                        for (RegionEffect effect : region.getEnterEffects()) {
                            applyEffect(effect, (ServerPlayer) player, serverLevel);
                        }
                    }

                    int tickCount = playerCounters.getOrDefault(region.getId(), 0) + 1;
                    playerCounters.put(region.getId(), tickCount);
                    if (tickCount % region.getTickInterval() == 0) {
                        for (RegionEffect effect : region.getTickEffects()) {
                            applyEffect(effect, (ServerPlayer) player, serverLevel);
                        }
                    }
                }
            }

            if (playerRegions.containsKey(uuid)) {
                for (String old : playerRegions.get(uuid)) {
                    if (!current.contains(old)) {
                        RegionData region = data.getRegions().get(old);
                        if (region != null) {
                            for (RegionEffect effect : region.getExitEffects()) {
                                applyEffect(effect, (ServerPlayer) player, serverLevel);
                            }
                        }
                    }
                }
            }

            playerRegions.put(uuid, current);
        }
    }

    private static void applyEffect(RegionEffect effect, ServerPlayer player, ServerLevel level) {
        switch (effect.getType()) {
            case COMMAND -> {
                if (!effect.getValue().isEmpty()) {
                    level.getServer().getCommands().performPrefixedCommand(
                            player.createCommandSourceStack().withLevel(level).withPosition(player.position()),
                            effect.getValue().replace("@p", player.getGameProfile().getName())
                    );
                }
            }
            case MESSAGE -> player.sendSystemMessage(Component.literal(effect.getValue()));
            case POTION -> {
                MobEffectInstance inst = new MobEffectInstance(
                        switch (effect.getValue().toLowerCase()) {
                            case "speed" -> MobEffects.MOVEMENT_SPEED;
                            case "slowness" -> MobEffects.MOVEMENT_SLOWDOWN;
                            case "strength" -> MobEffects.DAMAGE_BOOST;
                            case "jump" -> MobEffects.JUMP;
                            case "regeneration" -> MobEffects.REGENERATION;
                            case "resistance" -> MobEffects.DAMAGE_RESISTANCE;
                            case "fire_resistance" -> MobEffects.FIRE_RESISTANCE;
                            case "water_breathing" -> MobEffects.WATER_BREATHING;
                            case "invisibility" -> MobEffects.INVISIBILITY;
                            case "night_vision" -> MobEffects.NIGHT_VISION;
                            case "weakness" -> MobEffects.WEAKNESS;
                            case "poison" -> MobEffects.POISON;
                            case "wither" -> MobEffects.WITHER;
                            case "health_boost" -> MobEffects.HEALTH_BOOST;
                            case "absorption" -> MobEffects.ABSORPTION;
                            case "saturation" -> MobEffects.SATURATION;
                            case "glowing" -> MobEffects.GLOWING;
                            case "levitation" -> MobEffects.LEVITATION;
                            case "luck" -> MobEffects.LUCK;
                            case "unluck" -> MobEffects.UNLUCK;
                            case "slow_falling" -> MobEffects.SLOW_FALLING;
                            case "conduit_power" -> MobEffects.CONDUIT_POWER;
                            case "dolphins_grace" -> MobEffects.DOLPHINS_GRACE;
                            case "bad_omen" -> MobEffects.BAD_OMEN;
                            case "hero_of_the_village" -> MobEffects.HERO_OF_THE_VILLAGE;
                            case "darkness" -> MobEffects.DARKNESS;
                            default -> MobEffects.MOVEMENT_SPEED;
                        },
                        effect.getDuration(),
                        effect.getAmplifier(),
                        false,
                        false
                );
                player.addEffect(inst);
            }
            case DAMAGE -> player.hurt(player.level().damageSources().generic(), effect.getAmplifier() + 1);
            case HEAL -> player.heal(effect.getAmplifier() + 1);
            case TELEPORT -> {
                String[] coords = effect.getValue().split(" ");
                if (coords.length >= 3) {
                    try {
                        double x = Double.parseDouble(coords[0]);
                        double y = Double.parseDouble(coords[1]);
                        double z = Double.parseDouble(coords[2]);
                        player.teleportTo(x, y, z);
                    } catch (NumberFormatException ignored) {}
                }
            }
            case SOUND -> {
                try {
                    ResourceLocation soundId = new ResourceLocation(effect.getValue());
                    SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
                    if (sound != null) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                } catch (Exception ignored) {}
            }
            default -> {}
        }
    }

    public static void add(ServerLevel level, RegionData r) {
        RegionSavedData.get(level).addRegion(r);
    }
    public static void remove(ServerLevel level, String id) {
        RegionSavedData.get(level).removeRegion(id);
    }
    public static RegionData get(ServerLevel level, String id) {
        return RegionSavedData.get(level).getRegions().get(id);
    }
}