package com.iscript.imson.script.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.graalvm.polyglot.HostAccess;

public class ItemAPI {
    private final ItemStack stack;

    public ItemAPI(ItemStack stack) {
        this.stack = stack;
    }

    @HostAccess.Export
    public ItemStack getItem() {
        return stack;
    }

    @HostAccess.Export
    public int getCount() {
        return stack.getCount();
    }

    @HostAccess.Export
    public String getId() {
        return ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
    }

    @HostAccess.Export
    public net.minecraft.nbt.CompoundTag getNbt() {
        return stack.getTag();
    }

    @HostAccess.Export
    public String getName() {
        return stack.getDisplayName().getString();
    }

    @HostAccess.Export
    public int getMaxCount() {
        return stack.getMaxStackSize();
    }

    @HostAccess.Export
    public void setCount(int count) {
        stack.setCount(count);
    }

    @HostAccess.Export
    public void setName(String name) {
        stack.setHoverName(Component.literal(name));
    }

    @HostAccess.Export
    public void setCooldown(int ticks) {
    }

    @HostAccess.Export
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @HostAccess.Export
    public void remove() {
        stack.setCount(0);
    }

    @HostAccess.Export
    public void add(int amount) {
        stack.grow(amount);
    }
}