package com.iscript.iscript.data.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iscript.iscript.data.DataObject;
import com.iscript.iscript.data.Graph;
import java.util.ArrayList;
import java.util.List;

public class QuestData implements DataObject {
    private String id = "";
    private String title = "";
    private String description = "";
    private final List<QuestStage> stages = new ArrayList<>();
    private QuestReward reward = new QuestReward();
    private final List<String> prerequisites = new ArrayList<>();
    private String giverNpcId = "";
    private String turnInNpcId = "";
    private Graph graph;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<QuestStage> getStages() { return stages; }
    public QuestReward getReward() { return reward; }
    public void setReward(QuestReward reward) { this.reward = reward; }
    public List<String> getPrerequisites() { return prerequisites; }
    public String getGiverNpcId() { return giverNpcId; }
    public void setGiverNpcId(String id) { this.giverNpcId = id; }
    public String getTurnInNpcId() { return turnInNpcId; }
    public void setTurnInNpcId(String id) { this.turnInNpcId = id; }
    public Graph getGraph() { return graph; }
    public void setGraph(Graph graph) { this.graph = graph; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("title", title);
        json.addProperty("description", description);
        JsonArray arr = new JsonArray();
        for (QuestStage s : stages) arr.add(s.toJson());
        json.add("stages", arr);
        json.add("reward", reward.toJson());
        JsonArray pre = new JsonArray();
        for (String p : prerequisites) pre.add(p);
        json.add("prerequisites", pre);
        json.addProperty("giverNpcId", giverNpcId);
        json.addProperty("turnInNpcId", turnInNpcId);
        if (graph != null) json.add("graph", graph.toJson());
        return json;
    }

    public void fromJson(JsonObject json) {
        id = json.has("id") ? json.get("id").getAsString() : "";
        title = json.has("title") ? json.get("title").getAsString() : "";
        description = json.has("description") ? json.get("description").getAsString() : "";
        stages.clear();
        if (json.has("stages")) {
            for (JsonElement e : json.getAsJsonArray("stages")) {
                QuestStage s = new QuestStage();
                s.fromJson(e.getAsJsonObject());
                stages.add(s);
            }
        }
        reward = new QuestReward();
        if (json.has("reward")) reward.fromJson(json.getAsJsonObject("reward"));
        prerequisites.clear();
        if (json.has("prerequisites")) {
            for (JsonElement e : json.getAsJsonArray("prerequisites")) prerequisites.add(e.getAsString());
        }
        giverNpcId = json.has("giverNpcId") ? json.get("giverNpcId").getAsString() : "";
        turnInNpcId = json.has("turnInNpcId") ? json.get("turnInNpcId").getAsString() : "";
        if (json.has("graph")) {
            graph = new Graph(null);
            graph.fromJson(json.getAsJsonObject("graph"));
        } else {
            graph = null;
        }
    }

    public QuestData copy() {
        QuestData q = new QuestData();
        q.fromJson(this.toJson());
        q.id = this.id + "_copy";
        return q;
    }
}