package com.iscript.iscript.entity.ai;

import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class NPCWanderGoal extends Goal {
    private final IScriptNPCEntity npc;
    private final double speed;
    private double targetX;
    private double targetY;
    private double targetZ;

    public NPCWanderGoal(IScriptNPCEntity npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!npc.getNPCData().isWander()) return false;
        if (npc.getTarget() != null && npc.getTarget().isAlive()) return false;
        if (npc.getRandom().nextInt(120) != 0) return false;

        Vec3 pos = getPosition();
        if (pos == null) return false;

        targetX = pos.x;
        targetY = pos.y;
        targetZ = pos.z;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !npc.getNavigation().isDone();
    }

    @Override
    public void start() {
        npc.getNavigation().moveTo(targetX, targetY, targetZ, speed);
    }

    private Vec3 getPosition() {
        for (int i = 0; i < 10; i++) {
            Vec3 random = new Vec3(
                    npc.getX() + (npc.getRandom().nextDouble() - 0.5) * 20,
                    npc.getY(),
                    npc.getZ() + (npc.getRandom().nextDouble() - 0.5) * 20
            );
            BlockPos pos = new BlockPos((int) random.x, (int) random.y, (int) random.z);
            if (npc.level().getBlockState(pos.below()).isSolid()
                    && npc.level().isEmptyBlock(pos)
                    && npc.level().isEmptyBlock(pos.above())) {
                return random;
            }
        }
        return null;
    }
}