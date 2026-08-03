package com.iscript.iscript.client.camera;

import net.minecraft.world.phys.Vec3;

public class CutsceneCameraMode {
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

    public CameraShake shake = null;

    public Vec3 applyPosition(float progress, Vec3 pathPos) {
        if (orbitMode) {
            return calculateOrbitPosition(progress);
        }
        return pathPos;
    }

    public float[] applyRotation(float progress, Vec3 currentPos, float pathYaw, float pathPitch, float pathRoll) {
        if (orbitMode && orbitCenter != null) {
            return calculateLookAt(currentPos, orbitCenter, pathRoll);
        }
        if (useLookAt && lookAtTarget != null) {
            return calculateLookAt(currentPos, lookAtTarget, pathRoll);
        }
        return new float[]{pathYaw, pathPitch, pathRoll};
    }

    public float applyFov(float progress, float pathFov, Vec3 currentPos) {
        if (useDollyZoom && dollyTarget != null && dollyBaseDistance > 0) {
            double currentDist = currentPos.distanceTo(dollyTarget);
            if (currentDist > 0) {
                return (float) (dollyBaseFov * (dollyBaseDistance / currentDist));
            }
        }
        return pathFov;
    }

    private Vec3 calculateOrbitPosition(float progress) {
        float angle = progress * orbitSpeed * 360f;
        float rad = (float) Math.toRadians(angle);
        double x = orbitCenter.x + Math.cos(rad) * orbitRadius;
        double z = orbitCenter.z + Math.sin(rad) * orbitRadius;
        double y = orbitCenter.y + orbitHeight;
        return new Vec3(x, y, z);
    }

    private float[] calculateLookAt(Vec3 cameraPos, Vec3 target, float roll) {
        double dx = target.x - cameraPos.x;
        double dy = target.y - cameraPos.y;
        double dz = target.z - cameraPos.z;

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        return new float[]{yaw, pitch, roll};
    }
}