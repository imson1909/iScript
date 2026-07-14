package com.iscript.iscript.data.region;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class RegionData {
    private String id = "";
    private String name = "Region";
    private Vec3 pos1 = new Vec3(-2.5, -2.5, -2.5);
    private Vec3 pos2 = new Vec3(2.5, 2.5, 2.5);
    private List<RegionEffect> enterEffects = new ArrayList<>();
    private List<RegionEffect> exitEffects = new ArrayList<>();
    private List<RegionEffect> tickEffects = new ArrayList<>();
    private int tickInterval = 20;
    private String requiredFaction = "";
    private String onEnterCutsceneId = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Vec3 getPos1() { return pos1; }
    public void setPos1(Vec3 pos) { this.pos1 = pos; }
    public Vec3 getPos2() { return pos2; }
    public void setPos2(Vec3 pos) { this.pos2 = pos; }
    public List<RegionEffect> getEnterEffects() { return enterEffects; }
    public List<RegionEffect> getExitEffects() { return exitEffects; }
    public List<RegionEffect> getTickEffects() { return tickEffects; }
    public int getTickInterval() { return tickInterval; }
    public void setTickInterval(int interval) { this.tickInterval = interval; }
    public String getRequiredFaction() { return requiredFaction; }
    public void setRequiredFaction(String faction) { this.requiredFaction = faction; }
    public String getOnEnterCutsceneId() { return onEnterCutsceneId; }
    public void setOnEnterCutsceneId(String id) { this.onEnterCutsceneId = id != null ? id : ""; }

    public double getSizeX() { return Math.abs(pos2.x - pos1.x); }
    public double getSizeY() { return Math.abs(pos2.y - pos1.y); }
    public double getSizeZ() { return Math.abs(pos2.z - pos1.z); }

    public AABB getBounds(BlockPos anchor) {
        double minX = Math.min(pos1.x, pos2.x);
        double minY = Math.min(pos1.y, pos2.y);
        double minZ = Math.min(pos1.z, pos2.z);
        double maxX = Math.max(pos1.x, pos2.x);
        double maxY = Math.max(pos1.y, pos2.y);
        double maxZ = Math.max(pos1.z, pos2.z);

        return new AABB(
                anchor.getX() + minX,
                anchor.getY() + minY,
                anchor.getZ() + minZ,
                anchor.getX() + maxX,
                anchor.getY() + maxY,
                anchor.getZ() + maxZ
        );
    }

    public boolean isInside(BlockPos anchor, Vec3 pos) {
        return getBounds(anchor).contains(pos);
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", this.id);
        tag.putString("Name", this.name);
        tag.putDouble("Pos1X", pos1.x);
        tag.putDouble("Pos1Y", pos1.y);
        tag.putDouble("Pos1Z", pos1.z);
        tag.putDouble("Pos2X", pos2.x);
        tag.putDouble("Pos2Y", pos2.y);
        tag.putDouble("Pos2Z", pos2.z);
        tag.putInt("TickInterval", tickInterval);
        tag.putString("RequiredFaction", requiredFaction);
        tag.putString("OnEnterCutscene", onEnterCutsceneId);

        saveEffects(tag, "EnterEffects", enterEffects);
        saveEffects(tag, "ExitEffects", exitEffects);
        saveEffects(tag, "TickEffects", tickEffects);
    }

    private void saveEffects(CompoundTag tag, String key, List<RegionEffect> effects) {
        ListTag list = new ListTag();
        for (RegionEffect e : effects) {
            CompoundTag t = new CompoundTag();
            e.save(t);
            list.add(t);
        }
        tag.put(key, list);
    }

    public void load(CompoundTag tag) {
        this.id = tag.getString("Id");
        this.name = tag.getString("Name");
        this.pos1 = new Vec3(tag.getDouble("Pos1X"), tag.getDouble("Pos1Y"), tag.getDouble("Pos1Z"));
        this.pos2 = new Vec3(tag.getDouble("Pos2X"), tag.getDouble("Pos2Y"), tag.getDouble("Pos2Z"));
        this.tickInterval = tag.getInt("TickInterval");
        this.requiredFaction = tag.getString("RequiredFaction");
        this.onEnterCutsceneId = tag.getString("OnEnterCutscene");

        this.enterEffects = loadEffects(tag, "EnterEffects");
        this.exitEffects = loadEffects(tag, "ExitEffects");
        this.tickEffects = loadEffects(tag, "TickEffects");
    }

    private List<RegionEffect> loadEffects(CompoundTag tag, String key) {
        List<RegionEffect> list = new ArrayList<>();
        ListTag nbtList = tag.getList(key, 10);
        for (int i = 0; i < nbtList.size(); i++) {
            RegionEffect e = new RegionEffect();
            e.load(nbtList.getCompound(i));
            list.add(e);
        }
        return list;
    }

    public CompoundTag toNetworkTag() {
        CompoundTag tag = new CompoundTag();
        save(tag);
        return tag;
    }

    public static RegionData fromNetworkTag(CompoundTag tag) {
        RegionData r = new RegionData();
        if (tag != null) r.load(tag);
        return r;
    }
}