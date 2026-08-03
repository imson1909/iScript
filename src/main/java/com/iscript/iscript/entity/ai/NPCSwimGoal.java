package com.iscript.iscript.entity.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class NPCSwimGoal extends Goal {
    private final PathfinderMob mob;

    public NPCSwimGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return mob.isInWater() && mob.getFluidHeight(net.minecraft.tags.FluidTags.WATER) > mob.getFluidJumpThreshold();
    }

    @Override
    public void tick() {
        if (mob.getRandom().nextFloat() < 0.8f) {
            mob.getJumpControl().jump();
        }
    }
}