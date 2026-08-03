package com.iscript.iscript.data;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;

public interface DataObject {
    String getId();
    void setId(String id);
    JsonObject toJson();
    void fromJson(JsonObject json);
    default CompoundTag save(CompoundTag tag) {
        tag.putString("Id", getId());
        return tag;
    }
    default void load(CompoundTag tag) {
        setId(tag.getString("Id"));
    }
}