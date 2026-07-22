package com.iscript.iscript.data.dialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iscript.iscript.data.DataObject;
import com.iscript.iscript.data.Graph;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;

public class DialogData implements DataObject {
    private String id = "";
    private String title = "Dialog";
    private String text = "Hello!";
    private String sound = "";
    private String portrait = "";
    private final List<DialogOption> options = new ArrayList<>();
    private Graph graph;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }
    public String getPortrait() { return portrait; }
    public void setPortrait(String portrait) { this.portrait = portrait; }
    public List<DialogOption> getOptions() { return options; }
    public Graph getGraph() { return graph; }
    public void setGraph(Graph graph) { this.graph = graph; }

    public List<DialogOption> getAvailableOptions(Player player) {
        List<DialogOption> result = new ArrayList<>();
        for (DialogOption opt : options) if (opt.getCondition().check(player)) result.add(opt);
        return result;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("title", title);
        json.addProperty("text", text);
        json.addProperty("sound", sound);
        json.addProperty("portrait", portrait);
        JsonArray arr = new JsonArray();
        for (DialogOption o : options) arr.add(o.toJson());
        json.add("options", arr);
        if (graph != null) json.add("graph", graph.toJson());
        return json;
    }

    public void fromJson(JsonObject json) {
        id = json.has("id") ? json.get("id").getAsString() : "";
        title = json.has("title") ? json.get("title").getAsString() : "Dialog";
        text = json.has("text") ? json.get("text").getAsString() : "Hello!";
        sound = json.has("sound") ? json.get("sound").getAsString() : "";
        portrait = json.has("portrait") ? json.get("portrait").getAsString() : "";
        options.clear();
        if (json.has("options")) {
            for (JsonElement e : json.getAsJsonArray("options")) {
                DialogOption o = new DialogOption();
                o.fromJson(e.getAsJsonObject());
                options.add(o);
            }
        }
        if (json.has("graph")) {
            graph = new Graph(null);
            graph.fromJson(json.getAsJsonObject("graph"));
        } else {
            graph = null;
        }
    }

    public static class DialogOption implements DataObject {
        private String text = "Continue...";
        private String targetDialogId = "";
        private String command = "";
        private DialogCondition condition = new DialogCondition();
        private String tooltip = "";

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getTargetDialogId() { return targetDialogId; }
        public void setTargetDialogId(String id) { this.targetDialogId = id; }
        public String getCommand() { return command; }
        public void setCommand(String cmd) { this.command = cmd; }
        public DialogCondition getCondition() { return condition; }
        public void setCondition(DialogCondition c) { this.condition = c; }
        public String getTooltip() { return tooltip; }
        public void setTooltip(String t) { this.tooltip = t; }

        public String getId() { return targetDialogId; }
        public void setId(String id) { this.targetDialogId = id; }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("text", text);
            json.addProperty("target", targetDialogId);
            json.addProperty("command", command);
            json.addProperty("tooltip", tooltip);
            json.add("condition", condition.toJson());
            return json;
        }

        public void fromJson(JsonObject json) {
            text = json.has("text") ? json.get("text").getAsString() : "Continue...";
            targetDialogId = json.has("target") ? json.get("target").getAsString() : "";
            command = json.has("command") ? json.get("command").getAsString() : "";
            tooltip = json.has("tooltip") ? json.get("tooltip").getAsString() : "";
            if (json.has("condition")) condition.fromJson(json.getAsJsonObject("condition"));
        }
    }
}