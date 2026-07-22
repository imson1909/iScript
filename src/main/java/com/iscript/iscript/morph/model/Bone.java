package com.iscript.iscript.morph.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Bone {
    private String name = "";
    private String parent = "";
    private float[] pivot = new float[3];
    private float[] rotation = new float[3];
    private final List<Cube> cubes = new ArrayList<>();
    private final List<Bone> children = new ArrayList<>();
    private Bone parentBone;

    public static Bone parse(JsonObject json) {
        Bone b = new Bone();
        b.name = json.has("name") ? json.get("name").getAsString() : "";
        b.parent = json.has("parent") ? json.get("parent").getAsString() : "";
        if (json.has("pivot")) {
            JsonArray arr = json.getAsJsonArray("pivot");
            for (int i = 0; i < 3; i++) b.pivot[i] = arr.get(i).getAsFloat();
        }
        if (json.has("rotation")) {
            JsonArray arr = json.getAsJsonArray("rotation");
            for (int i = 0; i < 3; i++) b.rotation[i] = arr.get(i).getAsFloat();
        }
        if (json.has("cubes")) {
            JsonArray arr = json.getAsJsonArray("cubes");
            for (int i = 0; i < arr.size(); i++) {
                b.cubes.add(Cube.parse(arr.get(i).getAsJsonObject()));
            }
        }
        return b;
    }

    public String getName() { return name; }
    public String getParent() { return parent; }
    public float[] getPivot() { return pivot; }
    public float[] getRotation() { return rotation; }
    public List<Cube> getCubes() { return cubes; }
    public List<Bone> getChildren() { return children; }
    public Bone getParentBone() { return parentBone; }
    public void setParentBone(Bone b) { this.parentBone = b; }

    public Matrix4f getLocalTransform(float animRotX, float animRotY, float animRotZ) {
        Matrix4f m = new Matrix4f();
        m.translate(pivot[0] / 16f, pivot[1] / 16f, pivot[2] / 16f);

        float rx = (float) Math.toRadians(rotation[0] - animRotX);
        float ry = (float) Math.toRadians(rotation[1] - animRotY);
        float rz = (float) Math.toRadians(rotation[2] + animRotZ);

        m.rotate(new Quaternionf().rotationZYX(rz, ry, rx));
        m.translate(-pivot[0] / 16f, -pivot[1] / 16f, -pivot[2] / 16f);
        return m;
    }
}