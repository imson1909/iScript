package com.iscript.iscript.client.camera;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.iscript.iscript.network.packet.ServerCommandPacket;
import net.minecraft.nbt.CompoundTag;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CutsceneCameraHandler {
    private static final Minecraft mc = Minecraft.getInstance();
    private static boolean active = false;
    private static boolean paused = false;
    private static CutscenePath path;
    private static CutsceneCameraMode mode;
    private static int tickCounter = 0;
    private static int storedFov = 70;
    private static boolean finished = false;
    private static float speedMultiplier = 1.0f;
    private static float currentRoll = 0f;
    private static float currentFov = 70f;
    private static boolean freezePlayer = false;
    private static int resumeTick = 0;
    private static Vec3 startPosition = Vec3.ZERO;
    private static float startYaw = 0f;
    private static float startPitch = 0f;

    public static void start(CutscenePath path, CutsceneCameraMode mode, boolean freeze) {
        CutsceneCameraHandler.path = path;
        CutsceneCameraHandler.mode = mode != null ? mode : new CutsceneCameraMode();
        active = true;
        paused = false;
        finished = false;
        tickCounter = 0;
        resumeTick = 0;
        speedMultiplier = 1.0f;
        currentRoll = 0f;
        currentFov = 70f;
        freezePlayer = freeze;

        if (mc.options != null) {
            storedFov = mc.options.fov().get();
        }
        if (mc.player != null) {
            startPosition = mc.player.position();
            startYaw = mc.player.getYRot();
            startPitch = mc.player.getXRot();
        }

        if (mode != null && mode.shake != null) {
            mode.shake.setTrauma(0f);
        }
    }

    public static void start(CutscenePath path, CutsceneCameraMode mode, boolean freeze, float speed) {
        start(path, mode, freeze);
        speedMultiplier = speed;
    }

    public static void stop() {
        if (!active) return;
        active = false;
        paused = false;
        finished = false;
        path = null;
        mode = null;
        tickCounter = 0;
        resumeTick = 0;
        speedMultiplier = 1.0f;
        currentRoll = 0f;
        currentFov = 70f;
        freezePlayer = false;
        startPosition = Vec3.ZERO;
        startYaw = 0f;
        startPitch = 0f;

        if (mc.options != null) {
            mc.options.fov().set(storedFov);
        }
    }

    public static void pause() {
        paused = true;
    }

    public static void resume(float speed, int startTick) {
        speedMultiplier = speed;
        resumeTick = startTick;
        tickCounter = startTick;
        paused = false;
    }

    public static boolean isActive() { return active; }
    public static boolean isPaused() { return paused; }

    public static void setSpeed(float speed) {
        speedMultiplier = speed;
    }

    public static Vec3 getStartPosition() { return startPosition; }
    public static float getStartYaw() { return startYaw; }
    public static float getStartPitch() { return startPitch; }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && active && !paused && !finished) {
            tickCounter++;
            if (mode != null && mode.shake != null) {
                mode.shake.update(0.05f);
            }
            if (freezePlayer && mc.player != null) {
                mc.player.input.forwardImpulse = 0;
                mc.player.input.leftImpulse = 0;
                mc.player.input.jumping = false;
                mc.player.input.shiftKeyDown = false;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!active || paused || path == null || mc.player == null) return;
        if (event.phase != TickEvent.Phase.START) return;

        float partialTick = mc.getFrameTime();
        float totalTicks = (tickCounter + partialTick) * speedMultiplier;

        if (totalTicks >= path.getDuration()) {
            if (!finished) {
                finished = true;
                stop();
                IScriptNetwork.sendToServer(new ServerCommandPacket(ServerCommandPacket.Type.CUTSCENE_FINISHED, new CompoundTag()));
            }
            return;
        }

        float progress = totalTicks / path.getDuration();

        Vec3 pathPos = path.interpolatePosition(progress);
        float[] pathRot = path.interpolateRotation(progress);
        float pathFov = path.interpolateFov(progress);

        Vec3 pos = mode != null ? mode.applyPosition(progress, pathPos) : pathPos;
        float[] rotation = mode != null ? mode.applyRotation(progress, pos, pathRot[0], pathRot[1], pathRot[2]) : pathRot;
        float yaw = rotation[0];
        float pitch = rotation[1];
        float roll = rotation[2];
        float fov = mode != null ? mode.applyFov(progress, pathFov, pos) : pathFov;

        if (mode != null && mode.shake != null && mode.shake.isActive()) {
            float time = totalTicks / 20f;
            Vec3 shakeOffset = mode.shake.getOffset(time);
            pos = pos.add(shakeOffset);
            yaw += mode.shake.getYawShake(time);
            pitch += mode.shake.getPitchShake(time);
            roll += mode.shake.getRollShake(time);
        }

        currentRoll = roll;
        currentFov = fov;
        applyCamera(pos, yaw, pitch, roll, fov);
    }

    private static void applyCamera(Vec3 pos, float yaw, float pitch, float roll, float fov) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        player.setPos(pos.x, pos.y, pos.z);
        player.setOldPosAndRot();
        player.setYRot(yaw % 360.0f);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);

        if (player.connection != null) {
            player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    pos.x, pos.y, pos.z, yaw, pitch, player.onGround()
            ));
        }
    }

    public static float getCurrentRoll() {
        return currentRoll;
    }

    public static float getCurrentFov() {
        return currentFov;
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (active) {
            event.setFOV(currentFov);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (active) {
            event.setRoll(currentRoll);
        }
    }
}