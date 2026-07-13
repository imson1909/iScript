package com.iscript.iscript.entity.ai;

import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class NPCLookAtPlayerGoal extends Goal {
    private final IScriptNPCEntity npc;
    private final float distance;
    private Player target;
    private int lookTime;

    public NPCLookAtPlayerGoal(IScriptNPCEntity npc, float distance) {
        this.npc = npc;
        this.distance = distance;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!npc.getNPCData().isLookAtPlayer()) return false;
        target = npc.level().getNearestPlayer(npc, distance);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;
        if (npc.distanceToSqr(target) > distance * distance) return false;
        return lookTime > 0;
    }

    @Override
    public void start() {
        lookTime = 40 + npc.getRandom().nextInt(40);
    }

    @Override
    public void stop() {
        target = null;
    }

    @Override
    public void tick() {
        if (target != null) {
            npc.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
            lookTime--;
        }
    }
}