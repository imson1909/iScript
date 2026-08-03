package com.iscript.iscript.client;

import com.iscript.iscript.client.camera.CutsceneCameraHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "iscript", value = Dist.CLIENT)
public class CameraHandler {
    private static boolean active = false;
    private static boolean frozen = false;
    private static Vec3 startPos = Vec3.ZERO;
    private static Vec3 targetPos = Vec3.ZERO;
    private static float startYaw, startPitch;
    private static float targetYaw, targetPitch;
    private static int totalTicks = 0;
    private static int elapsedTicks = 0;

    public static void startCamera(double x, double y, double z, float yaw, float pitch, int duration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        active = true;
        frozen = true;
        startPos = mc.player.position();
        targetPos = new Vec3(x, y, z);
        startYaw = mc.player.getYRot();
        startPitch = mc.player.getXRot();
        targetYaw = yaw;
        targetPitch = pitch;
        totalTicks = Math.max(duration, 1);
        elapsedTicks = 0;
        mc.player.setNoGravity(true);
    }

    public static void resetCamera() {
        active = false;
        frozen = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setNoGravity(false);
        }
    }

    public static void setFrozen(boolean value) {
        frozen = value;
        if (!value) {
            active = false;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.setNoGravity(false);
            }
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isFrozen() {
        return frozen;
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    private static float lerpAngle(float start, float end, float progress) {
        float diff = end - start;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return start + diff * progress;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!active || event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        elapsedTicks++;
        float rawProgress = Math.min((float) elapsedTicks / (float) totalTicks, 1.0f);
        float progress = easeInOutCubic(rawProgress);

        double nx = startPos.x + (targetPos.x - startPos.x) * progress;
        double ny = startPos.y + (targetPos.y - startPos.y) * progress;
        double nz = startPos.z + (targetPos.z - startPos.z) * progress;

        player.setPos(nx, ny, nz);
        player.setYRot(lerpAngle(startYaw, targetYaw, progress));
        player.setXRot(lerpAngle(startPitch, targetPitch, progress));

        if (elapsedTicks >= totalTicks) {
            active = false;
            frozen = false;
            player.setNoGravity(false);
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
        if (isFrozen() && !CutsceneCameraHandler.isActive()) {
            event.setCanceled(true);
        }
    }
}