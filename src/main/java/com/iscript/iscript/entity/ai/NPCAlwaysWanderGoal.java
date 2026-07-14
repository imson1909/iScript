package com.iscript.iscript.entity.ai;

import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import com.iscript.iscript.data.npc.NPCData;

import java.util.EnumSet;

public class NPCAlwaysWanderGoal extends Goal {
    private final IScriptNPCEntity npc;
    private final double speed;

    public NPCAlwaysWanderGoal(IScriptNPCEntity npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        NPCData data = npc.getNPCData();
        return data != null && data.isAlwaysWander();
    }

    @Override
    public boolean canContinueToUse() {
        NPCData data = npc.getNPCData();
        return data != null && data.isAlwaysWander();
    }

    @Override
    public void tick() {
        if (npc.getNavigation().isDone()) {
            Vec3 pos = getPosition();
            if (pos != null) {
                npc.getNavigation().moveTo(pos.x, pos.y, pos.z, speed);
            }
        }
    }

    private Vec3 getPosition() {
        for (int i = 0; i < 10; i++) {
            Vec3 random = new Vec3(
                    npc.getX() + (npc.getRandom().nextDouble() - 0.5) * 16,
                    npc.getY(),
                    npc.getZ() + (npc.getRandom().nextDouble() - 0.5) * 16
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