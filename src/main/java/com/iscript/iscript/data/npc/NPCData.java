package com.iscript.iscript.data.npc;

import net.minecraft.nbt.CompoundTag;

public class NPCData {
    private String name = "New NPC";
    private String dialogId = "";
    private String faction = "neutral";
    private String skin = "";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDialogId() { return dialogId; }
    public void setDialogId(String dialogId) { this.dialogId = dialogId; }

    public String getFaction() { return faction; }
    public void setFaction(String faction) { this.faction = faction; }

    public String getSkin() { return skin; }
    public void setSkin(String skin) { this.skin = skin; }

    public void save(CompoundTag tag) {
        tag.putString("Name", this.name);
        tag.putString("DialogId", this.dialogId);
        tag.putString("Faction", this.faction);
        tag.putString("Skin", this.skin);
    }

    public void load(CompoundTag tag) {
        this.name = tag.contains("Name") ? tag.getString("Name") : "New NPC";
        this.dialogId = tag.contains("DialogId") ? tag.getString("DialogId") : "";
        this.faction = tag.contains("Faction") ? tag.getString("Faction") : "neutral";
        this.skin = tag.contains("Skin") ? tag.getString("Skin") : "";
    }
}
