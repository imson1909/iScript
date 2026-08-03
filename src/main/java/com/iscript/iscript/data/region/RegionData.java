package com.iscript.iscript.data.region;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iscript.iscript.data.DataObject;
import com.iscript.iscript.data.JsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class RegionData implements DataObject {
    private String id = "";
    private String name = "Region";
    private Vec3 pos1 = new Vec3(-2.5, -2.5, -2.5);
    private Vec3 pos2 = new Vec3(2.5, 2.5, 2.5);
    private final List<RegionTrigger> triggers = new ArrayList<>();
    private int tickInterval = 20;
    private String requiredFaction = "";
    private String onEnterCutsceneId = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Vec3 getPos1() { return pos1; }
    public void setPos1(Vec3 pos) { this.pos1 = pos; }
    public Vec3 getPos2() { return pos2; }
    public void setPos2(Vec3 pos) { this.pos2 = pos; }
    public List<RegionTrigger> getTriggers() { return triggers; }
    public int getTickInterval() { return tickInterval; }
    public void setTickInterval(int interval) { this.tickInterval = interval; }
    public String getRequiredFaction() { return requiredFaction; }
    public void setRequiredFaction(String faction) { this.requiredFaction = faction; }
    public String getOnEnterCutsceneId() { return onEnterCutsceneId; }
    public void setOnEnterCutsceneId(String id) { this.onEnterCutsceneId = id != null ? id : ""; }

    public double getSizeX() { return Math.abs(pos2.x - pos1.x); }
    public double getSizeY() { return Math.abs(pos2.y - pos1.y); }
    public double getSizeZ() { return Math.abs(pos2.z - pos1.z); }

    public AABB getBounds(BlockPos anchor) {
        double minX = Math.min(pos1.x, pos2.x);
        double minY = Math.min(pos1.y, pos2.y);
        double minZ = Math.min(pos1.z, pos2.z);
        double maxX = Math.max(pos1.x, pos2.x);
        double maxY = Math.max(pos1.y, pos2.y);
        double maxZ = Math.max(pos1.z, pos2.z);
        return new AABB(anchor.getX() + minX, anchor.getY() + minY, anchor.getZ() + minZ,
                anchor.getX() + maxX, anchor.getY() + maxY, anchor.getZ() + maxZ);
    }

    public boolean isInside(BlockPos anchor, Vec3 pos) {
        return getBounds(anchor).contains(pos);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.add("pos1", JsonHelper.writeVec3(pos1));
        json.add("pos2", JsonHelper.writeVec3(pos2));
        json.addProperty("tickInterval", tickInterval);
        json.addProperty("requiredFaction", requiredFaction);
        json.addProperty("onEnterCutsceneId", onEnterCutsceneId);
        JsonArray triggersArr = new JsonArray();
        for (RegionTrigger t : triggers) triggersArr.add(t.toJson());
        json.add("triggers", triggersArr);
        return json;
    }

    public void fromJson(JsonObject json) {
        id = json.has("id") ? json.get("id").getAsString() : "";
        name = json.has("name") ? json.get("name").getAsString() : "Region";
        pos1 = JsonHelper.readVec3(json.get("pos1"), pos1);
        pos2 = JsonHelper.readVec3(json.get("pos2"), pos2);
        tickInterval = json.has("tickInterval") ? json.get("tickInterval").getAsInt() : 20;
        requiredFaction = json.has("requiredFaction") ? json.get("requiredFaction").getAsString() : "";
        onEnterCutsceneId = json.has("onEnterCutsceneId") ? json.get("onEnterCutsceneId").getAsString() : "";
        triggers.clear();
        if (json.has("triggers") && json.get("triggers").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("triggers")) {
                RegionTrigger t = new RegionTrigger();
                t.fromJson(e.getAsJsonObject());
                triggers.add(t);
            }
        }
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putString("Id", id);
        tag.putString("Name", name);
        CompoundTag p1 = new CompoundTag();
        p1.putDouble("x", pos1.x);
        p1.putDouble("y", pos1.y);
        p1.putDouble("z", pos1.z);
        tag.put("Pos1", p1);
        CompoundTag p2 = new CompoundTag();
        p2.putDouble("x", pos2.x);
        p2.putDouble("y", pos2.y);
        p2.putDouble("z", pos2.z);
        tag.put("Pos2", p2);
        tag.putInt("TickInterval", tickInterval);
        tag.putString("RequiredFaction", requiredFaction);
        tag.putString("OnEnterCutsceneId", onEnterCutsceneId);
        ListTag triggersTag = new ListTag();
        for (RegionTrigger t : triggers) triggersTag.add(t.save(new CompoundTag()));
        tag.put("Triggers", triggersTag);
        return tag;
    }

    public void load(CompoundTag tag) {
        id = tag.getString("Id");
        name = tag.getString("Name");
        CompoundTag p1 = tag.getCompound("Pos1");
        pos1 = new Vec3(p1.getDouble("x"), p1.getDouble("y"), p1.getDouble("z"));
        CompoundTag p2 = tag.getCompound("Pos2");
        pos2 = new Vec3(p2.getDouble("x"), p2.getDouble("y"), p2.getDouble("z"));
        tickInterval = tag.getInt("TickInterval");
        requiredFaction = tag.getString("RequiredFaction");
        onEnterCutsceneId = tag.getString("OnEnterCutsceneId");
        triggers.clear();
        ListTag triggersTag = tag.getList("Triggers", Tag.TAG_COMPOUND);
        for (int i = 0; i < triggersTag.size(); i++) {
            RegionTrigger t = new RegionTrigger();
            t.load(triggersTag.getCompound(i));
            triggers.add(t);
        }
    }
}