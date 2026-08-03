package com.iscript.iscript.data.dialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.DataObject;
import com.iscript.iscript.data.Graph;
import com.iscript.iscript.data.Node;
import com.iscript.iscript.script.ScriptEngine;
import net.minecraft.server.level.ServerLevel;
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

    public DialogData resolve(Player player, String targetNodeId) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) return this;

        String nodeId = targetNodeId;
        if (nodeId == null || nodeId.isEmpty()) {
            nodeId = graph.getStartNodeId();
        }

        Node node = graph.getNode(nodeId);
        while (node != null) {
            String type = node.getParam("type");
            if (type == null) type = "";

            switch (type) {
                case "start" -> {
                    Node next = getNextNode(node, 0);
                    if (next != null) {
                        node = next;
                    } else {
                        return buildFromNode(player, node);
                    }
                }
                case "npc", "player", "choice" -> {
                    return buildFromNode(player, node);
                }
                case "action" -> {
                    executeAction(node, player);
                    node = getNextNode(node, 0);
                }
                case "condition" -> {
                    boolean result = evaluateCondition(node, player);
                    node = getNextNode(node, result ? 0 : 1);
                }
                case "jump" -> {
                    String targetDialog = node.getParam("target_dialog");
                    if (targetDialog != null && !targetDialog.isEmpty()) {
                        DialogData target = com.iscript.iscript.data.DataAccess.dialog(targetDialog);
                        if (target != null) {
                            DialogManager.setActiveDialog(player, targetDialog);
                            return target.resolve(player, null);
                        }
                    }
                    node = getNextNode(node, 0);
                }
                default -> {
                    node = getNextNode(node, 0);
                }
            }
        }

        return null;
    }

    private DialogData buildFromNode(Player player, Node node) {
        DialogData result = new DialogData();
        result.setId(this.id);
        result.setTitle(node.getParam("title"));
        result.setText(node.getParam("text"));
        result.setPortrait(node.getParam("portrait"));
        result.setSound(node.getParam("sound"));

        for (Node.Connection conn : node.getConnections()) {
            Node target = graph.getNode(conn.getTarget());
            DialogOption opt = new DialogOption();
            if (target != null) {
                opt.setText(target.getParam("title"));
            } else {
                opt.setText("...");
            }
            opt.setTargetDialogId(this.id + "/" + conn.getTarget());
            result.getOptions().add(opt);
        }

        if (result.getOptions().isEmpty()) {
            DialogOption end = new DialogOption();
            end.setText("End");
            end.setTargetDialogId("");
            result.getOptions().add(end);
        }

        DialogManager.setCurrentNode(player, node.getId());
        return result;
    }

    private Node getNextNode(Node node, int slot) {
        if (node == null || graph == null) return null;
        for (Node.Connection conn : node.getConnections()) {
            if (conn.getSourceSlot() == slot) {
                return graph.getNode(conn.getTarget());
            }
        }
        return null;
    }

    private void executeAction(Node node, Player player) {
        String script = node.getParam("text");
        if (script == null || script.isEmpty()) return;
        ScriptEngine engine = ScriptEngine.getInstance();
        if (!engine.isAvailable()) return;
        try {
            engine.execute(this.id + "_action", script, player, (ServerLevel) player.level());
        } catch (Exception e) {
            IScriptMod.LOGGER.error("Dialog action failed: {}", e.getMessage());
        }
    }

    private boolean evaluateCondition(Node node, Player player) {
        String condition = node.getParam("text");
        if (condition == null || condition.isEmpty()) return false;
        String lower = condition.trim().toLowerCase();
        if (lower.equals("true")) return true;
        if (lower.equals("false")) return false;
        ScriptEngine engine = ScriptEngine.getInstance();
        if (!engine.isAvailable()) return false;
        try {
            Object result = engine.execute(this.id + "_condition", condition, player, (ServerLevel) player.level());
            if (result instanceof Boolean) return (Boolean) result;
            if (result instanceof Number) return ((Number) result).doubleValue() != 0;
            if (result instanceof String) return Boolean.parseBoolean((String) result);
            return result != null;
        } catch (Exception e) {
            IScriptMod.LOGGER.error("Dialog condition failed: {}", e.getMessage());
            return false;
        }
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