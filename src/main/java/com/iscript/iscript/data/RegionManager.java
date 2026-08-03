package com.iscript.iscript.data;

import com.iscript.iscript.blockentities.RegionBlockEntity;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.Map;

public class RegionManager {
    private static final Map<String, MobEffect> EFFECT_MAP = new HashMap<>();

    static {
        EFFECT_MAP.put("speed", MobEffects.MOVEMENT_SPEED);
        EFFECT_MAP.put("slowness", MobEffects.MOVEMENT_SLOWDOWN);
        EFFECT_MAP.put("strength", MobEffects.DAMAGE_BOOST);
        EFFECT_MAP.put("jump", MobEffects.JUMP);
        EFFECT_MAP.put("regeneration", MobEffects.REGENERATION);
        EFFECT_MAP.put("resistance", MobEffects.DAMAGE_RESISTANCE);
        EFFECT_MAP.put("fire_resistance", MobEffects.FIRE_RESISTANCE);
        EFFECT_MAP.put("water_breathing", MobEffects.WATER_BREATHING);
        EFFECT_MAP.put("invisibility", MobEffects.INVISIBILITY);
        EFFECT_MAP.put("night_vision", MobEffects.NIGHT_VISION);
        EFFECT_MAP.put("weakness", MobEffects.WEAKNESS);
        EFFECT_MAP.put("poison", MobEffects.POISON);
        EFFECT_MAP.put("wither", MobEffects.WITHER);
        EFFECT_MAP.put("health_boost", MobEffects.HEALTH_BOOST);
        EFFECT_MAP.put("absorption", MobEffects.ABSORPTION);
        EFFECT_MAP.put("saturation", MobEffects.SATURATION);
        EFFECT_MAP.put("glowing", MobEffects.GLOWING);
        EFFECT_MAP.put("levitation", MobEffects.LEVITATION);
        EFFECT_MAP.put("luck", MobEffects.LUCK);
        EFFECT_MAP.put("unluck", MobEffects.UNLUCK);
        EFFECT_MAP.put("slow_falling", MobEffects.SLOW_FALLING);
        EFFECT_MAP.put("conduit_power", MobEffects.CONDUIT_POWER);
        EFFECT_MAP.put("dolphins_grace", MobEffects.DOLPHINS_GRACE);
        EFFECT_MAP.put("bad_omen", MobEffects.BAD_OMEN);
        EFFECT_MAP.put("hero_of_the_village", MobEffects.HERO_OF_THE_VILLAGE);
        EFFECT_MAP.put("darkness", MobEffects.DARKNESS);
    }

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
        return DataAccess.region(id);
    }

    public static RegionData getByBlockPos(ServerLevel level, BlockPos pos) {
        for (RegionBlockEntity rbe : RegionBlockEntity.getAllInstances()) {
            if (rbe.getLevel() == level && rbe.getBlockPos().equals(pos)) return rbe.getData();
        }
        return null;
    }

    public static void applyEffect(RegionEffect effect, ServerPlayer player, ServerLevel level) {
        switch (effect.getType()) {
            case COMMAND -> {
                if (effect.getValue().isEmpty()) return;
                level.getServer().getCommands().performPrefixedCommand(
                        player.createCommandSourceStack().withLevel(level).withPosition(player.position()),
                        effect.getValue().replace("@p", player.getGameProfile().getName()));
            }
            case MESSAGE -> player.sendSystemMessage(Component.literal(effect.getValue()));
            case POTION -> {
                MobEffect mob = EFFECT_MAP.getOrDefault(effect.getValue().toLowerCase(), MobEffects.MOVEMENT_SPEED);
                player.addEffect(new MobEffectInstance(mob, effect.getDuration(), effect.getAmplifier(), false, false));
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
                    var cutscene = DataAccess.cutscene(effect.getValue());
                    if (cutscene != null) CutscenePlayer.play(player, cutscene);
                }
            }
            default -> {}
        }
    }
}