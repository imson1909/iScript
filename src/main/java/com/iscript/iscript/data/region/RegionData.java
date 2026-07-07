package com.iscript.iscript.data.region;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class RegionData {
    private String id = "";
    private String name = "Region";
    private BlockPos pos1 = BlockPos.ZERO;
    private BlockPos pos2 = BlockPos.ZERO;
    private List<RegionEffect> enterEffects = new ArrayList<>();
    private List<RegionEffect> exitEffects = new ArrayList<>();
    private List<RegionEffect> tickEffects = new ArrayList<>();
    private int tickInterval = 20;
    private boolean showBorder = false;
    private String requiredFaction = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BlockPos getPos1() { return pos1; }
    public void setPos1(BlockPos pos) { this.pos1 = pos; }
    public BlockPos getPos2() { return pos2; }
    public void setPos2(BlockPos pos) { this.pos2 = pos; }
    public List<RegionEffect> getEnterEffects() { return enterEffects; }
    public List<RegionEffect> getExitEffects() { return exitEffects; }
    public List<RegionEffect> getTickEffects() { return tickEffects; }
    public int getTickInterval() { return tickInterval; }
    public void setTickInterval(int interval) { this.tickInterval = interval; }
    public boolean isShowBorder() { return showBorder; }
    public void setShowBorder(boolean show) { this.showBorder = show; }
    public String getRequiredFaction() { return requiredFaction; }
    public void setRequiredFaction(String faction) { this.requiredFaction = faction; }

    public boolean isInside(BlockPos pos) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX &&
               pos.getY() >= minY && pos.getY() <= maxY &&
               pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", this.id);
        tag.putString("Name", this.name);
        CompoundTag p1 = new CompoundTag();
        p1.putInt("x", pos1.getX());
        p1.putInt("y", pos1.getY());
        p1.putInt("z", pos1.getZ());
        tag.put("Pos1", p1);
        CompoundTag p2 = new CompoundTag();
        p2.putInt("x", pos2.getX());
        p2.putInt("y", pos2.getY());
        p2.putInt("z", pos2.getZ());
        tag.put("Pos2", p2);
        tag.putInt("TickInterval", tickInterval);
        tag.putBoolean("ShowBorder", showBorder);
        tag.putString("RequiredFaction", requiredFaction);

        ListTag enter = new ListTag();
        for (RegionEffect e : enterEffects) {
            CompoundTag t = new CompoundTag();
            e.save(t);
            enter.add(t);
        }
        tag.put("EnterEffects", enter);

        ListTag exit = new ListTag();
        for (RegionEffect e : exitEffects) {
            CompoundTag t = new CompoundTag();
            e.save(t);
            exit.add(t);
        }
        tag.put("ExitEffects", exit);

        ListTag tick = new ListTag();
        for (RegionEffect e : tickEffects) {
            CompoundTag t = new CompoundTag();
            e.save(t);
            tick.add(t);
        }
        tag.put("TickEffects", tick);
    }

    public void load(CompoundTag tag) {
        this.id = tag.getString("Id");
        this.name = tag.getString("Name");
        CompoundTag p1 = tag.getCompound("Pos1");
        this.pos1 = new BlockPos(p1.getInt("x"), p1.getInt("y"), p1.getInt("z"));
        CompoundTag p2 = tag.getCompound("Pos2");
        this.pos2 = new BlockPos(p2.getInt("x"), p2.getInt("y"), p2.getInt("z"));
        this.tickInterval = tag.getInt("TickInterval");
        this.showBorder = tag.getBoolean("ShowBorder");
        this.requiredFaction = tag.getString("RequiredFaction");

        this.enterEffects.clear();
        ListTag enter = tag.getList("EnterEffects", 10);
        for (int i = 0; i < enter.size(); i++) {
            RegionEffect e = new RegionEffect();
            e.load(enter.getCompound(i));
            this.enterEffects.add(e);
        }

        this.exitEffects.clear();
        ListTag exit = tag.getList("ExitEffects", 10);
        for (int i = 0; i < exit.size(); i++) {
            RegionEffect e = new RegionEffect();
            e.load(exit.getCompound(i));
            this.exitEffects.add(e);
        }

        this.tickEffects.clear();
        ListTag tick = tag.getList("TickEffects", 10);
        for (int i = 0; i < tick.size(); i++) {
            RegionEffect e = new RegionEffect();
            e.load(tick.getCompound(i));
            this.tickEffects.add(e);
        }
    }
}
