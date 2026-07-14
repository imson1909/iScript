package com.iscript.iscript.data.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class NPCWaypointData {
    private final List<BlockPos> waypoints = new ArrayList<>();
    private boolean patrolLoop = false;
    private boolean lookAtPlayer = true;
    private int currentIndex = 0;
    private boolean forward = true;

    public List<BlockPos> getWaypoints() { return waypoints; }
    public boolean isPatrolLoop() { return patrolLoop; }
    public void setPatrolLoop(boolean loop) { this.patrolLoop = loop; }
    public boolean isLookAtPlayer() { return lookAtPlayer; }
    public void setLookAtPlayer(boolean look) { this.lookAtPlayer = look; }

    public BlockPos getNextWaypoint() {
        if (waypoints.isEmpty()) return null;
        BlockPos pos = waypoints.get(currentIndex);
        if (patrolLoop) {
            currentIndex = (currentIndex + 1) % waypoints.size();
        } else {
            if (forward) {
                currentIndex++;
                if (currentIndex >= waypoints.size()) {
                    currentIndex = waypoints.size() - 2;
                    forward = false;
                    if (currentIndex < 0) currentIndex = 0;
                }
            } else {
                currentIndex--;
                if (currentIndex < 0) {
                    currentIndex = 1;
                    forward = true;
                    if (waypoints.size() <= 1) currentIndex = 0;
                }
            }
        }
        return pos;
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos pos : waypoints) {
            CompoundTag p = new CompoundTag();
            p.putInt("x", pos.getX());
            p.putInt("y", pos.getY());
            p.putInt("z", pos.getZ());
            list.add(p);
        }
        tag.put("Waypoints", list);
        tag.putBoolean("PatrolLoop", patrolLoop);
        tag.putBoolean("LookAtPlayer", lookAtPlayer);
        tag.putInt("CurrentIndex", currentIndex);
        tag.putBoolean("Forward", forward);
    }

    public void load(CompoundTag tag) {
        waypoints.clear();
        ListTag list = tag.getList("Waypoints", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag p = list.getCompound(i);
            waypoints.add(new BlockPos(p.getInt("x"), p.getInt("y"), p.getInt("z")));
        }
        patrolLoop = tag.getBoolean("PatrolLoop");
        lookAtPlayer = tag.getBoolean("LookAtPlayer");
        currentIndex = tag.getInt("CurrentIndex");
        forward = tag.getBoolean("Forward");
    }
}
