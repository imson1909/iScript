package com.iscript.iscript.data.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class NPCData {
    private String name = "New NPC";
    private String dialogId = "";
    private String faction = "neutral";
    private String skin = "";
    private float maxHealth = 20.0f;
    private float health = 20.0f;
    private float attackDamage = 2.0f;
    private float movementSpeed = 0.4f;
    private NPCState state = NPCState.IDLE;
    private boolean aggressive = false;
    private String hostileFactions = "";
    private String animation = "";
    private NPCTradeData tradeData = new NPCTradeData();
    private boolean enableTrade = false;
    private float scale = 1.0f;
    private boolean nameVisible = true;
    private int glowColor = 0xFFFFFFFF;
    private boolean glowEnabled = false;
    private boolean noAI = false;
    private boolean invulnerable = false;
    private boolean silent = false;
    private boolean hasGravity = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDialogId() { return dialogId; }
    public void setDialogId(String dialogId) { this.dialogId = dialogId; }
    public String getFaction() { return faction; }
    public void setFaction(String faction) { this.faction = faction; }
    public String getSkin() { return skin; }
    public void setSkin(String skin) { this.skin = skin; }
    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }
    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = Math.min(health, maxHealth); }
    public float getAttackDamage() { return attackDamage; }
    public void setAttackDamage(float attackDamage) { this.attackDamage = attackDamage; }
    public float getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(float movementSpeed) { this.movementSpeed = movementSpeed; }
    public NPCState getState() { return state; }
    public void setState(NPCState state) { this.state = state; }
    public boolean isAggressive() { return aggressive; }
    public void setAggressive(boolean aggressive) { this.aggressive = aggressive; }
    public String getHostileFactions() { return hostileFactions; }
    public void setHostileFactions(String hostileFactions) { this.hostileFactions = hostileFactions; }
    public String getAnimation() { return animation; }
    public void setAnimation(String animation) { this.animation = animation; }
    public NPCTradeData getTradeData() { return tradeData; }
    public void setTradeData(NPCTradeData tradeData) { this.tradeData = tradeData; }
    public boolean isEnableTrade() { return enableTrade; }
    public void setEnableTrade(boolean enableTrade) { this.enableTrade = enableTrade; }
    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }
    public boolean isNameVisible() { return nameVisible; }
    public void setNameVisible(boolean nameVisible) { this.nameVisible = nameVisible; }
    public int getGlowColor() { return glowColor; }
    public void setGlowColor(int glowColor) { this.glowColor = glowColor; }
    public boolean isGlowEnabled() { return glowEnabled; }
    public void setGlowEnabled(boolean glowEnabled) { this.glowEnabled = glowEnabled; }
    public boolean isNoAI() { return noAI; }
    public void setNoAI(boolean noAI) { this.noAI = noAI; }
    public boolean isInvulnerable() { return invulnerable; }
    public void setInvulnerable(boolean invulnerable) { this.invulnerable = invulnerable; }
    public boolean isSilent() { return silent; }
    public void setSilent(boolean silent) { this.silent = silent; }
    public boolean isHasGravity() { return hasGravity; }
    public void setHasGravity(boolean hasGravity) { this.hasGravity = hasGravity; }

    public boolean isHostileTo(String otherFaction) {
        if (hostileFactions.isEmpty()) return false;
        for (String f : hostileFactions.split(",")) {
            if (f.trim().equalsIgnoreCase(otherFaction)) return true;
        }
        return false;
    }

    public void save(CompoundTag tag) {
        tag.putString("Name", this.name);
        tag.putString("DialogId", this.dialogId);
        tag.putString("Faction", this.faction);
        tag.putString("Skin", this.skin);
        tag.putFloat("MaxHealth", this.maxHealth);
        tag.putFloat("Health", this.health);
        tag.putFloat("AttackDamage", this.attackDamage);
        tag.putFloat("MovementSpeed", this.movementSpeed);
        tag.putString("State", this.state.name());
        tag.putBoolean("Aggressive", this.aggressive);
        tag.putString("HostileFactions", this.hostileFactions);
        tag.putString("Animation", this.animation);
        tag.putBoolean("EnableTrade", this.enableTrade);
        tag.putFloat("Scale", this.scale);
        tag.putBoolean("NameVisible", this.nameVisible);
        tag.putInt("GlowColor", this.glowColor);
        tag.putBoolean("GlowEnabled", this.glowEnabled);
        tag.putBoolean("NoAI", this.noAI);
        tag.putBoolean("Invulnerable", this.invulnerable);
        tag.putBoolean("Silent", this.silent);
        tag.putBoolean("HasGravity", this.hasGravity);
        CompoundTag tradeTag = new CompoundTag();
        tradeData.save(tradeTag);
        tag.put("TradeData", tradeTag);
    }

    public void load(CompoundTag tag) {
        this.name = tag.contains("Name") ? tag.getString("Name") : "New NPC";
        this.dialogId = tag.contains("DialogId") ? tag.getString("DialogId") : "";
        this.faction = tag.contains("Faction") ? tag.getString("Faction") : "neutral";
        this.skin = tag.contains("Skin") ? tag.getString("Skin") : "";
        this.maxHealth = tag.contains("MaxHealth") ? tag.getFloat("MaxHealth") : 20.0f;
        this.health = tag.contains("Health") ? tag.getFloat("Health") : this.maxHealth;
        this.attackDamage = tag.contains("AttackDamage") ? tag.getFloat("AttackDamage") : 2.0f;
        this.movementSpeed = tag.contains("MovementSpeed") ? tag.getFloat("MovementSpeed") : 0.4f;
        try {
            this.state = NPCState.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException e) {
            this.state = NPCState.IDLE;
        }
        this.aggressive = tag.contains("Aggressive") ? tag.getBoolean("Aggressive") : false;
        this.hostileFactions = tag.contains("HostileFactions") ? tag.getString("HostileFactions") : "";
        this.animation = tag.contains("Animation") ? tag.getString("Animation") : "";
        this.enableTrade = tag.contains("EnableTrade") ? tag.getBoolean("EnableTrade") : false;
        this.scale = tag.contains("Scale") ? tag.getFloat("Scale") : 1.0f;
        this.nameVisible = tag.contains("NameVisible") ? tag.getBoolean("NameVisible") : true;
        this.glowColor = tag.contains("GlowColor") ? tag.getInt("GlowColor") : 0xFFFFFFFF;
        this.glowEnabled = tag.contains("GlowEnabled") ? tag.getBoolean("GlowEnabled") : false;
        this.noAI = tag.contains("NoAI") ? tag.getBoolean("NoAI") : false;
        this.invulnerable = tag.contains("Invulnerable") ? tag.getBoolean("Invulnerable") : false;
        this.silent = tag.contains("Silent") ? tag.getBoolean("Silent") : false;
        this.hasGravity = tag.contains("HasGravity") ? tag.getBoolean("HasGravity") : true;
        if (tag.contains("TradeData")) {
            tradeData.load(tag.getCompound("TradeData"));
        }
    }
}
