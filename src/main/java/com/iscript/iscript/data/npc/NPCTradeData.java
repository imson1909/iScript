package com.iscript.iscript.data.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NPCTradeData {
    private final List<TradeOffer> offers = new ArrayList<>();

    public List<TradeOffer> getOffers() { return offers; }

    public void addOffer(ItemStack input, ItemStack output, int maxUses) {
        offers.add(new TradeOffer(input, output, maxUses));
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (TradeOffer offer : offers) {
            CompoundTag t = new CompoundTag();
            offer.save(t);
            list.add(t);
        }
        tag.put("Offers", list);
    }

    public void load(CompoundTag tag) {
        offers.clear();
        ListTag list = tag.getList("Offers", 10);
        for (int i = 0; i < list.size(); i++) {
            TradeOffer o = new TradeOffer();
            o.load(list.getCompound(i));
            offers.add(o);
        }
    }

    public static class TradeOffer {
        private ItemStack input = ItemStack.EMPTY;
        private ItemStack output = ItemStack.EMPTY;
        private int maxUses = 64;
        private int uses = 0;

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

        public void save(CompoundTag tag) {
            tag.put("Input", input.save(new CompoundTag()));
            tag.put("Output", output.save(new CompoundTag()));
            tag.putInt("MaxUses", maxUses);
            tag.putInt("Uses", uses);
        }

        public void load(CompoundTag tag) {
            input = ItemStack.of(tag.getCompound("Input"));
            output = ItemStack.of(tag.getCompound("Output"));
            maxUses = tag.getInt("MaxUses");
            uses = tag.getInt("Uses");
        }
    }
}
