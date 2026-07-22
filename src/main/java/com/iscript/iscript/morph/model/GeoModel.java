package com.iscript.iscript.morph.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoModel {
    private String id = "";
    private String identifier = "";
    private int textureWidth = 64;
    private int textureHeight = 64;
    private final List<Bone> bones = new ArrayList<>();
    private final Map<String, Bone> boneMap = new HashMap<>();

    public static GeoModel parse(JsonObject json) {
        GeoModel model = new GeoModel();
        String format = json.has("format_version") ? json.get("format_version").getAsString() : "1.12.0";

        JsonArray geometries = null;
        if (json.has("minecraft:geometry")) {
            geometries = json.getAsJsonArray("minecraft:geometry");
        }

        if (geometries == null || geometries.isEmpty()) return model;

        JsonObject geo = geometries.get(0).getAsJsonObject();
        if (geo.has("description")) {
            JsonObject desc = geo.getAsJsonObject("description");
            model.identifier = desc.has("identifier") ? desc.get("identifier").getAsString() : "";
            model.textureWidth = desc.has("texture_width") ? desc.get("texture_width").getAsInt() : 64;
            model.textureHeight = desc.has("texture_height") ? desc.get("texture_height").getAsInt() : 64;
        }

        if (geo.has("bones")) {
            JsonArray bonesArr = geo.getAsJsonArray("bones");
            for (int i = 0; i < bonesArr.size(); i++) {
                Bone bone = Bone.parse(bonesArr.get(i).getAsJsonObject());
                model.bones.add(bone);
                model.boneMap.put(bone.getName(), bone);
            }
        }

        for (Bone bone : model.bones) {
            if (!bone.getParent().isEmpty()) {
                Bone parent = model.boneMap.get(bone.getParent());
                if (parent != null) {
                    bone.setParentBone(parent);
                    parent.getChildren().add(bone);
                }
            }
        }

        return model;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdentifier() { return identifier; }
    public int getTextureWidth() { return textureWidth; }
    public int getTextureHeight() { return textureHeight; }
    public List<Bone> getBones() { return bones; }
    public Map<String, Bone> getBoneMap() { return boneMap; }
    public Bone getBone(String name) { return boneMap.get(name); }
}