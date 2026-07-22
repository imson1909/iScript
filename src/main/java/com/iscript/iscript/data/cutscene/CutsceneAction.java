package com.iscript.iscript.data.cutscene;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iscript.iscript.data.JsonHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

public class CutsceneAction {
    private CutsceneActionType type = CutsceneActionType.DELAY;
    private Vec3 position = Vec3.ZERO;
    private float yaw, pitch, roll;
    private int duration = 20;
    private String stringValue = "";
    private int intValue;
    private float fov = 70.0f;
    private boolean useFov;
    private String interpolation = "LINEAR";
    private String pathType = "CATMULL_ROM";
    private final List<Vec3> splinePoints = new ArrayList<>();
    private final List<Float> splineYaws = new ArrayList<>();
    private final List<Float> splinePitches = new ArrayList<>();
    private float shakeTrauma = 0.5f;
    private float shakeDecay = 1.5f;
    private float shakeMaxAngle = 5.0f;
    private float shakeMaxOffset = 0.3f;
    private Vec3 lookAt = Vec3.ZERO;
    private float orbitRadius = 5.0f;
    private float orbitHeight = 2.0f;
    private float orbitSpeed = 1.0f;
    private float dollyBaseFov = 70.0f;
    private float dollyTargetDistance = 10.0f;
    private float speed = 1.0f;
    private boolean constantSpeed;

    public CutsceneActionType getType() { return type; }
    public void setType(CutsceneActionType type) { this.type = type; }
    public Vec3 getPosition() { return position; }
    public void setPosition(Vec3 v) { this.position = v; }
    public double getX() { return position.x; }
    public void setX(double x) { this.position = new Vec3(x, position.y, position.z); }
    public double getY() { return position.y; }
    public void setY(double y) { this.position = new Vec3(position.x, y, position.z); }
    public double getZ() { return position.z; }
    public void setZ(double z) { this.position = new Vec3(position.x, position.y, z); }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public float getRoll() { return roll; }
    public void setRoll(float roll) { this.roll = roll; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getStringValue() { return stringValue; }
    public void setStringValue(String v) { this.stringValue = v; }
    public int getIntValue() { return intValue; }
    public void setIntValue(int v) { this.intValue = v; }
    public float getFov() { return fov; }
    public void setFov(float fov) { this.fov = fov; }
    public boolean isUseFov() { return useFov; }
    public void setUseFov(boolean v) { this.useFov = v; }
    public String getInterpolation() { return interpolation; }
    public void setInterpolation(String v) { this.interpolation = v; }
    public String getPathType() { return pathType; }
    public void setPathType(String v) { this.pathType = v; }
    public List<Vec3> getSplinePoints() { return splinePoints; }
    public List<Float> getSplineYaws() { return splineYaws; }
    public List<Float> getSplinePitches() { return splinePitches; }
    public float getShakeTrauma() { return shakeTrauma; }
    public void setShakeTrauma(float v) { this.shakeTrauma = v; }
    public float getShakeDecay() { return shakeDecay; }
    public void setShakeDecay(float v) { this.shakeDecay = v; }
    public float getShakeMaxAngle() { return shakeMaxAngle; }
    public void setShakeMaxAngle(float v) { this.shakeMaxAngle = v; }
    public float getShakeMaxOffset() { return shakeMaxOffset; }
    public void setShakeMaxOffset(float v) { this.shakeMaxOffset = v; }
    public Vec3 getLookAt() { return lookAt; }
    public void setLookAt(Vec3 v) { this.lookAt = v; }
    public double getLookAtX() { return lookAt.x; }
    public void setLookAtX(double v) { this.lookAt = new Vec3(v, lookAt.y, lookAt.z); }
    public double getLookAtY() { return lookAt.y; }
    public void setLookAtY(double v) { this.lookAt = new Vec3(lookAt.x, v, lookAt.z); }
    public double getLookAtZ() { return lookAt.z; }
    public void setLookAtZ(double v) { this.lookAt = new Vec3(lookAt.x, lookAt.y, v); }
    public float getOrbitRadius() { return orbitRadius; }
    public void setOrbitRadius(float v) { this.orbitRadius = v; }
    public float getOrbitHeight() { return orbitHeight; }
    public void setOrbitHeight(float v) { this.orbitHeight = v; }
    public float getOrbitSpeed() { return orbitSpeed; }
    public void setOrbitSpeed(float v) { this.orbitSpeed = v; }
    public float getDollyBaseFov() { return dollyBaseFov; }
    public void setDollyBaseFov(float v) { this.dollyBaseFov = v; }
    public float getDollyTargetDistance() { return dollyTargetDistance; }
    public void setDollyTargetDistance(float v) { this.dollyTargetDistance = v; }
    public float getSpeed() { return speed; }
    public void setSpeed(float v) { this.speed = v; }
    public boolean isConstantSpeed() { return constantSpeed; }
    public void setConstantSpeed(boolean v) { this.constantSpeed = v; }

    public void addSplinePoint(Vec3 point, float yaw, float pitch) {
        splinePoints.add(point);
        splineYaws.add(yaw);
        splinePitches.add(pitch);
    }

    public boolean isCameraAction() {
        return type == CutsceneActionType.CAMERA_IDLE ||
                type == CutsceneActionType.CAMERA_PATH ||
                type == CutsceneActionType.CAMERA_LOOK ||
                type == CutsceneActionType.CAMERA_FOLLOW ||
                type == CutsceneActionType.CAMERA_ORBIT ||
                type == CutsceneActionType.CAMERA_DOLLY ||
                type == CutsceneActionType.CAMERA_SHAKE;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.add("position", JsonHelper.writeVec3(position));
        json.addProperty("yaw", yaw);
        json.addProperty("pitch", pitch);
        json.addProperty("roll", roll);
        json.addProperty("duration", duration);
        json.addProperty("stringValue", stringValue);
        json.addProperty("intValue", intValue);
        json.addProperty("fov", fov);
        json.addProperty("useFov", useFov);
        json.addProperty("interpolation", interpolation);
        json.addProperty("pathType", pathType);
        json.addProperty("shakeTrauma", shakeTrauma);
        json.addProperty("shakeDecay", shakeDecay);
        json.addProperty("shakeMaxAngle", shakeMaxAngle);
        json.addProperty("shakeMaxOffset", shakeMaxOffset);
        json.add("lookAt", JsonHelper.writeVec3(lookAt));
        json.addProperty("orbitRadius", orbitRadius);
        json.addProperty("orbitHeight", orbitHeight);
        json.addProperty("orbitSpeed", orbitSpeed);
        json.addProperty("dollyBaseFov", dollyBaseFov);
        json.addProperty("dollyTargetDistance", dollyTargetDistance);
        json.addProperty("speed", speed);
        json.addProperty("constantSpeed", constantSpeed);
        JsonArray points = new JsonArray();
        for (int i = 0; i < splinePoints.size(); i++) {
            JsonObject p = new JsonObject();
            p.add("pos", JsonHelper.writeVec3(splinePoints.get(i)));
            p.addProperty("yaw", splineYaws.get(i));
            p.addProperty("pitch", splinePitches.get(i));
            points.add(p);
        }
        json.add("splinePoints", points);
        return json;
    }

    public void fromJson(JsonObject json) {
        try { type = CutsceneActionType.valueOf(json.get("type").getAsString()); }
        catch (Exception e) { type = CutsceneActionType.DELAY; }
        position = JsonHelper.readVec3(json.get("position"), Vec3.ZERO);
        yaw = json.has("yaw") ? json.get("yaw").getAsFloat() : 0;
        pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 0;
        roll = json.has("roll") ? json.get("roll").getAsFloat() : 0;
        duration = json.has("duration") ? json.get("duration").getAsInt() : 20;
        stringValue = json.has("stringValue") ? json.get("stringValue").getAsString() : "";
        intValue = json.has("intValue") ? json.get("intValue").getAsInt() : 0;
        fov = json.has("fov") ? json.get("fov").getAsFloat() : 70.0f;
        useFov = json.has("useFov") && json.get("useFov").getAsBoolean();
        interpolation = json.has("interpolation") ? json.get("interpolation").getAsString() : "LINEAR";
        pathType = json.has("pathType") ? json.get("pathType").getAsString() : "CATMULL_ROM";
        shakeTrauma = json.has("shakeTrauma") ? json.get("shakeTrauma").getAsFloat() : 0.5f;
        shakeDecay = json.has("shakeDecay") ? json.get("shakeDecay").getAsFloat() : 1.5f;
        shakeMaxAngle = json.has("shakeMaxAngle") ? json.get("shakeMaxAngle").getAsFloat() : 5.0f;
        shakeMaxOffset = json.has("shakeMaxOffset") ? json.get("shakeMaxOffset").getAsFloat() : 0.3f;
        lookAt = JsonHelper.readVec3(json.get("lookAt"), Vec3.ZERO);
        orbitRadius = json.has("orbitRadius") ? json.get("orbitRadius").getAsFloat() : 5.0f;
        orbitHeight = json.has("orbitHeight") ? json.get("orbitHeight").getAsFloat() : 2.0f;
        orbitSpeed = json.has("orbitSpeed") ? json.get("orbitSpeed").getAsFloat() : 1.0f;
        dollyBaseFov = json.has("dollyBaseFov") ? json.get("dollyBaseFov").getAsFloat() : 70.0f;
        dollyTargetDistance = json.has("dollyTargetDistance") ? json.get("dollyTargetDistance").getAsFloat() : 10.0f;
        speed = json.has("speed") ? json.get("speed").getAsFloat() : 1.0f;
        constantSpeed = json.has("constantSpeed") && json.get("constantSpeed").getAsBoolean();
        splinePoints.clear();
        splineYaws.clear();
        splinePitches.clear();
        if (json.has("splinePoints")) {
            for (JsonElement e : json.getAsJsonArray("splinePoints")) {
                JsonObject p = e.getAsJsonObject();
                splinePoints.add(JsonHelper.readVec3(p.get("pos"), Vec3.ZERO));
                splineYaws.add(p.has("yaw") ? p.get("yaw").getAsFloat() : 0);
                splinePitches.add(p.has("pitch") ? p.get("pitch").getAsFloat() : 0);
            }
        }
    }


    public CompoundTag save(CompoundTag tag) {
        tag.putString("Type", type.name());
        tag.putDouble("X", position.x);
        tag.putDouble("Y", position.y);
        tag.putDouble("Z", position.z);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        tag.putFloat("Roll", roll);
        tag.putInt("Duration", duration);
        tag.putString("StringValue", stringValue);
        tag.putInt("IntValue", intValue);
        tag.putFloat("Fov", fov);
        tag.putBoolean("UseFov", useFov);
        tag.putString("Interpolation", interpolation);
        tag.putString("PathType", pathType);
        tag.putFloat("ShakeTrauma", shakeTrauma);
        tag.putFloat("ShakeDecay", shakeDecay);
        tag.putFloat("ShakeMaxAngle", shakeMaxAngle);
        tag.putFloat("ShakeMaxOffset", shakeMaxOffset);
        tag.putDouble("LookAtX", lookAt.x);
        tag.putDouble("LookAtY", lookAt.y);
        tag.putDouble("LookAtZ", lookAt.z);
        tag.putFloat("OrbitRadius", orbitRadius);
        tag.putFloat("OrbitHeight", orbitHeight);
        tag.putFloat("OrbitSpeed", orbitSpeed);
        tag.putFloat("DollyBaseFov", dollyBaseFov);
        tag.putFloat("DollyTargetDistance", dollyTargetDistance);
        tag.putFloat("Speed", speed);
        tag.putBoolean("ConstantSpeed", constantSpeed);
        ListTag points = new ListTag();
        for (int i = 0; i < splinePoints.size(); i++) {
            CompoundTag p = new CompoundTag();
            p.putDouble("x", splinePoints.get(i).x);
            p.putDouble("y", splinePoints.get(i).y);
            p.putDouble("z", splinePoints.get(i).z);
            p.putFloat("yaw", splineYaws.get(i));
            p.putFloat("pitch", splinePitches.get(i));
            points.add(p);
        }
        tag.put("SplinePoints", points);
        return tag;
    }

    public void load(CompoundTag tag) {
        try { type = CutsceneActionType.valueOf(tag.getString("Type")); }
        catch (Exception e) { type = CutsceneActionType.DELAY; }
        position = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
        yaw = tag.getFloat("Yaw");
        pitch = tag.getFloat("Pitch");
        roll = tag.getFloat("Roll");
        duration = tag.getInt("Duration");
        stringValue = tag.getString("StringValue");
        intValue = tag.getInt("IntValue");
        fov = tag.getFloat("Fov");
        useFov = tag.getBoolean("UseFov");
        interpolation = tag.getString("Interpolation");
        pathType = tag.getString("PathType");
        shakeTrauma = tag.getFloat("ShakeTrauma");
        shakeDecay = tag.getFloat("ShakeDecay");
        shakeMaxAngle = tag.getFloat("ShakeMaxAngle");
        shakeMaxOffset = tag.getFloat("ShakeMaxOffset");
        lookAt = new Vec3(tag.getDouble("LookAtX"), tag.getDouble("LookAtY"), tag.getDouble("LookAtZ"));
        orbitRadius = tag.getFloat("OrbitRadius");
        orbitHeight = tag.getFloat("OrbitHeight");
        orbitSpeed = tag.getFloat("OrbitSpeed");
        dollyBaseFov = tag.getFloat("DollyBaseFov");
        dollyTargetDistance = tag.getFloat("DollyTargetDistance");
        speed = tag.getFloat("Speed");
        constantSpeed = tag.getBoolean("ConstantSpeed");
        splinePoints.clear();
        splineYaws.clear();
        splinePitches.clear();
        ListTag points = tag.getList("SplinePoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < points.size(); i++) {
            CompoundTag p = points.getCompound(i);
            splinePoints.add(new Vec3(p.getDouble("x"), p.getDouble("y"), p.getDouble("z")));
            splineYaws.add(p.getFloat("yaw"));
            splinePitches.add(p.getFloat("pitch"));
        }
    }
}