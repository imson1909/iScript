package com.iscript.iscript.entity.ai;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class NPCReturnToPostGoal extends Goal {
    private final IScriptNPCEntity npc;
    private final double speed;
    private int timer;

    public NPCReturnToPostGoal(IScriptNPCEntity npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        NPCData data = npc.getNPCData();
        if (!data.isHasPost() || data.getPostPosition() == null) return false;
        if (npc.getTarget() != null && npc.getTarget().isAlive()) return false;

        double distSqr = npc.distanceToSqr(
                data.getPostPosition().getX() + 0.5,
                data.getPostPosition().getY(),
                data.getPostPosition().getZ() + 0.5
        );

        return distSqr > data.getPostRadius() * data.getPostRadius();
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.getTarget() != null && npc.getTarget().isAlive()) return false;
        return canUse();
    }

    @Override
    public void start() {
        this.timer = 0;
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
    }

    @Override
    public void tick() {
        NPCData data = npc.getNPCData();
        BlockPos post = data.getPostPosition();

        npc.getLookControl().setLookAt(
                post.getX() + 0.5,
                post.getY() + npc.getEyeHeight(),
                post.getZ() + 0.5
        );

        if (timer > 0) {
            timer--;
            return;
        }
        timer = 10;

        npc.getNavigation().moveTo(
                post.getX() + 0.5,
                post.getY(),
                post.getZ() + 0.5,
                speed
        );
    }
}