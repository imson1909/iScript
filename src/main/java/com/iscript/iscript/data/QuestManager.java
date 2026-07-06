package com.iscript.iscript.data;

import com.iscript.iscript.data.quest.QuestData;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;

public class QuestManager {
    public static QuestData get(ServerLevel level, String id) {
        return IScriptSavedData.get(level).getQuests().get(id);
    }
    public static void add(ServerLevel level, QuestData q) {
        IScriptSavedData.get(level).addQuest(q);
    }
    public static void remove(ServerLevel level, String id) {
        IScriptSavedData.get(level).removeQuest(id);
    }
    public static Map<String, QuestData> getAll(ServerLevel level) {
        return IScriptSavedData.get(level).getQuests();
    }
}
