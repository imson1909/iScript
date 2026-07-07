package com.iscript.iscript.data.cutscene;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class CutsceneAction {
    private CutsceneActionType type = CutsceneActionType.DELAY;
    private double x, y, z;
    private float yaw, pitch;
    private int duration = 20;
    private String stringValue = "";
    private int intValue = 0;
    private boolean useSpline = false;
    private double splineTension = 0.5;
    private boolean autoLook = true;
    private float fov = 70.0f;
    private boolean useFov = false;
    private List<Vec3> splinePoints = new ArrayList<>();
    private List<Float> splineYaws = new ArrayList<>();
    private List<Float> splinePitches = new ArrayList<>();

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
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getStringValue() { return stringValue; }
    public void setStringValue(String v) { this.stringValue = v; }
    public int getIntValue() { return intValue; }
    public void setIntValue(int v) { this.intValue = v; }
    public boolean isUseSpline() { return useSpline; }
    public void setUseSpline(boolean useSpline) { this.useSpline = useSpline; }
    public double getSplineTension() { return splineTension; }
    public void setSplineTension(double splineTension) { this.splineTension = splineTension; }
    public boolean isAutoLook() { return autoLook; }
    public void setAutoLook(boolean autoLook) { this.autoLook = autoLook; }
    public float getFov() { return fov; }
    public void setFov(float fov) { this.fov = fov; }
    public boolean isUseFov() { return useFov; }
    public void setUseFov(boolean useFov) { this.useFov = useFov; }
    public List<Vec3> getSplinePoints() { return splinePoints; }
    public List<Float> getSplineYaws() { return splineYaws; }
    public List<Float> getSplinePitches() { return splinePitches; }

    public void addSplinePoint(Vec3 point, float yaw, float pitch) {
        splinePoints.add(point);
        splineYaws.add(yaw);
        splinePitches.add(pitch);
    }

    public void save(CompoundTag tag) {
        tag.putString("Type", type.name());
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        tag.putInt("Duration", duration);
        tag.putString("StringValue", stringValue);
        tag.putInt("IntValue", intValue);
        tag.putBoolean("UseSpline", useSpline);
        tag.putDouble("SplineTension", splineTension);
        tag.putBoolean("AutoLook", autoLook);
        tag.putFloat("Fov", fov);
        tag.putBoolean("UseFov", useFov);
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
        duration = tag.getInt("Duration");
        stringValue = tag.getString("StringValue");
        intValue = tag.getInt("IntValue");
        useSpline = tag.getBoolean("UseSpline");
        splineTension = tag.getDouble("SplineTension");
        autoLook = tag.getBoolean("AutoLook");
        fov = tag.getFloat("Fov");
        useFov = tag.getBoolean("UseFov");
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
