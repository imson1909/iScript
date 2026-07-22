package com.iscript.iscript.data.cutscene;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iscript.iscript.data.DataObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

public class CutsceneData implements DataObject {
    private String id = "";
    private String name = "Cutscene";
    private boolean loop;
    private final List<CutsceneAction> actions = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isLoop() { return loop; }
    public void setLoop(boolean loop) { this.loop = loop; }
    public List<CutsceneAction> getActions() { return actions; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("loop", loop);
        JsonArray arr = new JsonArray();
        for (CutsceneAction a : actions) arr.add(a.toJson());
        json.add("actions", arr);
        return json;
    }

    public void fromJson(JsonObject json) {
        id = json.has("id") ? json.get("id").getAsString() : "";
        name = json.has("name") ? json.get("name").getAsString() : "Cutscene";
        loop = json.has("loop") && json.get("loop").getAsBoolean();
        actions.clear();
        if (json.has("actions")) {
            for (JsonElement e : json.getAsJsonArray("actions")) {
                CutsceneAction a = new CutsceneAction();
                a.fromJson(e.getAsJsonObject());
                actions.add(a);
            }
        }
    }


    public CompoundTag save(CompoundTag tag) {
        tag.putString("Id", id);
        tag.putString("Name", name);
        tag.putBoolean("Loop", loop);
        ListTag list = new ListTag();
        for (CutsceneAction a : actions) {
            CompoundTag t = new CompoundTag();
            t.putString("Type", a.getType().name());
            t.putDouble("X", a.getX());
            t.putDouble("Y", a.getY());
            t.putDouble("Z", a.getZ());
            t.putFloat("Yaw", a.getYaw());
            t.putFloat("Pitch", a.getPitch());
            t.putFloat("Roll", a.getRoll());
            t.putInt("Duration", a.getDuration());
            t.putString("StringValue", a.getStringValue());
            t.putInt("IntValue", a.getIntValue());
            t.putFloat("Fov", a.getFov());
            t.putBoolean("UseFov", a.isUseFov());
            t.putString("Interpolation", a.getInterpolation());
            t.putString("PathType", a.getPathType());
            list.add(t);
        }
        tag.put("Actions", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        id = tag.getString("Id");
        name = tag.getString("Name");
        loop = tag.getBoolean("Loop");
        actions.clear();
        ListTag list = tag.getList("Actions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            CutsceneAction a = new CutsceneAction();
            try { a.setType(CutsceneActionType.valueOf(t.getString("Type"))); }
            catch (Exception e) { a.setType(CutsceneActionType.DELAY); }
            a.setX(t.getDouble("X"));
            a.setY(t.getDouble("Y"));
            a.setZ(t.getDouble("Z"));
            a.setYaw(t.getFloat("Yaw"));
            a.setPitch(t.getFloat("Pitch"));
            a.setRoll(t.getFloat("Roll"));
            a.setDuration(t.getInt("Duration"));
            a.setStringValue(t.getString("StringValue"));
            a.setIntValue(t.getInt("IntValue"));
            a.setFov(t.getFloat("Fov"));
            a.setUseFov(t.getBoolean("UseFov"));
            a.setInterpolation(t.getString("Interpolation"));
            a.setPathType(t.getString("PathType"));
            actions.add(a);
        }
    }
}