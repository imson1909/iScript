package com.iscript.iscript.data.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iscript.iscript.data.DataObject;
import com.iscript.iscript.data.JsonHelper;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;

public class NPCData implements DataObject {
    private String id = "";
    private String name = "New NPC";
    private String dialogId = "";
    private String faction = "neutral";
    private String skin = "";
    private Stats stats = new Stats();
    private Behavior behavior = new Behavior();
    private Movement movement = new Movement();
    private NPCTradeData tradeData = new NPCTradeData();
    private boolean enableTrade;
    private NPCState state = NPCState.IDLE;
    private String followTarget = "";
    private String animation = "";
    private float scale = 1.0f;
    private boolean nameVisible = true;
    private boolean glowEnabled;
    private boolean canFallDamage = true;
    private boolean canGetBurned = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDialogId() { return dialogId; }
    public void setDialogId(String id) { this.dialogId = id; }
    public String getFaction() { return faction; }
    public void setFaction(String f) { this.faction = f; }
    public String getSkin() { return skin; }
    public void setSkin(String s) { this.skin = s; }
    public Stats stats() { return stats; }
    public Behavior behavior() { return behavior; }
    public Movement movement() { return movement; }
    public NPCTradeData getTradeData() { return tradeData; }
    public void setTradeData(NPCTradeData t) { this.tradeData = t; }
    public boolean isEnableTrade() { return enableTrade; }
    public void setEnableTrade(boolean v) { this.enableTrade = v; }

    public NPCState getState() { return state; }
    public void setState(NPCState s) { this.state = s; }
    public String getFollowTarget() { return followTarget; }
    public void setFollowTarget(String t) { this.followTarget = t != null ? t : ""; }
    public String getAnimation() { return animation; }
    public void setAnimation(String a) { this.animation = a != null ? a : ""; }
    public float getScale() { return scale; }
    public void setScale(float s) { this.scale = s; }
    public boolean isNameVisible() { return nameVisible; }
    public void setNameVisible(boolean v) { this.nameVisible = v; }
    public boolean isGlowEnabled() { return glowEnabled; }
    public void setGlowEnabled(boolean v) { this.glowEnabled = v; }
    public boolean isCanFallDamage() { return canFallDamage; }
    public void setCanFallDamage(boolean v) { this.canFallDamage = v; }
    public boolean isCanGetBurned() { return canGetBurned; }
    public void setCanGetBurned(boolean v) { this.canGetBurned = v; }

    public boolean isNoAI() { return behavior.noAI; }
    public void setNoAI(boolean v) { behavior.noAI = v; }
    public boolean isInvulnerable() { return behavior.invulnerable; }
    public void setInvulnerable(boolean v) { behavior.invulnerable = v; }
    public boolean isSilent() { return behavior.silent; }
    public void setSilent(boolean v) { behavior.silent = v; }
    public boolean isHasGravity() { return behavior.hasGravity; }
    public void setHasGravity(boolean v) { behavior.hasGravity = v; }
    public boolean isCanSwim() { return behavior.canSwim; }
    public void setCanSwim(boolean v) { behavior.canSwim = v; }
    public boolean isCanFly() { return behavior.canFly; }
    public void setCanFly(boolean v) { behavior.canFly = v; }
    public boolean isImmovable() { return behavior.immovable; }
    public void setImmovable(boolean v) { behavior.immovable = v; }
    public boolean isKillable() { return behavior.killable; }
    public void setKillable(boolean v) { behavior.killable = v; }
    public boolean isAggressive() { return behavior.aggressive; }
    public void setAggressive(boolean v) { behavior.aggressive = v; }
    public String getHostileFactions() { return behavior.hostileFactions; }
    public void setHostileFactions(String v) { behavior.hostileFactions = v != null ? v : ""; }
    public String getBehaviorMode() { return behavior.mode; }
    public void setBehaviorMode(String v) { behavior.mode = v != null ? v : "peaceful"; }
    public boolean isAlwaysWander() { return movement.alwaysWander; }
    public void setAlwaysWander(boolean v) { movement.alwaysWander = v; }
    public boolean isHasPost() { return movement.hasPost; }
    public void setHasPost(boolean v) { movement.hasPost = v; }
    public BlockPos getPostPosition() { return movement.postPosition; }
    public void setPostPosition(BlockPos v) { movement.postPosition = v; }
    public float getPostRadius() { return movement.postRadius; }
    public void setPostRadius(float v) { movement.postRadius = v; }
    public float getFallback() { return movement.fallback; }
    public void setFallback(float v) { movement.fallback = v; }
    public boolean isPatrolLoop() { return movement.patrolLoop; }
    public void setPatrolLoop(boolean v) { movement.patrolLoop = v; }
    public List<BlockPos> getPatrolPoints() { return movement.patrolPoints; }
    public void setPatrolPoints(List<BlockPos> v) { movement.patrolPoints.clear(); if (v != null) movement.patrolPoints.addAll(v); }
    public boolean isLookAtPlayer() { return movement.lookAtPlayer; }
    public void setLookAtPlayer(boolean v) { movement.lookAtPlayer = v; }
    public boolean isLookAround() { return movement.lookAround; }
    public void setLookAround(boolean v) { movement.lookAround = v; }
    public boolean isWander() { return movement.wander; }
    public void setWander(boolean v) { movement.wander = v; }
    public float getPathDistance() { return stats.pathDistance; }
    public void setPathDistance(float v) { stats.pathDistance = v; }
    public float getMaxHealth() { return stats.maxHealth; }
    public void setMaxHealth(float v) { stats.maxHealth = v; }
    public float getHealth() { return stats.health; }
    public void setHealth(float h) { stats.health = Math.min(h, stats.maxHealth); }
    public float getAttackDamage() { return stats.attackDamage; }
    public void setAttackDamage(float v) { stats.attackDamage = v; }
    public float getMovementSpeed() { return stats.movementSpeed; }
    public void setMovementSpeed(float v) { stats.movementSpeed = v; }
    public int getRegenDelay() { return stats.regenDelay; }
    public void setRegenDelay(int v) { stats.regenDelay = v; }
    public int getRegenFrequency() { return stats.regenFrequency; }
    public void setRegenFrequency(int v) { stats.regenFrequency = v; }
    public int getDamageDelay() { return stats.damageDelay; }
    public void setDamageDelay(int v) { stats.damageDelay = v; }

    public boolean isHostileTo(String otherFaction) {
        if (behavior.hostileFactions.isEmpty()) return false;
        for (String f : behavior.hostileFactions.split(",")) if (f.trim().equalsIgnoreCase(otherFaction)) return true;
        return false;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("dialogId", dialogId);
        json.addProperty("faction", faction);
        json.addProperty("skin", skin);
        json.add("stats", stats.toJson());
        json.add("behavior", behavior.toJson());
        json.add("movement", movement.toJson());
        json.addProperty("enableTrade", enableTrade);
        json.add("trade", tradeData.toJson());
        json.addProperty("state", state.name());
        json.addProperty("followTarget", followTarget);
        json.addProperty("animation", animation);
        json.addProperty("scale", scale);
        json.addProperty("nameVisible", nameVisible);
        json.addProperty("glowEnabled", glowEnabled);
        json.addProperty("canFallDamage", canFallDamage);
        json.addProperty("canGetBurned", canGetBurned);
        return json;
    }

    public void fromJson(JsonObject json) {
        id = json.has("id") ? json.get("id").getAsString() : "";
        name = json.has("name") ? json.get("name").getAsString() : "New NPC";
        dialogId = json.has("dialogId") ? json.get("dialogId").getAsString() : "";
        faction = json.has("faction") ? json.get("faction").getAsString() : "neutral";
        skin = json.has("skin") ? json.get("skin").getAsString() : "";
        if (json.has("stats")) stats.fromJson(json.getAsJsonObject("stats"));
        if (json.has("behavior")) behavior.fromJson(json.getAsJsonObject("behavior"));
        if (json.has("movement")) movement.fromJson(json.getAsJsonObject("movement"));
        enableTrade = json.has("enableTrade") && json.get("enableTrade").getAsBoolean();
        if (json.has("trade")) tradeData.load(json.getAsJsonObject("trade"));
        if (json.has("state")) try { state = NPCState.valueOf(json.get("state").getAsString()); } catch (Exception e) { state = NPCState.IDLE; }
        followTarget = json.has("followTarget") ? json.get("followTarget").getAsString() : "";
        animation = json.has("animation") ? json.get("animation").getAsString() : "";
        scale = json.has("scale") ? json.get("scale").getAsFloat() : 1.0f;
        nameVisible = !json.has("nameVisible") || json.get("nameVisible").getAsBoolean();
        glowEnabled = json.has("glowEnabled") && json.get("glowEnabled").getAsBoolean();
        canFallDamage = !json.has("canFallDamage") || json.get("canFallDamage").getAsBoolean();
        canGetBurned = !json.has("canGetBurned") || json.get("canGetBurned").getAsBoolean();
    }


    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("Id", id);
        tag.putString("Name", name);
        tag.putString("DialogId", dialogId);
        tag.putString("Faction", faction);
        tag.putString("Skin", skin);
        tag.putString("Stats", stats.toJson().toString());
        tag.putString("Behavior", behavior.toJson().toString());
        tag.putString("Movement", movement.toJson().toString());
        tag.putBoolean("EnableTrade", enableTrade);
        tag.putString("State", state.name());
        tag.putString("FollowTarget", followTarget);
        tag.putString("Animation", animation);
        tag.putFloat("Scale", scale);
        tag.putBoolean("NameVisible", nameVisible);
        tag.putBoolean("GlowEnabled", glowEnabled);
        tag.putBoolean("CanFallDamage", canFallDamage);
        tag.putBoolean("CanGetBurned", canGetBurned);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        id = tag.getString("Id");
        name = tag.getString("Name");
        dialogId = tag.getString("DialogId");
        faction = tag.getString("Faction");
        skin = tag.getString("Skin");
        if (tag.contains("Stats")) {
            try { stats.fromJson(com.google.gson.JsonParser.parseString(tag.getString("Stats")).getAsJsonObject()); } catch (Exception ignored) {}
        }
        if (tag.contains("Behavior")) {
            try { behavior.fromJson(com.google.gson.JsonParser.parseString(tag.getString("Behavior")).getAsJsonObject()); } catch (Exception ignored) {}
        }
        if (tag.contains("Movement")) {
            try { movement.fromJson(com.google.gson.JsonParser.parseString(tag.getString("Movement")).getAsJsonObject()); } catch (Exception ignored) {}
        }
        enableTrade = tag.getBoolean("EnableTrade");
        if (tag.contains("State")) try { state = NPCState.valueOf(tag.getString("State")); } catch (Exception e) { state = NPCState.IDLE; }
        followTarget = tag.getString("FollowTarget");
        animation = tag.getString("Animation");
        scale = tag.getFloat("Scale");
        nameVisible = tag.getBoolean("NameVisible");
        glowEnabled = tag.getBoolean("GlowEnabled");
        canFallDamage = tag.getBoolean("CanFallDamage");
        canGetBurned = tag.getBoolean("CanGetBurned");
    }


    public static class Stats {
        float maxHealth = 20, health = 20, attackDamage = 2, movementSpeed = 0.4f;
        int regenDelay, regenFrequency = 20, damageDelay = 20;
        float pathDistance = 32;

        JsonObject toJson() {
            JsonObject t = new JsonObject();
            t.addProperty("maxHealth", maxHealth);
            t.addProperty("health", health);
            t.addProperty("attackDamage", attackDamage);
            t.addProperty("movementSpeed", movementSpeed);
            t.addProperty("regenDelay", regenDelay);
            t.addProperty("regenFrequency", regenFrequency);
            t.addProperty("damageDelay", damageDelay);
            t.addProperty("pathDistance", pathDistance);
            return t;
        }

        void fromJson(JsonObject t) {
            maxHealth = t.has("maxHealth") ? t.get("maxHealth").getAsFloat() : 20;
            health = t.has("health") ? Math.min(t.get("health").getAsFloat(), maxHealth) : maxHealth;
            attackDamage = t.has("attackDamage") ? t.get("attackDamage").getAsFloat() : 2;
            movementSpeed = t.has("movementSpeed") ? t.get("movementSpeed").getAsFloat() : 0.4f;
            regenDelay = t.has("regenDelay") ? t.get("regenDelay").getAsInt() : 0;
            regenFrequency = t.has("regenFrequency") ? t.get("regenFrequency").getAsInt() : 20;
            damageDelay = t.has("damageDelay") ? t.get("damageDelay").getAsInt() : 20;
            pathDistance = t.has("pathDistance") ? t.get("pathDistance").getAsFloat() : 32;
        }
    }

    public static class Behavior {
        String mode = "peaceful";
        boolean aggressive, noAI, invulnerable, silent, hasGravity = true;
        boolean canSwim, canFly, immovable, killable = true;
        String hostileFactions = "", animation = "";

        JsonObject toJson() {
            JsonObject t = new JsonObject();
            t.addProperty("mode", mode);
            t.addProperty("aggressive", aggressive);
            t.addProperty("noAI", noAI);
            t.addProperty("invulnerable", invulnerable);
            t.addProperty("silent", silent);
            t.addProperty("hasGravity", hasGravity);
            t.addProperty("canSwim", canSwim);
            t.addProperty("canFly", canFly);
            t.addProperty("immovable", immovable);
            t.addProperty("killable", killable);
            t.addProperty("hostileFactions", hostileFactions);
            t.addProperty("animation", animation);
            return t;
        }

        void fromJson(JsonObject t) {
            mode = t.has("mode") ? t.get("mode").getAsString() : "peaceful";
            aggressive = t.has("aggressive") && t.get("aggressive").getAsBoolean();
            noAI = t.has("noAI") && t.get("noAI").getAsBoolean();
            invulnerable = t.has("invulnerable") && t.get("invulnerable").getAsBoolean();
            silent = t.has("silent") && t.get("silent").getAsBoolean();
            hasGravity = !t.has("hasGravity") || t.get("hasGravity").getAsBoolean();
            canSwim = t.has("canSwim") && t.get("canSwim").getAsBoolean();
            canFly = t.has("canFly") && t.get("canFly").getAsBoolean();
            immovable = t.has("immovable") && t.get("immovable").getAsBoolean();
            killable = !t.has("killable") || t.get("killable").getAsBoolean();
            hostileFactions = t.has("hostileFactions") ? t.get("hostileFactions").getAsString() : "";
            animation = t.has("animation") ? t.get("animation").getAsString() : "";
        }
    }

    public static class Movement {
        boolean hasPost, patrolLoop, lookAtPlayer, lookAround, wander, alwaysWander;
        BlockPos postPosition;
        float postRadius = 1, fallback = 15;
        List<BlockPos> patrolPoints = new ArrayList<>();
        String followTarget = "";

        JsonObject toJson() {
            JsonObject t = new JsonObject();
            t.addProperty("hasPost", hasPost);
            if (postPosition != null) t.add("postPosition", JsonHelper.writeBlockPos(postPosition));
            t.addProperty("postRadius", postRadius);
            t.addProperty("fallback", fallback);
            t.addProperty("patrolLoop", patrolLoop);
            JsonArray list = new JsonArray();
            for (BlockPos p : patrolPoints) list.add(JsonHelper.writeBlockPos(p));
            t.add("patrolPoints", list);
            t.addProperty("lookAtPlayer", lookAtPlayer);
            t.addProperty("lookAround", lookAround);
            t.addProperty("wander", wander);
            t.addProperty("alwaysWander", alwaysWander);
            t.addProperty("followTarget", followTarget);
            return t;
        }

        void fromJson(JsonObject t) {
            hasPost = t.has("hasPost") && t.get("hasPost").getAsBoolean();
            postPosition = t.has("postPosition") ? JsonHelper.readBlockPos(t.get("postPosition")) : null;
            postRadius = t.has("postRadius") ? t.get("postRadius").getAsFloat() : 1;
            fallback = t.has("fallback") ? t.get("fallback").getAsFloat() : 15;
            patrolLoop = t.has("patrolLoop") && t.get("patrolLoop").getAsBoolean();
            patrolPoints.clear();
            if (t.has("patrolPoints")) {
                for (JsonElement e : t.getAsJsonArray("patrolPoints")) {
                    patrolPoints.add(JsonHelper.readBlockPos(e));
                }
            }
            lookAtPlayer = t.has("lookAtPlayer") && t.get("lookAtPlayer").getAsBoolean();
            lookAround = t.has("lookAround") && t.get("lookAround").getAsBoolean();
            wander = t.has("wander") && t.get("wander").getAsBoolean();
            alwaysWander = t.has("alwaysWander") && t.get("alwaysWander").getAsBoolean();
            followTarget = t.has("followTarget") ? t.get("followTarget").getAsString() : "";
        }
    }
}