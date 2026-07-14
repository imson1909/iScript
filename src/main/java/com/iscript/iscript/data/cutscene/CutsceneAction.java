package com.iscript.iscript.data.cutscene;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class CutsceneAction {
    private CutsceneActionType type = CutsceneActionType.DELAY;
    private double x, y, z;
    private float yaw, pitch, roll;
    private int duration = 20;
    private String stringValue = "";
    private int intValue = 0;
    private float fov = 70.0f;
    private boolean useFov = false;
    private String interpolation = "LINEAR";
    private String pathType = "CATMULL_ROM";

    private List<Vec3> splinePoints = new ArrayList<>();
    private List<Float> splineYaws = new ArrayList<>();
    private List<Float> splinePitches = new ArrayList<>();

    private float shakeTrauma = 0.5f;
    private float shakeDecay = 1.5f;
    private float shakeMaxAngle = 5.0f;
    private float shakeMaxOffset = 0.3f;

    private double lookAtX = 0, lookAtY = 0, lookAtZ = 0;

    private float orbitRadius = 5.0f;
    private float orbitHeight = 2.0f;
    private float orbitSpeed = 1.0f;

    private float dollyBaseFov = 70.0f;
    private float dollyTargetDistance = 10.0f;

    private float speed = 1.0f;
    private boolean constantSpeed = false;

    public CutsceneActionType getType() { return type; }
    public void setType(CutsceneActionType type) { this.type = type; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
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

    public double getLookAtX() { return lookAtX; }
    public void setLookAtX(double v) { this.lookAtX = v; }
    public double getLookAtY() { return lookAtY; }
    public void setLookAtY(double v) { this.lookAtY = v; }
    public double getLookAtZ() { return lookAtZ; }
    public void setLookAtZ(double v) { this.lookAtZ = v; }

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

    public Vec3 getPosition() { return new Vec3(x, y, z); }
    public void setPosition(Vec3 pos) { this.x = pos.x; this.y = pos.y; this.z = pos.z; }
    public Vec3 getLookAt() { return new Vec3(lookAtX, lookAtY, lookAtZ); }
    public void setLookAt(Vec3 v) { this.lookAtX = v.x; this.lookAtY = v.y; this.lookAtZ = v.z; }

    public boolean isCameraAction() {
        return type == CutsceneActionType.CAMERA_IDLE ||
                type == CutsceneActionType.CAMERA_PATH ||
                type == CutsceneActionType.CAMERA_LOOK ||
                type == CutsceneActionType.CAMERA_FOLLOW ||
                type == CutsceneActionType.CAMERA_ORBIT ||
                type == CutsceneActionType.CAMERA_DOLLY ||
                type == CutsceneActionType.CAMERA_SHAKE;
    }

    public void save(CompoundTag tag) {
        tag.putString("Type", type.name());
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
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
        tag.putDouble("LookAtX", lookAtX);
        tag.putDouble("LookAtY", lookAtY);
        tag.putDouble("LookAtZ", lookAtZ);
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
            p.putFloat("Yaw", splineYaws.get(i));
            p.putFloat("Pitch", splinePitches.get(i));
            points.add(p);
        }
        tag.put("SplinePoints", points);
    }

    public void load(CompoundTag tag) {
        try {
            type = CutsceneActionType.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException e) {
            type = CutsceneActionType.DELAY;
        }
        x = tag.getDouble("X");
        y = tag.getDouble("Y");
        z = tag.getDouble("Z");
        yaw = tag.getFloat("Yaw");
        pitch = tag.getFloat("Pitch");
        roll = tag.getFloat("Roll");
        duration = tag.getInt("Duration");
        stringValue = tag.getString("StringValue");
        intValue = tag.getInt("IntValue");
        fov = tag.getFloat("Fov");
        useFov = tag.getBoolean("UseFov");
        interpolation = tag.getString("Interpolation");
        if (interpolation.isEmpty()) interpolation = "LINEAR";
        pathType = tag.getString("PathType");
        if (pathType.isEmpty()) pathType = "CATMULL_ROM";
        shakeTrauma = tag.getFloat("ShakeTrauma");
        shakeDecay = tag.getFloat("ShakeDecay");
        shakeMaxAngle = tag.getFloat("ShakeMaxAngle");
        shakeMaxOffset = tag.getFloat("ShakeMaxOffset");
        lookAtX = tag.getDouble("LookAtX");
        lookAtY = tag.getDouble("LookAtY");
        lookAtZ = tag.getDouble("LookAtZ");
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
        ListTag points = tag.getList("SplinePoints", 10);
        for (int i = 0; i < points.size(); i++) {
            CompoundTag p = points.getCompound(i);
            splinePoints.add(new Vec3(p.getDouble("x"), p.getDouble("y"), p.getDouble("z")));
            splineYaws.add(p.getFloat("Yaw"));
            splinePitches.add(p.getFloat("Pitch"));
        }
    }
}