package com.iscript.iscript.data.dialog;

import com.iscript.iscript.capability.ModCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DialogCondition {
    private ConditionType type = ConditionType.NONE;
    private String value = "";
    private int amount = 1;

    public enum ConditionType {
        NONE, QUEST_COMPLETED, QUEST_ACTIVE, HAS_ITEM, FACTION, LEVEL_ABOVE, REPUTATION_ABOVE
    }

    public ConditionType getType() { return type; }
    public void setType(ConditionType type) { this.type = type; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public boolean check(Player player) {
        return switch (type) {
            case NONE -> true;
            case QUEST_COMPLETED -> player.getCapability(ModCapabilities.PLAYER_QUESTS)
                    .map(data -> data.isCompleted(value)).orElse(false);
            case QUEST_ACTIVE -> player.getCapability(ModCapabilities.PLAYER_QUESTS)
                    .map(data -> data.getProgress(value) > 0 && !data.isCompleted(value)).orElse(false);
            case HAS_ITEM -> {
                int count = 0;
                for (ItemStack stack : player.getInventory().items) {
                    if (!stack.isEmpty() && stack.getItem().getDescriptionId().equals(value)) {
                        count += stack.getCount();
                    }
                }
                yield count >= amount;
            }
            case FACTION -> player.getCapability(ModCapabilities.PLAYER_DATA)
                    .map(data -> data.getFaction().equalsIgnoreCase(value)).orElse(false);
            case LEVEL_ABOVE -> player.experienceLevel >= amount;
            case REPUTATION_ABOVE -> player.getCapability(ModCapabilities.PLAYER_DATA)
                    .map(data -> data.getReputation() >= amount).orElse(false);
        };
    }

    public void save(CompoundTag tag) {
        tag.putString("Type", type.name());
        tag.putString("Value", value);
        tag.putInt("Amount", amount);
    }

    public void load(CompoundTag tag) {
        try {
            type = ConditionType.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException e) {
            type = ConditionType.NONE;
        }
        value = tag.getString("Value");
        amount = tag.getInt("Amount");
    }
}