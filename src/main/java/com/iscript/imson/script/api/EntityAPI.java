package com.iscript.imson.script.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.graalvm.polyglot.HostAccess;

public class EntityAPI {
    private final Entity entity;

    public EntityAPI(Entity entity) {
        this.entity = entity;
    }

    @HostAccess.Export
    public double[] getPosition() {
        return new double[] { entity.getX(), entity.getY(), entity.getZ() };
    }

    @HostAccess.Export
    public void setPosition(double x, double y, double z) {
        entity.setPos(x, y, z);
    }

    @HostAccess.Export
    public double[] getRotations() {
        return new double[] { entity.getYRot(), entity.getXRot(), 0 };
    }

    @HostAccess.Export
    public void setRotations(float yaw, float pitch) {
        entity.setYRot(yaw);
        entity.setXRot(pitch);
    }

    @HostAccess.Export
    public void setSpeed(double speed) {
        if (entity instanceof LivingEntity le) {
            le.setSpeed((float) speed);
        }
    }

    @HostAccess.Export
    public double getHp() {
        if (entity instanceof LivingEntity le) {
            return le.getHealth();
        }
        return 0;
    }

    @HostAccess.Export
    public double getMaxHp() {
        if (entity instanceof LivingEntity le) {
            return le.getMaxHealth();
        }
        return 0;
    }

    @HostAccess.Export
    public void remove() {
        entity.discard();
    }

    @HostAccess.Export
    public boolean isEntity() {
        return true;
    }

    @HostAccess.Export
    public boolean isWalking() {
        return entity.getDeltaMovement().horizontalDistanceSqr() > 0.001;
    }

    @HostAccess.Export
    public boolean isSprinting() {
        return entity.isSprinting();
    }

    @HostAccess.Export
    public boolean isPlayer() {
        return entity instanceof Player;
    }

    @HostAccess.Export
    public boolean isNpc() {
        return entity instanceof com.iscript.imson.entity.IScriptNPCEntity;
    }

    @HostAccess.Export
    public void setMorph(String modelId) {
        if (entity instanceof Player p) {
            p.getCapability(com.iscript.imson.morph.MorphData.CAPABILITY).ifPresent(data -> {
                data.setModelId(modelId);
                data.setTextureId(modelId);
                data.setMorphed(true);
                data.setCurrentAnimation("animation." + modelId + ".idle");
                data.resetAnimationTick();
                com.iscript.imson.network.IScriptNetwork.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) p),
                        com.iscript.imson.morph.network.MorphSyncPacket.create(p)
                );
            });
        }
    }

    @HostAccess.Export
    public void setLocalMorph(String modelId, Entity target) {
        if (target instanceof Player p) {
            p.getCapability(com.iscript.imson.morph.MorphData.CAPABILITY).ifPresent(data -> {
                data.setModelId(modelId);
                data.setTextureId(modelId);
                data.setMorphed(true);
                data.setCurrentAnimation("animation." + modelId + ".idle");
                data.resetAnimationTick();
                com.iscript.imson.network.IScriptNetwork.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                        com.iscript.imson.morph.network.MorphSyncPacket.create(p)
                );
            });
        }
    }
}