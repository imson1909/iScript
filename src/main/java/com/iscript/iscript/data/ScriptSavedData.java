package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.script.ScriptGraphData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class ScriptSavedData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_script_graphs";
    private final Map<String, ScriptGraphData> graphs = new HashMap<>();

    public static ScriptSavedData load(CompoundTag tag) {
        ScriptSavedData data = new ScriptSavedData();
        ListTag list = tag.getList("Graphs", 10);
        for (int i = 0; i < list.size(); i++) {
            ScriptGraphData graph = new ScriptGraphData();
            graph.load(list.getCompound(i));
            data.graphs.put(graph.getId(), graph);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ScriptGraphData graph : graphs.values()) {
            CompoundTag t = new CompoundTag();
            graph.save(t);
            list.add(t);
        }
        tag.put("Graphs", list);
        return tag;
    }

    public Map<String, ScriptGraphData> getGraphs() { return graphs; }
    public ScriptGraphData getGraph(String id) { return graphs.get(id); }
    public void addGraph(ScriptGraphData graph) { graphs.put(graph.getId(), graph); setDirty(); }
    public void removeGraph(String id) { graphs.remove(id); setDirty(); }

    public static ScriptSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ScriptSavedData::load, ScriptSavedData::new, DATA_NAME);
    }
}