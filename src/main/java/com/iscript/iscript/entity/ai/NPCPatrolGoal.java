package com.iscript.iscript.entity.ai;

import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

public class NPCPatrolGoal extends Goal {
    private final IScriptNPCEntity npc;
    private final double speed;
    private int timer;
    private int index;
    private int direction = 1;

    public NPCPatrolGoal(IScriptNPCEntity npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        NPCData data = npc.getNPCData();
        if (data == null) return false;
        if (npc.getTarget() != null && npc.getTarget().isAlive()) return false;
        List<BlockPos> points = data.getPatrolPoints();
        if (points.isEmpty()) return false;
        if (index < 0 || index >= points.size()) index = 0;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.getTarget() != null && npc.getTarget().isAlive()) return false;
        NPCData data = npc.getNPCData();
        return data != null && !data.getPatrolPoints().isEmpty();
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
        if (data == null) return;
        List<BlockPos> points = data.getPatrolPoints();

        if (index < 0 || index >= points.size()) {
            index = 0;
            if (points.isEmpty()) return;
        }

        BlockPos pos = points.get(index);

        if (npc.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 2.0) {
            int next = index + direction;

            if (data.isPatrolLoop()) {
                index = (index + 1) % points.size();
            } else {
                if (next < 0 || next >= points.size()) {
                    direction *= -1;
                }
                index += direction;
                if (index < 0) index = 0;
                if (index >= points.size()) index = points.size() - 1;
            }

            timer = 0;
        }

        npc.getLookControl().setLookAt(
                pos.getX() + 0.5,
                pos.getY() + npc.getEyeHeight(),
                pos.getZ() + 0.5
        );

        if (timer > 0) {
            timer--;
            return;
        }
        timer = 10;

        npc.getNavigation().moveTo(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                speed
        );
    }
}