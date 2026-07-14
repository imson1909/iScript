package com.iscript.iscript.data;

import com.iscript.iscript.data.cutscene.CutsceneData;
import net.minecraft.server.level.ServerLevel;

public class CutsceneManager {
    public static CutsceneData get(ServerLevel level, String id) {
        return CutsceneSavedData.get(level).get(id);
    }
    public static void add(ServerLevel level, CutsceneData c) {
        CutsceneSavedData.get(level).add(c);
    }
    public static void remove(ServerLevel level, String id) {
        CutsceneSavedData.get(level).remove(id);
    }
    public static java.util.Map<String, CutsceneData> getAll(ServerLevel level) {
        return CutsceneSavedData.get(level).getCutscenes();
    }
}