package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.quest.QuestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class QuestSavedData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_quests";
    private final Map<String, QuestData> quests = new HashMap<>();

    public QuestSavedData() {}

    public static QuestSavedData load(CompoundTag tag) {
        QuestSavedData data = new QuestSavedData();
        ListTag list = tag.getList("Quests", 10);
        for (int i = 0; i < list.size(); i++) {
            QuestData q = new QuestData();
            q.load(list.getCompound(i));
            data.quests.put(q.getId(), q);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (QuestData q : quests.values()) {
            CompoundTag t = new CompoundTag();
            q.save(t);
            list.add(t);
        }
        tag.put("Quests", list);
        return tag;
    }

    public Map<String, QuestData> getQuests() { return quests; }
    public void addQuest(QuestData q) { quests.put(q.getId(), q); setDirty(); }
    public void removeQuest(String id) { quests.remove(id); setDirty(); }

    public static QuestSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(QuestSavedData::load, QuestSavedData::new, DATA_NAME);
    }
}