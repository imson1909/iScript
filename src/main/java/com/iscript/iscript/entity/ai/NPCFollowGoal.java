package com.iscript.iscript.entity.ai;

import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;

public class NPCFollowGoal extends Goal {
    private final IScriptNPCEntity npc;
    private final double speed;
    private LivingEntity target;
    private int timer;

    public NPCFollowGoal(IScriptNPCEntity npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        String followId = npc.getNPCData().getFollowTarget();
        if (followId.isEmpty()) return false;

        target = findTarget(followId);
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;
        if (npc.getNPCData().getState() != com.iscript.iscript.data.npc.NPCState.FOLLOW) return false;
        return true;
    }

    @Override
    public void start() {
        this.timer = 0;
    }

    @Override
    public void stop() {
        target = null;
        npc.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null) return;

        npc.getLookControl().setLookAt(target);

        if (timer > 0) {
            timer--;
            return;
        }
        timer = 10;

        double distSqr = npc.distanceToSqr(target);
        if (distSqr > 144) {
            teleportNear(target);
            return;
        }

        if (distSqr > 4.0) {
            npc.getNavigation().moveTo(target, speed);
        } else {
            npc.getNavigation().stop();
        }
    }

    private LivingEntity findTarget(String followId) {
        if (followId.equals("@r")) {
            var players = npc.level().players();
            if (players.isEmpty()) return null;
            return players.get(npc.getRandom().nextInt(players.size()));
        }

        if (followId.startsWith("@")) {
            return npc.level().getNearestPlayer(npc, 64);
        }

        for (Player player : npc.level().players()) {
            if (player.getName().getString().equalsIgnoreCase(followId)) {
                return player;
            }
        }

        try {
            java.util.UUID uuid = java.util.UUID.fromString(followId);
            return npc.level().getPlayerByUUID(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void teleportNear(LivingEntity target) {
        double x = target.getX();
        double z = target.getZ();
        double y = target.getY();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) < 1 && Math.abs(dz) < 1) continue;
                BlockPos pos = new BlockPos((int)(x + dx), (int)y - 1, (int)(z + dz));
                if (npc.level().getBlockState(pos).isSolid()) {
                    npc.teleportTo(x + dx + 0.5, y, z + dz + 0.5);
                    npc.getNavigation().stop();
                    return;
                }
            }
        }
    }
}