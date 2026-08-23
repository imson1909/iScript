package com.iscript.imson.script.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.graalvm.polyglot.HostAccess;

public class InventoryAPI {
    private final Player player;

    public InventoryAPI(Player player) {
        this.player = player;
    }

    @HostAccess.Export
    public void clear() {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            player.getInventory().items.set(i, ItemStack.EMPTY);
        }
    }

    @HostAccess.Export
    public ItemStack getStack(int slot) {
        if (slot >= 0 && slot < player.getInventory().items.size()) {
            return player.getInventory().items.get(slot);
        }
        return ItemStack.EMPTY;
    }

    @HostAccess.Export
    public void setStack(String itemId, int slot) {
        try {
            net.minecraft.resources.ResourceLocation id = new net.minecraft.resources.ResourceLocation(itemId);
            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
            if (item != null && slot >= 0 && slot < player.getInventory().items.size()) {
                player.getInventory().items.set(slot, new ItemStack(item));
            }
        } catch (Exception e) {
        }
    }
}