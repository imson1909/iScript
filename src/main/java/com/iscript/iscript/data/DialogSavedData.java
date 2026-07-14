package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.dialog.DialogData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class DialogSavedData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_dialogs";
    private final Map<String, DialogData> dialogs = new HashMap<>();

    public DialogSavedData() {}

    public static DialogSavedData load(CompoundTag tag) {
        DialogSavedData data = new DialogSavedData();
        ListTag list = tag.getList("Dialogs", 10);
        for (int i = 0; i < list.size(); i++) {
            DialogData d = new DialogData();
            d.load(list.getCompound(i));
            data.dialogs.put(d.getId(), d);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (DialogData d : dialogs.values()) {
            CompoundTag t = new CompoundTag();
            d.save(t);
            list.add(t);
        }
        tag.put("Dialogs", list);
        return tag;
    }

    public Map<String, DialogData> getDialogs() { return dialogs; }
    public void addDialog(DialogData d) { dialogs.put(d.getId(), d); setDirty(); }
    public void removeDialog(String id) { dialogs.remove(id); setDirty(); }

    public static DialogSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DialogSavedData::load, DialogSavedData::new, DATA_NAME);
    }
}