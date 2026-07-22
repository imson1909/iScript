package com.iscript.iscript.data.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iscript.iscript.data.DataObject;
import java.util.ArrayList;
import java.util.List;

public class QuestReward implements DataObject {
    private final List<ItemReward> items = new ArrayList<>();
    private int exp;
    private String title = "";
    private String command = "";

    public List<ItemReward> getItems() { return items; }
    public int getExp() { return exp; }
    public void setExp(int exp) { this.exp = exp; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getId() { return title; }
    public void setId(String id) { this.title = id; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        JsonArray arr = new JsonArray();
        for (ItemReward i : items) arr.add(i.toJson());
        json.add("items", arr);
        json.addProperty("exp", exp);
        json.addProperty("title", title);
        json.addProperty("command", command);
        return json;
    }

    public void fromJson(JsonObject json) {
        items.clear();
        if (json.has("items")) {
            for (JsonElement e : json.getAsJsonArray("items")) {
                ItemReward r = new ItemReward();
                r.fromJson(e.getAsJsonObject());
                items.add(r);
            }
        }
        exp = json.has("exp") ? json.get("exp").getAsInt() : 0;
        title = json.has("title") ? json.get("title").getAsString() : "";
        command = json.has("command") ? json.get("command").getAsString() : "";
    }

    public QuestReward copy() {
        QuestReward r = new QuestReward();
        r.fromJson(this.toJson());
        return r;
    }

    public static class ItemReward implements DataObject {
        private String itemId = "";
        private int count = 1;

        public String getItemId() { return itemId; }
        public void setItemId(String id) { this.itemId = id; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public String getId() { return itemId; }
        public void setId(String id) { this.itemId = id; }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("itemId", itemId);
            json.addProperty("count", count);
            return json;
        }

        public void fromJson(JsonObject json) {
            itemId = json.has("itemId") ? json.get("itemId").getAsString() : "";
            count = json.has("count") ? json.get("count").getAsInt() : 1;
        }

        public ItemReward copy() {
            ItemReward r = new ItemReward();
            r.fromJson(this.toJson());
            return r;
        }
    }
}