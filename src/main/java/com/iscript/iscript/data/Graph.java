package com.iscript.iscript.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;

public class Graph implements DataObject {
    private String id = "";
    private String name = "";
    private String startNodeId = "";
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Class<? extends Enum<?>> nodeTypeEnum;

    public Graph() { this(null); }
    public Graph(Class<? extends Enum<?>> nodeTypeEnum) { this.nodeTypeEnum = nodeTypeEnum; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id != null ? id : ""; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name : ""; }
    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String id) { this.startNodeId = id != null ? id : ""; }
    public Map<String, Node> getNodes() { return Collections.unmodifiableMap(nodes); }
    public Node getNode(String id) { return nodes.get(id); }

    public void addNode(Node node) {
        if (node == null || node.getId() == null || node.getId().isEmpty()) return;
        nodes.put(node.getId(), node);
        if (startNodeId.isEmpty() && node.getType() != null && node.getType().equals("START")) startNodeId = node.getId();
    }

    public void removeNode(String id) {
        nodes.remove(id);
        if (startNodeId.equals(id)) {
            startNodeId = "";
            for (Node n : nodes.values()) if ("START".equals(n.getType())) { startNodeId = n.getId(); break; }
        }
        for (Node n : nodes.values()) n.getConnections().removeIf(c -> c.getTarget().equals(id));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("startNodeId", startNodeId);
        JsonArray arr = new JsonArray();
        for (Node n : nodes.values()) arr.add(n.toJson());
        json.add("nodes", arr);
        return json;
    }

    public void fromJson(JsonObject json) {
        id = json.has("id") ? json.get("id").getAsString() : "";
        name = json.has("name") ? json.get("name").getAsString() : "";
        startNodeId = json.has("startNodeId") ? json.get("startNodeId").getAsString() : "";
        nodes.clear();
        if (json.has("nodes")) {
            JsonArray arr = json.getAsJsonArray("nodes");
            for (JsonElement e : arr) {
                Node n = new Node(nodeTypeEnum);
                n.fromJson(e.getAsJsonObject());
                if (n.getId() != null && !n.getId().isEmpty()) nodes.put(n.getId(), n);
            }
        }
    }

    public Graph copy() {
        Graph g = new Graph(nodeTypeEnum);
        g.id = this.id + "_copy";
        g.name = this.name + " (Copy)";
        g.startNodeId = this.startNodeId;
        for (Node n : this.nodes.values()) {
            Node c = new Node(nodeTypeEnum);
            c.fromJson(n.toJson());
            g.nodes.put(c.getId(), c);
        }
        return g;
    }
}