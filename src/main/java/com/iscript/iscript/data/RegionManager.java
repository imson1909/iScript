package com.iscript.iscript.data;

import com.iscript.iscript.blockentities.RegionBlockEntity;
import com.iscript.iscript.data.CutsceneManager;
import com.iscript.iscript.data.cutscene.CutscenePlayer;
import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.data.region.RegionEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class RegionManager {

    public static void tick(Level level) {}

    public static void add(ServerLevel level, RegionData r) {
        for (RegionBlockEntity rbe : RegionBlockEntity.getAllInstances()) {
            if (rbe.getLevel() == level && rbe.getData().getId().equals(r.getId())) {
                rbe.setData(r);
                return;
            }
        }
    }

    public static void remove(ServerLevel level, String id) {
        for (RegionBlockEntity rbe : RegionBlockEntity.getAllInstances()) {
            if (rbe.getLevel() == level && rbe.getData().getId().equals(id)) {
                level.removeBlock(rbe.getBlockPos(), false);
                return;
            }
        }
    }

    public static RegionData get(ServerLevel level, String id) {
        for (RegionBlockEntity rbe : RegionBlockEntity.getAllInstances()) {
            if (rbe.getLevel() == level && rbe.getData().getId().equals(id)) {
                return rbe.getData();
            }
        }
        return null;
    }

    public static RegionData getByBlockPos(ServerLevel level, BlockPos pos) {
        for (RegionBlockEntity rbe : RegionBlockEntity.getAllInstances()) {
            if (rbe.getLevel() == level && rbe.getBlockPos().equals(pos)) {
                return rbe.getData();
            }
        }
        return null;
    }

    public static void applyEffect(RegionEffect effect, ServerPlayer player, ServerLevel level) {
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
                        false, false
                );
                player.addEffect(inst);
            }
            case DAMAGE -> player.hurt(player.level().damageSources().generic(), effect.getAmplifier() + 1);
            case HEAL -> player.heal(effect.getAmplifier() + 1);
            case TELEPORT -> {
                String[] c = effect.getValue().split(" ");
                if (c.length >= 3) try {
                    player.teleportTo(Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2]));
                } catch (NumberFormatException ignored) {}
            }
            case SOUND -> {
                try {
                    ResourceLocation id = new ResourceLocation(effect.getValue());
                    SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(id);
                    if (s != null) level.playSound(null, player.getX(), player.getY(), player.getZ(), s, SoundSource.PLAYERS, 1.0f, 1.0f);
                } catch (Exception ignored) {}
            }
            case CUTSCENE -> {
                if (!effect.getValue().isEmpty() && !CutscenePlayer.isPlaying(player)) {
                    var cutscene = CutsceneManager.get(level, effect.getValue());
                    if (cutscene != null) CutscenePlayer.play(player, cutscene);
                }
            }
            default -> {}
        }
    }
}