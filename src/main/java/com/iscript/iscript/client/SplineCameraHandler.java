package com.iscript.iscript.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "iscript", value = Dist.CLIENT)
public class SplineCameraHandler {
    private static boolean active = false;
    private static boolean frozen = false;
    private static List<Vec3> points = new ArrayList<>();
    private static List<Float> yaws = new ArrayList<>();
    private static List<Float> pitches = new ArrayList<>();
    private static int totalTicks = 0;
    private static int elapsedTicks = 0;
    private static double tension = 0.5;
    private static boolean autoLook = true;
    private static float fovOverride = 70.0f;
    private static boolean fovActive = false;
    private static float lastPartialTick = 0f;
    private static Vec3 lastPos = Vec3.ZERO;
    private static float lastYaw = 0;
    private static float lastPitch = 0;

    public static void startSpline(List<Vec3> path, List<Float> pathYaws, List<Float> pathPitches, int duration, double splineTension, boolean lookAuto) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || path.size() < 2) return;
        active = true;
        frozen = true;
        points = new ArrayList<>(path);
        yaws = pathYaws != null ? new ArrayList<>(pathYaws) : new ArrayList<>();
        pitches = pathPitches != null ? new ArrayList<>(pathPitches) : new ArrayList<>();
        while (yaws.size() < points.size()) yaws.add(mc.player.getYRot());
        while (pitches.size() < points.size()) pitches.add(mc.player.getXRot());
        totalTicks = Math.max(duration, 1);
        elapsedTicks = 0;
        tension = splineTension;
        autoLook = lookAuto;
        lastPos = mc.player.position();
        lastYaw = mc.player.getYRot();
        lastPitch = mc.player.getXRot();
        mc.player.setNoGravity(true);
    }

    public static void resetCamera() {
        active = false;
        frozen = false;
        points.clear();
        yaws.clear();
        pitches.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setNoGravity(false);
        }
    }

    public static void setFrozen(boolean value) {
        frozen = value;
        if (!value && !active) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.setNoGravity(false);
            }
        }
    }

    public static boolean isActive() { return active; }
    public static boolean isFrozen() { return frozen; }
    public static void setFov(float fov) { fovOverride = fov; fovActive = true; }
    public static void clearFov() { fovActive = false; }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    private static float easeInOutQuint(float t) {
        return t < 0.5f ? 16 * t * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 5) / 2;
    }

    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t, double tau) {
        double t2 = t * t;
        double t3 = t2 * t;
        double a0 = -tau * t3 + 2 * tau * t2 - tau * t;
        double a1 = (2 - tau) * t3 + (tau - 3) * t2 + 1;
        double a2 = (tau - 2) * t3 + (3 - 2 * tau) * t2 + tau * t;
        double a3 = tau * t3 - tau * t2;
        return new Vec3(
            a0 * p0.x + a1 * p1.x + a2 * p2.x + a3 * p3.x,
            a0 * p0.y + a1 * p1.y + a2 * p2.y + a3 * p3.y,
            a0 * p0.z + a1 * p1.z + a2 * p2.z + a3 * p3.z
        );
    }

    private static float catmullRomFloat(float p0, float p1, float p2, float p3, double t, double tau) {
        double t2 = t * t;
        double t3 = t2 * t;
        double a0 = -tau * t3 + 2 * tau * t2 - tau * t;
        double a1 = (2 - tau) * t3 + (tau - 3) * t2 + 1;
        double a2 = (tau - 2) * t3 + (3 - 2 * tau) * t2 + tau * t;
        double a3 = tau * t3 - tau * t2;
        return (float) (a0 * p0 + a1 * p1 + a2 * p2 + a3 * p3);
    }

    private static Vec3 getSplinePoint(double globalT) {
        int n = points.size();
        if (n == 0) return Vec3.ZERO;
        if (n == 1) return points.get(0);
        if (n == 2) {
            Vec3 a = points.get(0);
            Vec3 b = points.get(1);
            return new Vec3(a.x + (b.x - a.x) * globalT, a.y + (b.y - a.y) * globalT, a.z + (b.z - a.z) * globalT);
        }

        double segmentCount = n - 1;
        double tScaled = globalT * segmentCount;
        int segment = (int) Math.floor(tScaled);
        double localT = tScaled - segment;
        if (segment >= n - 1) {
            segment = n - 2;
            localT = 1.0;
        }

        Vec3 p0 = segment > 0 ? points.get(segment - 1) : points.get(0);
        Vec3 p1 = points.get(segment);
        Vec3 p2 = points.get(segment + 1);
        Vec3 p3 = segment + 2 < n ? points.get(segment + 2) : p2;

        return catmullRom(p0, p1, p2, p3, localT, tension);
    }

    private static float getSplineYaw(double globalT) {
        int n = yaws.size();
        if (n <= 1) return yaws.isEmpty() ? 0 : yaws.get(0);
        double segmentCount = n - 1;
        double tScaled = globalT * segmentCount;
        int segment = (int) Math.floor(tScaled);
        double localT = tScaled - segment;
        if (segment >= n - 1) {
            segment = n - 2;
            localT = 1.0;
        }
        float p0 = segment > 0 ? yaws.get(segment - 1) : yaws.get(0);
        float p1 = yaws.get(segment);
        float p2 = yaws.get(segment + 1);
        float p3 = segment + 2 < n ? yaws.get(segment + 2) : p2;
        return catmullRomFloat(p0, p1, p2, p3, localT, tension);
    }

    private static float getSplinePitch(double globalT) {
        int n = pitches.size();
        if (n <= 1) return pitches.isEmpty() ? 0 : pitches.get(0);
        double segmentCount = n - 1;
        double tScaled = globalT * segmentCount;
        int segment = (int) Math.floor(tScaled);
        double localT = tScaled - segment;
        if (segment >= n - 1) {
            segment = n - 2;
            localT = 1.0;
        }
        float p0 = segment > 0 ? pitches.get(segment - 1) : pitches.get(0);
        float p1 = pitches.get(segment);
        float p2 = pitches.get(segment + 1);
        float p3 = segment + 2 < n ? pitches.get(segment + 2) : p2;
        return catmullRomFloat(p0, p1, p2, p3, localT, tension);
    }

    private static float[] computeAutoLook(double globalT) {
        double delta = 0.005;
        Vec3 here = getSplinePoint(Math.max(0, globalT - delta));
        Vec3 ahead = getSplinePoint(Math.min(1, globalT + delta));
        double dx = ahead.x - here.x;
        double dy = ahead.y - here.y;
        double dz = ahead.z - here.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = dist < 0.0001 ? lastYaw : (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, dist));
        return new float[]{yaw, pitch};
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!active || event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        elapsedTicks++;
        float rawProgress = Math.min((float) elapsedTicks / (float) totalTicks, 1.0f);
        float progress = easeInOutQuint(rawProgress);

        Vec3 targetPos = getSplinePoint(progress);
        float targetYaw;
        float targetPitch;

        if (autoLook) {
            float[] look = computeAutoLook(progress);
            targetYaw = look[0];
            targetPitch = look[1];
        } else {
            targetYaw = getSplineYaw(progress);
            targetPitch = getSplinePitch(progress);
        }

        float smooth = 0.3f;
        Vec3 smoothedPos = new Vec3(
            lastPos.x + (targetPos.x - lastPos.x) * smooth,
            lastPos.y + (targetPos.y - lastPos.y) * smooth,
            lastPos.z + (targetPos.z - lastPos.z) * smooth
        );
        float smoothedYaw = lastYaw + (targetYaw - lastYaw) * smooth;
        float smoothedPitch = lastPitch + (targetPitch - lastPitch) * smooth;

        player.setPos(smoothedPos.x, smoothedPos.y, smoothedPos.z);
        player.setYRot(smoothedYaw);
        player.setXRot(smoothedPitch);

        lastPos = smoothedPos;
        lastYaw = smoothedYaw;
        lastPitch = smoothedPitch;

        if (elapsedTicks >= totalTicks) {
            active = false;
            frozen = false;
            player.setNoGravity(false);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (fovActive && active) {
            event.setFOV(fovOverride);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (frozen) {
            event.getInput().up = false;
            event.getInput().down = false;
            event.getInput().left = false;
            event.getInput().right = false;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
            event.getInput().forwardImpulse = 0;
            event.getInput().leftImpulse = 0;
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!frozen) return;
        if (event.getScreen() instanceof PauseScreen) return;
        if (event.getScreen() instanceof DeathScreen) return;
        if (event.getScreen() instanceof WinScreen) return;
        if (event.getScreen() instanceof InBedChatScreen) return;
        if (event.getScreen() instanceof InventoryScreen) return;
        event.setCanceled(true);
    }
}
