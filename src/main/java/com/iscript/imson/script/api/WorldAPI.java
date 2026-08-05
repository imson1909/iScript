package com.iscript.imson.script.api;

import com.iscript.imson.IScriptMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.graalvm.polyglot.HostAccess;

public class WorldAPI {
    private final ScriptAPI root;

    public WorldAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public void runCommand(String command) {
        root.level.getServer().getCommands().performPrefixedCommand(
                root.player.createCommandSourceStack().withLevel(root.level).withPosition(root.player.position()),
                command.replace("@p", root.player.getGameProfile().getName())
        );
    }

    @HostAccess.Export
    public void spawnEntity(String type, double x, double y, double z) {
        try {
            ResourceLocation id = new ResourceLocation(type);
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (entityType != null) {
                Entity entity = entityType.create(root.level);
                if (entity != null) {
                    entity.setPos(x, y, z);
                    root.level.addFreshEntity(entity);
                }
            }
        } catch (Exception e) {
            IScriptMod.LOGGER.error("Failed to spawn entity: {}", e.getMessage());
        }
    }

    @HostAccess.Export
    public void setBlock(String blockId, double x, double y, double z) {
        try {
            ResourceLocation id = new ResourceLocation(blockId);
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block != null) {
                root.level.setBlockAndUpdate(new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)), block.defaultBlockState());
            }
        } catch (Exception e) {
            IScriptMod.LOGGER.error("Failed to set block: {}", e.getMessage());
        }
    }

    @HostAccess.Export
    public String getBlock(double x, double y, double z) {
        return root.level.getBlockState(new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))).getBlock().builtInRegistryHolder().key().location().toString();
    }

    @HostAccess.Export
    public void playSound(String soundId, double x, double y, double z) {
        try {
            ResourceLocation id = new ResourceLocation(soundId);
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
            if (sound != null) {
                root.level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        } catch (Exception e) {
            IScriptMod.LOGGER.error("Failed to play sound: {}", e.getMessage());
        }
    }

    @HostAccess.Export
    public void particle(String particleId, double x, double y, double z) {
        try {
            ResourceLocation id = new ResourceLocation(particleId);
            var pType = ForgeRegistries.PARTICLE_TYPES.getValue(id);
            if (pType instanceof net.minecraft.core.particles.SimpleParticleType simple) {
                root.level.sendParticles(simple, x, y, z, 1, 0, 0, 0, 0);
            } else {
                root.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cParticle '" + particleId + "' is not a simple particle type"));
            }
        } catch (Exception e) {
            root.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cParticle error: " + e.getMessage()));
        }
    }

    @HostAccess.Export
    public void npcMove(int entityId, double x, double y, double z) {
        Entity entity = root.level.getEntity(entityId);
        if (entity instanceof com.iscript.imson.entity.IScriptNPCEntity npc) {
            npc.teleportTo(x, y, z);
        }
    }
}