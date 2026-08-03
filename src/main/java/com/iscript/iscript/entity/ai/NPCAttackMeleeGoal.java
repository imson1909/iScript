package com.iscript.iscript.entity.ai;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class NPCAttackMeleeGoal extends Goal {
    private final IScriptNPCEntity npc;
    private final double speed;
    private final boolean longMemory;
    private int attackTick;
    private int delay;

    public NPCAttackMeleeGoal(IScriptNPCEntity npc, double speed, boolean longMemory) {
        this.npc = npc;
        this.speed = speed;
        this.longMemory = longMemory;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = npc.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!longMemory) {
            if (npc.getNavigation().isDone()) return false;
        }
        return true;
    }

    @Override
    public void start() {
        NPCData data = npc.getNPCData();
        this.delay = data.getDamageDelay();
        this.attackTick = 0;
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = npc.getTarget();
        if (target == null) return;

        npc.getLookControl().setLookAt(target);

        double distSqr = npc.distanceToSqr(target);
        double reach = getAttackReachSqr(target);

        if (distSqr <= reach) {
            npc.getNavigation().stop();
            if (attackTick <= 0) {
                attackTick = delay;
                npc.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                npc.doHurtTarget(target);
            }
        } else {
            npc.getNavigation().moveTo(target, speed);
        }

        if (attackTick > 0) attackTick--;
    }

    protected double getAttackReachSqr(LivingEntity target) {
        return npc.getBbWidth() * 2.0f * npc.getBbWidth() * 2.0f + target.getBbWidth();
    }
}