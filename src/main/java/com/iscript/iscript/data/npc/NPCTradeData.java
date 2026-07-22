package com.iscript.iscript.data.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.List;

public class NPCTradeData {
    private final List<TradeOffer> offers = new ArrayList<>();

    public List<TradeOffer> getOffers() { return offers; }

    public void addOffer(ItemStack input, ItemStack output, int maxUses) {
        offers.add(new TradeOffer(input, output, maxUses));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        JsonArray arr = new JsonArray();
        for (TradeOffer o : offers) arr.add(o.toJson());
        json.add("offers", arr);
        return json;
    }

    public void load(JsonObject json) {
        offers.clear();
        if (json.has("offers")) {
            for (JsonElement e : json.getAsJsonArray("offers")) {
                TradeOffer o = new TradeOffer();
                o.fromJson(e.getAsJsonObject());
                offers.add(o);
            }
        }
    }

    public static class TradeOffer implements com.iscript.iscript.data.DataObject {
        private ItemStack input = ItemStack.EMPTY;
        private ItemStack output = ItemStack.EMPTY;
        private int maxUses = 64;
        private int uses;

        public TradeOffer() {}
        public TradeOffer(ItemStack input, ItemStack output, int maxUses) {
            this.input = input.copy();
            this.output = output.copy();
            this.maxUses = maxUses;
        }

        public ItemStack getInput() { return input; }
        public ItemStack getOutput() { return output; }
        public int getMaxUses() { return maxUses; }
        public int getUses() { return uses; }
        public boolean isAvailable() { return uses < maxUses; }
        public void use() { uses++; }
        public void restock() { uses = 0; }

        public String getId() { return input.getItem().toString(); }
        public void setId(String id) {}

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.add("input", itemToJson(input));
            json.add("output", itemToJson(output));
            json.addProperty("maxUses", maxUses);
            json.addProperty("uses", uses);
            return json;
        }

        public void fromJson(JsonObject json) {
            input = json.has("input") ? itemFromJson(json.getAsJsonObject("input")) : ItemStack.EMPTY;
            output = json.has("output") ? itemFromJson(json.getAsJsonObject("output")) : ItemStack.EMPTY;
            maxUses = json.has("maxUses") ? json.get("maxUses").getAsInt() : 64;
            uses = json.has("uses") ? json.get("uses").getAsInt() : 0;
        }

        private JsonObject itemToJson(ItemStack stack) {
            JsonObject o = new JsonObject();
            o.addProperty("id", stack.getItem().toString());
            o.addProperty("count", stack.getCount());
            if (stack.hasTag()) {
                o.addProperty("nbt", stack.getTag().toString());
            }
            return o;
        }

        private ItemStack itemFromJson(JsonObject json) {
            String id = json.has("id") ? json.get("id").getAsString() : "minecraft:air";
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)), count);
            if (json.has("nbt")) {
                try {
                    net.minecraft.nbt.Tag tag = TagParser.parseTag(json.get("nbt").getAsString());
                    stack.setTag((CompoundTag) tag);
                } catch (Exception ignored) {}
            }
            return stack;
        }
    }


    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (TradeOffer o : offers) {
            CompoundTag t = new CompoundTag();
            t.put("input", o.getInput().save(new CompoundTag()));
            t.put("output", o.getOutput().save(new CompoundTag()));
            t.putInt("maxUses", o.getMaxUses());
            t.putInt("uses", o.getUses());
            list.add(t);
        }
        tag.put("Offers", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        offers.clear();
        ListTag list = tag.getList("Offers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            TradeOffer o = new TradeOffer();
            o.input = ItemStack.of(t.getCompound("input"));
            o.output = ItemStack.of(t.getCompound("output"));
            o.maxUses = t.getInt("maxUses");
            o.uses = t.getInt("uses");
            offers.add(o);
        }
    }
}