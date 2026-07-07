package com.iscript.iscript.script;

import com.iscript.iscript.data.ScriptSavedData;
import com.iscript.iscript.data.script.ScriptGraphData;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;

public class ScriptGraphManager {
    public static ScriptGraphData get(ServerLevel level, String id) {
        return ScriptSavedData.get(level).getGraph(id);
    }
    public static void add(ServerLevel level, ScriptGraphData graph) {
        ScriptSavedData.get(level).addGraph(graph);
    }
    public static void remove(ServerLevel level, String id) {
        ScriptSavedData.get(level).removeGraph(id);
    }
    public static Map<String, ScriptGraphData> getAll(ServerLevel level) {
        return ScriptSavedData.get(level).getGraphs();
    }
}