package com.iscript.iscript.data.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NPCData {
    private String id = "";
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
    private String behaviorMode = "peaceful";
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

    private boolean canSwim = false;
    private boolean canFly = false;
    private boolean immovable = false;
    private boolean hasPost = false;
    private BlockPos postPosition = null;
    private float postRadius = 1.0f;
    private float fallback = 15.0f;
    private boolean patrolLoop = false;
    private List<BlockPos> patrolPoints = new ArrayList<>();
    private boolean lookAtPlayer = false;
    private boolean lookAround = false;
    private boolean wander = false;
    private boolean alwaysWander = false;
    private int regenDelay = 0;
    private int regenFrequency = 20;
    private int damageDelay = 20;
    private float pathDistance = 32.0f;
    private boolean canFallDamage = true;
    private boolean canGetBurned = true;
    private boolean killable = true;
    private String followTarget = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
    public boolean isAggressive() { return "aggressive".equals(behaviorMode); }
    public void setAggressive(boolean aggressive) { this.aggressive = aggressive; }
    public String getBehaviorMode() { return behaviorMode; }
    public void setBehaviorMode(String behaviorMode) { this.behaviorMode = behaviorMode; }
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

    public boolean isCanSwim() { return canSwim; }
    public void setCanSwim(boolean canSwim) { this.canSwim = canSwim; }
    public boolean isCanFly() { return canFly; }
    public void setCanFly(boolean canFly) { this.canFly = canFly; }
    public boolean isImmovable() { return immovable; }
    public void setImmovable(boolean immovable) { this.immovable = immovable; }
    public boolean isHasPost() { return hasPost; }
    public void setHasPost(boolean hasPost) { this.hasPost = hasPost; }
    public BlockPos getPostPosition() { return postPosition; }
    public void setPostPosition(BlockPos postPosition) { this.postPosition = postPosition; }
    public float getPostRadius() { return postRadius; }
    public void setPostRadius(float postRadius) { this.postRadius = postRadius; }
    public float getFallback() { return fallback; }
    public void setFallback(float fallback) { this.fallback = fallback; }
    public boolean isPatrolLoop() { return patrolLoop; }
    public void setPatrolLoop(boolean patrolLoop) { this.patrolLoop = patrolLoop; }
    public List<BlockPos> getPatrolPoints() { return patrolPoints; }
    public void setPatrolPoints(List<BlockPos> patrolPoints) { this.patrolPoints = patrolPoints; }
    public boolean isLookAtPlayer() { return lookAtPlayer; }
    public void setLookAtPlayer(boolean lookAtPlayer) { this.lookAtPlayer = lookAtPlayer; }
    public boolean isLookAround() { return lookAround; }
    public void setLookAround(boolean lookAround) { this.lookAround = lookAround; }
    public boolean isWander() { return wander; }
    public void setWander(boolean wander) { this.wander = wander; }
    public boolean isAlwaysWander() { return alwaysWander; }
    public void setAlwaysWander(boolean alwaysWander) { this.alwaysWander = alwaysWander; }
    public int getRegenDelay() { return regenDelay; }
    public void setRegenDelay(int regenDelay) { this.regenDelay = regenDelay; }
    public int getRegenFrequency() { return regenFrequency; }
    public void setRegenFrequency(int regenFrequency) { this.regenFrequency = regenFrequency; }
    public int getDamageDelay() { return damageDelay; }
    public void setDamageDelay(int damageDelay) { this.damageDelay = damageDelay; }
    public float getPathDistance() { return pathDistance; }
    public void setPathDistance(float pathDistance) { this.pathDistance = pathDistance; }
    public boolean isCanFallDamage() { return canFallDamage; }
    public void setCanFallDamage(boolean canFallDamage) { this.canFallDamage = canFallDamage; }
    public boolean isCanGetBurned() { return canGetBurned; }
    public void setCanGetBurned(boolean canGetBurned) { this.canGetBurned = canGetBurned; }
    public boolean isKillable() { return killable; }
    public void setKillable(boolean killable) { this.killable = killable; }
    public String getFollowTarget() { return followTarget; }
    public void setFollowTarget(String followTarget) { this.followTarget = followTarget; }

    public boolean isHostileTo(String otherFaction) {
        if (hostileFactions.isEmpty()) return false;
        for (String f : hostileFactions.split(",")) {
            if (f.trim().equalsIgnoreCase(otherFaction)) return true;
        }
        return false;
    }

    public void save(CompoundTag tag) {
        tag.putString("Id", this.id);
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
        tag.putString("BehaviorMode", this.behaviorMode);
        tag.putString("HostileFactions", this.hostileFactions);
        tag.putString("Animation", this.animation);
        tag.putBoolean("EnableTrade", this.enableTrade);
        tag.putFloat("Scale", this.scale);
        tag.putBoolean("NameVisible", this.nameVisible);
        tag.putBoolean("GlowEnabled", this.glowEnabled);
        tag.putBoolean("NoAI", this.noAI);
        tag.putBoolean("Invulnerable", this.invulnerable);
        tag.putBoolean("Silent", this.silent);
        tag.putBoolean("HasGravity", this.hasGravity);

        tag.putBoolean("CanSwim", this.canSwim);
        tag.putBoolean("CanFly", this.canFly);
        tag.putBoolean("Immovable", this.immovable);
        tag.putBoolean("HasPost", this.hasPost);
        if (this.postPosition != null) tag.put("PostPosition", NbtUtils.writeBlockPos(this.postPosition));
        tag.putFloat("PostRadius", this.postRadius);
        tag.putFloat("Fallback", this.fallback);
        tag.putBoolean("PatrolLoop", this.patrolLoop);
        ListTag patrolList = new ListTag();
        for (BlockPos pos : this.patrolPoints) {
            patrolList.add(NbtUtils.writeBlockPos(pos));
        }
        tag.put("PatrolPoints", patrolList);
        tag.putBoolean("LookAtPlayer", this.lookAtPlayer);
        tag.putBoolean("LookAround", this.lookAround);
        tag.putBoolean("Wander", this.wander);
        tag.putBoolean("AlwaysWander", this.alwaysWander);
        tag.putInt("RegenDelay", this.regenDelay);
        tag.putInt("RegenFrequency", this.regenFrequency);
        tag.putInt("DamageDelay", this.damageDelay);
        tag.putFloat("PathDistance", this.pathDistance);
        tag.putBoolean("CanFallDamage", this.canFallDamage);
        tag.putBoolean("CanGetBurned", this.canGetBurned);
        tag.putBoolean("Killable", this.killable);
        tag.putString("FollowTarget", this.followTarget);
        tag.putString("BehaviorMode", this.behaviorMode);
        CompoundTag tradeTag = new CompoundTag();
        tradeData.save(tradeTag);
        tag.put("TradeData", tradeTag);
    }

    public void load(CompoundTag tag) {
        this.id = tag.contains("Id") ? tag.getString("Id") : "";
        this.name = tag.contains("Name") ? tag.getString("Name") : "New NPC";
        this.dialogId = tag.contains("DialogId") ? tag.getString("DialogId") : "";
        this.faction = tag.contains("Faction") ? tag.getString("Faction") : "neutral";
        this.skin = tag.contains("Skin") ? tag.getString("Skin") : "";
        this.maxHealth = tag.contains("MaxHealth") ? tag.getFloat("MaxHealth") : 20.0f;
        this.health = Math.min(
                tag.contains("Health") ? tag.getFloat("Health") : this.maxHealth,
                this.maxHealth
        );
        this.attackDamage = tag.contains("AttackDamage") ? tag.getFloat("AttackDamage") : 2.0f;
        this.movementSpeed = tag.contains("MovementSpeed") ? tag.getFloat("MovementSpeed") : 0.4f;
        try {
            this.state = NPCState.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException e) {
            this.state = NPCState.IDLE;
        }
        this.aggressive = tag.contains("Aggressive") ? tag.getBoolean("Aggressive") : false;
        this.behaviorMode = tag.contains("BehaviorMode") ? tag.getString("BehaviorMode") : "peaceful";
        this.hostileFactions = tag.contains("HostileFactions") ? tag.getString("HostileFactions") : "";
        this.animation = tag.contains("Animation") ? tag.getString("Animation") : "";
        this.enableTrade = tag.contains("EnableTrade") ? tag.getBoolean("EnableTrade") : false;
        this.scale = tag.contains("Scale") ? tag.getFloat("Scale") : 1.0f;
        this.nameVisible = tag.contains("NameVisible") ? tag.getBoolean("NameVisible") : true;
        this.glowEnabled = tag.contains("GlowEnabled") ? tag.getBoolean("GlowEnabled") : false;
        this.noAI = tag.contains("NoAI") ? tag.getBoolean("NoAI") : false;
        this.invulnerable = tag.contains("Invulnerable") ? tag.getBoolean("Invulnerable") : false;
        this.silent = tag.contains("Silent") ? tag.getBoolean("Silent") : false;
        this.hasGravity = tag.contains("HasGravity") ? tag.getBoolean("HasGravity") : true;
        this.behaviorMode = tag.contains("BehaviorMode") ? tag.getString("BehaviorMode") : "peaceful";
        this.canSwim = tag.contains("CanSwim") ? tag.getBoolean("CanSwim") : false;
        this.canFly = tag.contains("CanFly") ? tag.getBoolean("CanFly") : false;
        this.immovable = tag.contains("Immovable") ? tag.getBoolean("Immovable") : false;
        this.hasPost = tag.contains("HasPost") ? tag.getBoolean("HasPost") : false;
        this.postPosition = tag.contains("PostPosition") ? NbtUtils.readBlockPos(tag.getCompound("PostPosition")) : null;
        this.postRadius = tag.contains("PostRadius") ? tag.getFloat("PostRadius") : 1.0f;
        this.fallback = tag.contains("Fallback") ? tag.getFloat("Fallback") : 15.0f;
        this.patrolLoop = tag.contains("PatrolLoop") ? tag.getBoolean("PatrolLoop") : false;
        this.patrolPoints.clear();
        if (tag.contains("PatrolPoints")) {
            ListTag patrolList = tag.getList("PatrolPoints", 10);
            for (int i = 0; i < patrolList.size(); i++) {
                this.patrolPoints.add(NbtUtils.readBlockPos(patrolList.getCompound(i)));
            }
        }
        this.lookAtPlayer = tag.contains("LookAtPlayer") ? tag.getBoolean("LookAtPlayer") : false;
        this.lookAround = tag.contains("LookAround") ? tag.getBoolean("LookAround") : false;
        this.wander = tag.contains("Wander") ? tag.getBoolean("Wander") : false;
        this.alwaysWander = tag.contains("AlwaysWander") ? tag.getBoolean("AlwaysWander") : false;
        this.regenDelay = tag.contains("RegenDelay") ? tag.getInt("RegenDelay") : 0;
        this.regenFrequency = tag.contains("RegenFrequency") ? tag.getInt("RegenFrequency") : 20;
        this.damageDelay = tag.contains("DamageDelay") ? tag.getInt("DamageDelay") : 20;
        this.pathDistance = tag.contains("PathDistance") ? tag.getFloat("PathDistance") : 32.0f;
        this.canFallDamage = tag.contains("CanFallDamage") ? tag.getBoolean("CanFallDamage") : true;
        this.canGetBurned = tag.contains("CanGetBurned") ? tag.getBoolean("CanGetBurned") : true;
        this.killable = tag.contains("Killable") ? tag.getBoolean("Killable") : true;
        this.followTarget = tag.contains("FollowTarget") ? tag.getString("FollowTarget") : "";

        if (tag.contains("TradeData")) {
            tradeData.load(tag.getCompound("TradeData"));
        }
    }
}