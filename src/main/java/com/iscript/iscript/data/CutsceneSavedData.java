package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.cutscene.CutsceneData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class CutsceneSavedData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_cutscenes";
    private final Map<String, CutsceneData> cutscenes = new HashMap<>();

    public static CutsceneSavedData load(CompoundTag tag) {
        CutsceneSavedData data = new CutsceneSavedData();
        ListTag list = tag.getList("Cutscenes", 10);
        for (int i = 0; i < list.size(); i++) {
            CutsceneData c = new CutsceneData();
            c.load(list.getCompound(i));
            data.cutscenes.put(c.getId(), c);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (CutsceneData c : cutscenes.values()) {
            CompoundTag t = new CompoundTag();
            c.save(t);
            list.add(t);
        }
        tag.put("Cutscenes", list);
        return tag;
    }

    public Map<String, CutsceneData> getCutscenes() { return cutscenes; }
    public CutsceneData get(String id) { return cutscenes.get(id); }
    public void add(CutsceneData c) { cutscenes.put(c.getId(), c); setDirty(); }
    public void remove(String id) { cutscenes.remove(id); setDirty(); }

    public static CutsceneSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CutsceneSavedData::load, CutsceneSavedData::new, DATA_NAME);
    }
}