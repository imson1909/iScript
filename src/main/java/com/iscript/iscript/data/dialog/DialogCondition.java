package com.iscript.iscript.data.dialog;

import com.google.gson.JsonObject;
import com.iscript.iscript.capability.ModCapabilities;
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
                    if (!stack.isEmpty() && stack.getItem().getDescriptionId().equals(value)) count += stack.getCount();
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

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.addProperty("value", value);
        json.addProperty("amount", amount);
        return json;
    }

    public void fromJson(JsonObject json) {
        try { type = ConditionType.valueOf(json.get("type").getAsString()); }
        catch (Exception e) { type = ConditionType.NONE; }
        value = json.has("value") ? json.get("value").getAsString() : "";
        amount = json.has("amount") ? json.get("amount").getAsInt() : 1;
    }
}