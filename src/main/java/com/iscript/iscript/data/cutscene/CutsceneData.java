package com.iscript.iscript.data.cutscene;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class CutsceneData {
    private String id = "";
    private String name = "Cutscene";
    private List<CutsceneAction> actions = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<CutsceneAction> getActions() { return actions; }

    public void save(CompoundTag tag) {
        tag.putString("Id", id);
        tag.putString("Name", name);
        ListTag list = new ListTag();
        for (CutsceneAction action : actions) {
            CompoundTag t = new CompoundTag();
            action.save(t);
            list.add(t);
        }
        tag.put("Actions", list);
    }

    public void load(CompoundTag tag) {
        id = tag.getString("Id");
        name = tag.getString("Name");
        actions.clear();
        ListTag list = tag.getList("Actions", 10);
        for (int i = 0; i < list.size(); i++) {
            CutsceneAction action = new CutsceneAction();
            action.load(list.getCompound(i));
            actions.add(action);
        }
    }
}