package com.iscript.iscript.client.camera;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class CutscenePath {
    public final List<Keyframe> keyframes = new ArrayList<>();
    public Interpolation easing = Interpolation.LINEAR;
    public float durationTicks = 20;
    public CameraShake shake = null;
    public boolean useLookAt = false;
    public Vec3 lookAtTarget = Vec3.ZERO;
    public boolean orbitMode = false;
    public float orbitRadius = 5f;
    public float orbitHeight = 2f;
    public float orbitSpeed = 1f;
    public Vec3 orbitCenter = Vec3.ZERO;
    public boolean useDollyZoom = false;
    public Vec3 dollyTarget = Vec3.ZERO;
    public float dollyBaseFov = 70f;
    public double dollyBaseDistance = 10f;

    public boolean constantSpeed = false;

    public static class Keyframe {
        public Vec3 point;
        public float yaw, pitch, roll, fov;
        public float tick;
        public PathType segmentType;

        public Keyframe(Vec3 point, float yaw, float pitch, float roll, float fov, float tick, PathType segmentType) {
            this.point = point;
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
            this.fov = fov;
            this.tick = tick;
            this.segmentType = segmentType;
        }
    }

    public float getDuration() { return durationTicks; }

    public void recalculateForConstantSpeed(float blocksPerSecond) {
        if (keyframes.isEmpty()) return;
        float ticksPerBlock = 20.0f / blocksPerSecond;
        float currentTick = 0;
        keyframes.get(0).tick = 0;
        for (int i = 1; i < keyframes.size(); i++) {
            double dist = keyframes.get(i-1).point.distanceTo(keyframes.get(i).point);
            currentTick += dist * ticksPerBlock;
            keyframes.get(i).tick = currentTick;
        }
        durationTicks = currentTick;
    }

    private int findSegment(float totalTicks) {
        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (totalTicks >= keyframes.get(i).tick && totalTicks < keyframes.get(i+1).tick) {
                return i;
            }
        }
        return Math.max(0, keyframes.size() - 2);
    }

    public Vec3 interpolatePosition(float progress) {
        if (keyframes.isEmpty()) return Vec3.ZERO;
        if (keyframes.size() == 1) return keyframes.get(0).point;

        float totalTicks = progress * durationTicks;
        if (totalTicks >= durationTicks) return keyframes.get(keyframes.size()-1).point;

        int index = findSegment(totalTicks);
        Keyframe k1 = keyframes.get(index);
        Keyframe k2 = keyframes.get(index + 1);
        float segDur = k2.tick - k1.tick;
        float segProg = segDur > 0 ? (totalTicks - k1.tick) / segDur : 0;
        float eased = easing.apply(segProg);

        Keyframe k0 = keyframes.get(Math.max(0, index - 1));
        Keyframe k3 = keyframes.get(Math.min(keyframes.size() - 1, index + 2));

        switch (k1.segmentType) {
            case LINEAR:
                return lerpVec3(k1.point, k2.point, eased);
            case CUBIC_BEZIER:
                return bezierPosition(index, eased);
            case HERMITE:
                return hermite(k0.point, k1.point, k2.point, k3.point, eased);
            case BSPLINE:
                return bspline(k0.point, k1.point, k2.point, k3.point, eased);
            case CIRCULAR:
                return circular(k1.point, k2.point, eased);
            case ELLIPTICAL:
                return elliptical(k1.point, k2.point, eased);
            case SPIRAL:
                return spiral(k1.point, k2.point, eased, index);
            case HELIX:
                return helix(k1.point, k2.point, eased, index);
            case STEP:
                return k1.point;
            case NONE:
                return keyframes.get(0).point;
            case CATMULL_ROM:
            default:
                return catmullRom(k0.point, k1.point, k2.point, k3.point, eased);
        }
    }

    public float[] interpolateRotation(float progress) {
        if (keyframes.isEmpty()) return new float[]{0, 0, 0};
        if (keyframes.size() == 1) {
            return new float[]{keyframes.get(0).yaw, keyframes.get(0).pitch, keyframes.get(0).roll};
        }

        float totalTicks = progress * durationTicks;
        if (totalTicks >= durationTicks) {
            Keyframe last = keyframes.get(keyframes.size()-1);
            return new float[]{last.yaw, last.pitch, last.roll};
        }

        int index = findSegment(totalTicks);
        Keyframe k1 = keyframes.get(index);
        Keyframe k2 = keyframes.get(index + 1);
        float segDur = k2.tick - k1.tick;
        float segProg = segDur > 0 ? (totalTicks - k1.tick) / segDur : 0;
        float eased = easing.apply(segProg);

        float yaw = lerpYaw(k1.yaw, k2.yaw, eased);
        float pitch = lerpPitch(k1.pitch, k2.pitch, eased);
        float roll = Mth.lerp(eased, k1.roll, k2.roll);

        return new float[]{yaw, pitch, roll};
    }

    public float interpolateFov(float progress) {
        if (keyframes.isEmpty()) return 70;
        if (keyframes.size() == 1) return keyframes.get(0).fov;

        float totalTicks = progress * durationTicks;
        if (totalTicks >= durationTicks) return keyframes.get(keyframes.size()-1).fov;

        int index = findSegment(totalTicks);
        Keyframe k1 = keyframes.get(index);
        Keyframe k2 = keyframes.get(index + 1);
        float segDur = k2.tick - k1.tick;
        float segProg = segDur > 0 ? (totalTicks - k1.tick) / segDur : 0;

        return Mth.lerp(easing.apply(segProg), k1.fov, k2.fov);
    }

    private Vec3 lerpVec3(Vec3 a, Vec3 b, float t) {
        return new Vec3(
                Mth.lerp(t, a.x, b.x),
                Mth.lerp(t, a.y, b.y),
                Mth.lerp(t, a.z, b.z)
        );
    }

    private Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        double x = cubic(p0.x, p1.x, p2.x, p3.x, t);
        double y = cubic(p0.y, p1.y, p2.y, p3.y, t);
        double z = cubic(p0.z, p1.z, p2.z, p3.z, t);
        return new Vec3(x, y, z);
    }

    private Vec3 bezierPosition(int index, float t) {
        Keyframe k0 = keyframes.get(index);
        Keyframe k1 = keyframes.get(index + 1);
        Vec3 p0 = k0.point;
        Vec3 p3 = k1.point;
        Vec3 p1 = new Vec3(
                p0.x + (p3.x - p0.x) * 0.3,
                p0.y + (p3.y - p0.y) * 0.3 + 2,
                p0.z + (p3.z - p0.z) * 0.3
        );
        Vec3 p2 = new Vec3(
                p3.x - (p3.x - p0.x) * 0.3,
                p3.y - (p3.y - p0.y) * 0.3 + 2,
                p3.z - (p3.z - p0.z) * 0.3
        );

        float invT = 1 - t;
        double x = invT * invT * invT * p0.x + 3 * invT * invT * t * p1.x + 3 * invT * t * t * p2.x + t * t * t * p3.x;
        double y = invT * invT * invT * p0.y + 3 * invT * invT * t * p1.y + 3 * invT * t * t * p2.y + t * t * t * p3.y;
        double z = invT * invT * invT * p0.z + 3 * invT * invT * t * p1.z + 3 * invT * t * t * p2.z + t * t * t * p3.z;

        return new Vec3(x, y, z);
    }

    private Vec3 hermite(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        float h0 = 2*t3 - 3*t2 + 1;
        float h1 = -2*t3 + 3*t2;
        float h2 = t3 - 2*t2 + t;
        float h3 = t3 - t2;
        Vec3 m0 = p2.subtract(p0).scale(0.5f);
        Vec3 m1 = p3.subtract(p1).scale(0.5f);
        return p1.scale(h0).add(p2.scale(h1)).add(m0.scale(h2)).add(m1.scale(h3));
    }

    private Vec3 bspline(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        float b0 = (1 - 3*t + 3*t2 - t3) / 6f;
        float b1 = (4 - 6*t2 + 3*t3) / 6f;
        float b2 = (1 + 3*t + 3*t2 - 3*t3) / 6f;
        float b3 = t3 / 6f;
        return p0.scale(b0).add(p1.scale(b1)).add(p2.scale(b2)).add(p3.scale(b3));
    }

    private Vec3 circular(Vec3 a, Vec3 b, float t) {
        Vec3 mid = lerpVec3(a, b, 0.5f);
        Vec3 dir = b.subtract(a).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = dir.cross(up);
        if (side.lengthSqr() < 0.001) side = new Vec3(1, 0, 0);
        else side = side.normalize();
        float radius = (float)a.distanceTo(b) / 2f;
        float angle = (t - 0.5f) * (float)Math.PI;
        Vec3 offset = side.scale((float)Math.sin(angle) * radius).add(up.scale((float)(1 - Math.cos(angle)) * radius * 0.3f));
        return mid.add(offset);
    }

    private Vec3 elliptical(Vec3 a, Vec3 b, float t) {
        Vec3 mid = lerpVec3(a, b, 0.5f);
        Vec3 dir = b.subtract(a).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = dir.cross(up);
        if (side.lengthSqr() < 0.001) side = new Vec3(1, 0, 0);
        else side = side.normalize();
        float rx = (float)a.distanceTo(b) / 2f;
        float ry = rx * 0.6f;
        float angle = t * (float)Math.PI * 2f;
        return mid.add(side.scale((float)Math.cos(angle) * rx)).add(up.scale((float)Math.sin(angle) * ry));
    }

    private Vec3 spiral(Vec3 a, Vec3 b, float t, int index) {
        Vec3 center = lerpVec3(a, b, 0.5f);
        float radius = 0.5f + index * 0.3f;
        float angle = t * (float)Math.PI * 4f;
        float height = (float)(b.y - a.y) * t;
        return new Vec3(
                center.x + Math.cos(angle) * radius,
                a.y + height,
                center.z + Math.sin(angle) * radius
        );
    }

    private Vec3 helix(Vec3 a, Vec3 b, float t, int index) {
        Vec3 center = lerpVec3(a, b, 0.5f);
        float radius = 1.0f;
        float angle = t * (float)Math.PI * 6f + index;
        float height = (float)(b.y - a.y) * t;
        return new Vec3(
                center.x + Math.cos(angle) * radius,
                a.y + height,
                center.z + Math.sin(angle) * radius
        );
    }

    private float lerpYaw(float a, float b, float t) {
        float diff = b - a;
        while (diff < -180f) diff += 360f;
        while (diff >= 180f) diff -= 360f;
        return a + diff * t;
    }

    private float lerpPitch(float a, float b, float t) {
        return Mth.lerp(t, a, b);
    }

    private static double cubic(double p0, double p1, double p2, double p3, float t) {
        return 0.5 * (2*p1 + (p2-p0)*t + (2*p0-5*p1+4*p2-p3)*t*t + (3*p1-p0-3*p2+p3)*t*t*t);
    }
}