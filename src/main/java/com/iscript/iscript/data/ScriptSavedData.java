package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.script.ScriptGraphData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScriptSavedData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_script_graphs";
    private final Map<String, ScriptGraphData> graphs = new HashMap<>();

    public ScriptSavedData() {}

    public static ScriptSavedData load(CompoundTag tag) {
        ScriptSavedData data = new ScriptSavedData();
        ListTag list = tag.getList("Graphs", 10);
        for (int i = 0; i < list.size(); i++) {
            ScriptGraphData graph = new ScriptGraphData();
            graph.load(list.getCompound(i));
            if (graph.getId() != null && !graph.getId().isEmpty()) {
                data.graphs.put(graph.getId(), graph);
            }
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

    public ScriptGraphData getGraph(String id) {
        return graphs.get(id);
    }

    public Map<String, ScriptGraphData> getGraphs() {
        return Collections.unmodifiableMap(graphs);
    }

    public void addGraph(ScriptGraphData graph) {
        if (graph == null || graph.getId() == null || graph.getId().isEmpty()) return;
        graphs.put(graph.getId(), graph);
        setDirty();
    }

    public void removeGraph(String id) {
        if (id == null) return;
        graphs.remove(id);
        setDirty();
    }

    public static ScriptSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ScriptSavedData::load, ScriptSavedData::new, DATA_NAME);
    }
}