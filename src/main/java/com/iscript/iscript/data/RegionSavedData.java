package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.region.RegionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class RegionSavedData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_regions";
    private final Map<String, RegionData> regions = new HashMap<>();

    public RegionSavedData() {}

    public static RegionSavedData load(CompoundTag tag) {
        RegionSavedData data = new RegionSavedData();
        ListTag list = tag.getList("Regions", 10);
        for (int i = 0; i < list.size(); i++) {
            RegionData r = new RegionData();
            r.load(list.getCompound(i));
            data.regions.put(r.getId(), r);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (RegionData r : regions.values()) {
            CompoundTag t = new CompoundTag();
            r.save(t);
            list.add(t);
        }
        tag.put("Regions", list);
        return tag;
    }

    public Map<String, RegionData> getRegions() { return regions; }
    public void addRegion(RegionData r) { regions.put(r.getId(), r); setDirty(); }
    public void removeRegion(String id) { regions.remove(id); setDirty(); }

    public static RegionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RegionSavedData::load, RegionSavedData::new, DATA_NAME);
    }
}