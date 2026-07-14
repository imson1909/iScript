package com.iscript.iscript.data.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class QuestReward {
    private final List<ItemReward> items = new ArrayList<>();
    private int exp = 0;
    private String title = "";
    private String command = "";

    public List<ItemReward> getItems() { return items; }
    public int getExp() { return exp; }
    public void setExp(int exp) { this.exp = exp; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public void load(CompoundTag tag) {
        this.items.clear();
        ListTag itemList = tag.getList("Items", 10);
        for (int i = 0; i < itemList.size(); i++) {
            ItemReward item = new ItemReward();
            item.load(itemList.getCompound(i));
            this.items.add(item);
        }
        this.exp = tag.getInt("Exp");
        this.title = tag.getString("Title");
        this.command = tag.getString("Command");
    }

    public void save(CompoundTag tag) {
        ListTag itemList = new ListTag();
        for (ItemReward item : this.items) {
            CompoundTag t = new CompoundTag();
            item.save(t);
            itemList.add(t);
        }
        tag.put("Items", itemList);
        tag.putInt("Exp", this.exp);
        tag.putString("Title", this.title);
        tag.putString("Command", this.command);
    }

    public QuestReward copy() {
        QuestReward copy = new QuestReward();
        for (ItemReward item : this.items) {
            copy.items.add(item.copy());
        }
        copy.exp = this.exp;
        copy.title = this.title;
        copy.command = this.command;
        return copy;
    }

    public static class ItemReward {
        private String itemId = "";
        private int count = 1;

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public void load(CompoundTag tag) {
            this.itemId = tag.getString("ItemId");
            this.count = tag.getInt("Count");
        }

        public void save(CompoundTag tag) {
            tag.putString("ItemId", this.itemId);
            tag.putInt("Count", this.count);
        }

        public ItemReward copy() {
            ItemReward copy = new ItemReward();
            copy.itemId = this.itemId;
            copy.count = this.count;
            return copy;
        }
    }
}