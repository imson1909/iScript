package com.iscript.iscript.entity.ai;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class NPCFactionAttackGoal extends Goal {
    private final IScriptNPCEntity npc;
    private LivingEntity target;
    private int scanCooldown;

    public NPCFactionAttackGoal(IScriptNPCEntity npc) {
        this.npc = npc;
    }

    @Override
    public boolean canUse() {
        NPCData data = npc.getNPCData();
        if (data == null) return false;
        if (!data.isAggressive() && data.getHostileFactions().isEmpty()) return false;

        if (scanCooldown > 0) {
            scanCooldown--;
            return target != null && target.isAlive();
        }

        scanCooldown = 20;
        findTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;
        if (isOutOfFallback()) return false;
        return true;
    }

    @Override
    public void start() {
        npc.setTarget(target);
    }

    @Override
    public void stop() {
        target = null;
        npc.setTarget(null);
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive() || isOutOfFallback()) {
            stop();
            return;
        }

        double distSqr = npc.distanceToSqr(target);
        NPCData data = npc.getNPCData();

        if (distSqr < 9.0) {
            npc.getNavigation().stop();
            npc.getLookControl().setLookAt(target);
            if (npc.attackCooldown <= 0) {
                npc.doHurtTarget(target);
                npc.attackCooldown = data.getDamageDelay();
            }
        } else {
            npc.getNavigation().moveTo(target, data.getMovementSpeed());
        }
    }

    private void findTarget() {
        NPCData data = npc.getNPCData();
        if (data == null) {
            target = null;
            return;
        }

        var players = npc.level().getEntitiesOfClass(Player.class, npc.getBoundingBox().inflate(data.getPathDistance()));

        for (Player p : players) {
            // === ИСПРАВЛЕНО: проверка creative и spectator ===
            if (p.isSpectator() || !p.isAlive() || p.isCreative()) continue;

            if (data.isAggressive()) {
                target = p;
                return;
            }

            String pf = p.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA)
                    .map(com.iscript.iscript.capability.PlayerData::getFaction).orElse("neutral");

            if (data.isHostileTo(pf)) {
                target = p;
                return;
            }
        }

        target = null;
    }

    private boolean isOutOfFallback() {
        NPCData data = npc.getNPCData();
        if (data == null) return false;
        if (!data.isHasPost() || data.getPostPosition() == null) return false;
        if (target == null) return false;

        double distSqr = target.distanceToSqr(
                data.getPostPosition().getX() + 0.5,
                data.getPostPosition().getY(),
                data.getPostPosition().getZ() + 0.5
        );

        return distSqr > data.getFallback() * data.getFallback();
    }
}