package com.iscript.iscript.data;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.dialog.DialogGraphData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class DialogGraphSavedData extends SavedData {
    public static final String DATA_NAME = IScriptMod.MOD_ID + "_dialog_graphs";
    private final Map<String, DialogGraphData> graphs = new HashMap<>();

    public DialogGraphSavedData() {}

    public static DialogGraphSavedData load(CompoundTag tag) {
        DialogGraphSavedData data = new DialogGraphSavedData();
        ListTag list = tag.getList("Graphs", 10);
        for (int i = 0; i < list.size(); i++) {
            DialogGraphData graph = new DialogGraphData();
            graph.load(list.getCompound(i));
            data.graphs.put(graph.getId(), graph);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (DialogGraphData graph : graphs.values()) {
            CompoundTag t = new CompoundTag();
            graph.save(t);
            list.add(t);
        }
        tag.put("Graphs", list);
        return tag;
    }

    public Map<String, DialogGraphData> getGraphs() { return graphs; }
    public DialogGraphData getGraph(String id) { return graphs.get(id); }
    public void addGraph(DialogGraphData graph) { graphs.put(graph.getId(), graph); setDirty(); }
    public void removeGraph(String id) { graphs.remove(id); setDirty(); }

    public static DialogGraphSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DialogGraphSavedData::load, DialogGraphSavedData::new, DATA_NAME);
    }
}