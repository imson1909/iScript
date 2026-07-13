package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.region.RegionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class IScriptSavedData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_data";
    private final Map<String, DialogData> dialogs = new HashMap<>();
    private final Map<String, QuestData> quests = new HashMap<>();
    private final Map<String, RegionData> regions = new HashMap<>();

    public IScriptSavedData() {}

    public static IScriptSavedData load(CompoundTag tag) {
        IScriptSavedData data = new IScriptSavedData();
        ListTag dialogsTag = tag.getList("Dialogs", 10);
        for (int i = 0; i < dialogsTag.size(); i++) {
            DialogData d = new DialogData();
            d.load(dialogsTag.getCompound(i));
            data.dialogs.put(d.getId(), d);
        }
        ListTag questsTag = tag.getList("Quests", 10);
        for (int i = 0; i < questsTag.size(); i++) {
            QuestData q = new QuestData();
            q.load(questsTag.getCompound(i));
            data.quests.put(q.getId(), q);
        }
        ListTag regionsTag = tag.getList("Regions", 10);
        for (int i = 0; i < regionsTag.size(); i++) {
            RegionData r = new RegionData();
            r.load(regionsTag.getCompound(i));
            data.regions.put(r.getId(), r);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag dialogsTag = new ListTag();
        for (DialogData d : dialogs.values()) {
            CompoundTag t = new CompoundTag();
            d.save(t);
            dialogsTag.add(t);
        }
        tag.put("Dialogs", dialogsTag);
        ListTag questsTag = new ListTag();
        for (QuestData q : quests.values()) {
            CompoundTag t = new CompoundTag();
            q.save(t);
            questsTag.add(t);
        }
        tag.put("Quests", questsTag);
        ListTag regionsTag = new ListTag();
        for (RegionData r : regions.values()) {
            CompoundTag t = new CompoundTag();
            r.save(t);
            regionsTag.add(t);
        }
        tag.put("Regions", regionsTag);
        return tag;
    }

    public Map<String, DialogData> getDialogs() { return dialogs; }
    public Map<String, QuestData> getQuests() { return quests; }
    public Map<String, RegionData> getRegions() { return regions; }

    public void addDialog(DialogData d) { dialogs.put(d.getId(), d); setDirty(); }
    public void removeDialog(String id) { dialogs.remove(id); setDirty(); }
    public void addQuest(QuestData q) { quests.put(q.getId(), q); setDirty(); }
    public void removeQuest(String id) { quests.remove(id); setDirty(); }
    public void addRegion(RegionData r) { regions.put(r.getId(), r); setDirty(); }
    public void removeRegion(String id) { regions.remove(id); setDirty(); }

    public static IScriptSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(IScriptSavedData::load, IScriptSavedData::new, DATA_NAME);
    }
}
