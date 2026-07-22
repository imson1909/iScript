package com.iscript.iscript.morph.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class Cube {
    private float[] origin = new float[3];
    private float[] size = new float[3];
    private float[] uv = new float[2];
    private float inflate = 0;
    private boolean mirror = false;
    private final Map<String, FaceUv> faceUvs = new HashMap<>();

    public static class FaceUv {
        public final float[] uv = new float[2];
        public final float[] uvSize = new float[2];
    }

    public static Cube parse(JsonObject json) {
        Cube c = new Cube();
        if (json.has("origin")) {
            JsonArray arr = json.getAsJsonArray("origin");
            for (int i = 0; i < 3; i++) c.origin[i] = arr.get(i).getAsFloat();
        }
        if (json.has("size")) {
            JsonArray arr = json.getAsJsonArray("size");
            for (int i = 0; i < 3; i++) c.size[i] = arr.get(i).getAsFloat();
        }
        if (json.has("inflate")) c.inflate = json.get("inflate").getAsFloat();
        if (json.has("mirror")) c.mirror = json.get("mirror").getAsBoolean();

        JsonElement uvEl = json.get("uv");
        if (uvEl != null) {
            if (uvEl.isJsonArray()) {
                JsonArray arr = uvEl.getAsJsonArray();
                c.uv[0] = arr.get(0).getAsFloat();
                c.uv[1] = arr.get(1).getAsFloat();
            } else if (uvEl.isJsonObject()) {
                JsonObject obj = uvEl.getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    String face = e.getKey();
                    JsonObject faceObj = e.getValue().getAsJsonObject();
                    FaceUv f = new FaceUv();
                    if (faceObj.has("uv")) {
                        JsonArray arr = faceObj.getAsJsonArray("uv");
                        f.uv[0] = arr.get(0).getAsFloat();
                        f.uv[1] = arr.get(1).getAsFloat();
                    }
                    if (faceObj.has("uv_size")) {
                        JsonArray arr = faceObj.getAsJsonArray("uv_size");
                        f.uvSize[0] = arr.get(0).getAsFloat();
                        f.uvSize[1] = arr.get(1).getAsFloat();
                    }
                    c.faceUvs.put(face, f);
                }
            }
        }
        return c;
    }

    public float[] getOrigin() { return origin; }
    public float[] getSize() { return size; }
    public float[] getUv() { return uv; }
    public float getInflate() { return inflate; }
    public boolean isMirror() { return mirror; }
    public Map<String, FaceUv> getFaceUvs() { return faceUvs; }
    public boolean hasPerFaceUv() { return !faceUvs.isEmpty(); }
}