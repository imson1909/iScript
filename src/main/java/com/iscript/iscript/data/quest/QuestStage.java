package com.iscript.iscript.data.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class QuestStage {
    private String id = "";
    private String description = "";
    private final List<QuestObjective> objectives = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<QuestObjective> getObjectives() { return objectives; }

    public void load(CompoundTag tag) {
        this.id = tag.getString("Id");
        this.description = tag.getString("Description");
        this.objectives.clear();
        ListTag list = tag.getList("Objectives", 10);
        for (int i = 0; i < list.size(); i++) {
            QuestObjective obj = new QuestObjective();
            obj.load(list.getCompound(i));
            this.objectives.add(obj);
        }
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", this.id);
        tag.putString("Description", this.description);
        ListTag list = new ListTag();
        for (QuestObjective obj : this.objectives) {
            CompoundTag t = new CompoundTag();
            obj.save(t);
            list.add(t);
        }
        tag.put("Objectives", list);
    }

    public QuestStage copy() {
        QuestStage copy = new QuestStage();
        copy.id = this.id;
        copy.description = this.description;
        for (QuestObjective obj : this.objectives) {
            copy.objectives.add(obj.copy());
        }
        return copy;
    }

    public QuestStage copyTemplate() {
        QuestStage copy = new QuestStage();
        copy.id = this.id;
        copy.description = this.description;
        for (QuestObjective obj : this.objectives) {
            copy.objectives.add(obj.copyTemplate());
        }
        return copy;
    }
}