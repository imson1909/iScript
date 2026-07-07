package com.iscript.iscript.data;

import com.iscript.iscript.data.quest.QuestData;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;

public class QuestManager {
    public static QuestData get(ServerLevel level, String id) {
        return QuestSavedData.get(level).getQuests().get(id);
    }
    public static void add(ServerLevel level, QuestData q) {
        QuestSavedData.get(level).addQuest(q);
    }
    public static void remove(ServerLevel level, String id) {
        QuestSavedData.get(level).removeQuest(id);
    }
    public static Map<String, QuestData> getAll(ServerLevel level) {
        return QuestSavedData.get(level).getQuests();
    }
}