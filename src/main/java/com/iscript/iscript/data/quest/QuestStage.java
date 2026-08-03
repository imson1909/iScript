package com.iscript.iscript.data.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iscript.iscript.data.DataObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

public class QuestStage implements DataObject {
    private String id = "";
    private String description = "";
    private final List<QuestObjective> objectives = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<QuestObjective> getObjectives() { return objectives; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("description", description);
        JsonArray arr = new JsonArray();
        for (QuestObjective o : objectives) arr.add(o.toJson());
        json.add("objectives", arr);
        return json;
    }

    public void fromJson(JsonObject json) {
        id = json.has("id") ? json.get("id").getAsString() : "";
        description = json.has("description") ? json.get("description").getAsString() : "";
        objectives.clear();
        if (json.has("objectives")) {
            for (JsonElement e : json.getAsJsonArray("objectives")) {
                QuestObjective o = new QuestObjective();
                o.fromJson(e.getAsJsonObject());
                objectives.add(o);
            }
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Description", description);
        ListTag list = new ListTag();
        for (QuestObjective o : objectives) list.add(o.save());
        tag.put("Objectives", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        id = tag.getString("Id");
        description = tag.getString("Description");
        objectives.clear();
        ListTag list = tag.getList("Objectives", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            QuestObjective o = new QuestObjective();
            o.load(list.getCompound(i));
            objectives.add(o);
        }
    }

    public QuestStage copy() {
        QuestStage s = new QuestStage();
        s.fromJson(this.toJson());
        return s;
    }

    public QuestStage copyTemplate() {
        QuestStage copy = new QuestStage();
        copy.id = this.id;
        copy.description = this.description;
        for (QuestObjective obj : this.objectives) copy.objectives.add(obj.copyTemplate());
        return copy;
    }
}