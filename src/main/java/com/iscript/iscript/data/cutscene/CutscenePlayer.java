package com.iscript.iscript.data.cutscene;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.client.camera.CutsceneCameraMode;
import com.iscript.iscript.client.camera.CutscenePath;
import com.iscript.iscript.client.camera.Interpolation;
import com.iscript.iscript.client.camera.PathType;
import com.iscript.iscript.client.camera.CameraShake;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CutscenePlayer {
    private static final Map<ServerPlayer, CutsceneInstance> activeCutscenes = new HashMap<>();

    public static void play(ServerPlayer player, CutsceneData cutscene) {
        if (activeCutscenes.containsKey(player)) return;
        activeCutscenes.put(player, new CutsceneInstance(player, cutscene));
    }

    public static void stop(ServerPlayer player) {
        CutsceneInstance inst = activeCutscenes.remove(player);
        if (inst != null) inst.stop();
    }

    public static boolean isPlaying(ServerPlayer player) {
        return activeCutscenes.containsKey(player);
    }

    public static void tick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            CutsceneInstance inst = activeCutscenes.get(player);
            if (inst != null && !inst.tick()) {
                if (!inst.cutscene.isLoop()) {
                    activeCutscenes.remove(player);
                }
            }
        }
    }

    private static class CutsceneInstance {
        private final ServerPlayer player;
        private final CutsceneData cutscene;
        private int actionIndex = 0;
        private int actionTick = 0;
        private boolean waiting = false;
        private boolean frozen = false;
        private boolean stopped = false;

        CutsceneInstance(ServerPlayer player, CutsceneData cutscene) {
            this.player = player;
            this.cutscene = cutscene;
        }

        void stop() {
            if (stopped) return;
            stopped = true;
            if (frozen) {
                IScriptNetwork.sendToPlayer(new UnfreezePlayerPacket(), player);
            }
            IScriptNetwork.sendToPlayer(new CameraResetPacket(), player);
        }

        void restart() {
            actionIndex = 0;
            actionTick = 0;
            waiting = false;
            stopped = false;
        }

        boolean tick() {
            if (stopped) return false;
            if (actionIndex >= cutscene.getActions().size()) {
                if (cutscene.isLoop()) {
                    restart();
                    return true;
                }
                stop();
                return false;
            }

            CutsceneAction action = cutscene.getActions().get(actionIndex);

            if (!waiting) {
                executeAction(action);
                if (action.getType() == CutsceneActionType.DELAY ||
                        action.getType() == CutsceneActionType.CAMERA_PATH ||
                        action.getType() == CutsceneActionType.CAMERA_IDLE ||
                        action.getType() == CutsceneActionType.CAMERA_ORBIT ||
                        action.getType() == CutsceneActionType.CAMERA_DOLLY ||
                        action.getType() == CutsceneActionType.CAMERA_FOLLOW ||
                        action.getType() == CutsceneActionType.NPC_MOVE) {
                    waiting = true;
                    actionTick = action.getDuration();
                } else {
                    actionIndex++;
                }
            } else {
                actionTick--;
                if (actionTick <= 0) {
                    waiting = false;
                    actionIndex++;
                }
            }
            return true;
        }

        private void executeAction(CutsceneAction action) {
            switch (action.getType()) {
                case CAMERA_IDLE -> sendCameraIdle(action);
                case CAMERA_PATH -> sendCameraPath(action);
                case CAMERA_LOOK -> updateCameraLook(action);
                case CAMERA_FOLLOW -> sendCameraFollow(action);
                case CAMERA_ORBIT -> sendCameraOrbit(action);
                case CAMERA_DOLLY -> sendCameraDolly(action);
                case CAMERA_SHAKE -> updateCameraShake(action);
                case FREEZE_PLAYER -> {
                    frozen = true;
                    IScriptNetwork.sendToPlayer(new FreezePlayerPacket(), player);
                }
                case UNFREEZE_PLAYER -> {
                    frozen = false;
                    IScriptNetwork.sendToPlayer(new UnfreezePlayerPacket(), player);
                }
                case DIALOG -> {
                    var dialog = com.iscript.iscript.data.DialogManager.get((ServerLevel) player.level(), action.getStringValue());
                    if (dialog != null) {
                        var filtered = new com.iscript.iscript.data.dialog.DialogData();
                        filtered.setId(dialog.getId());
                        filtered.setTitle(dialog.getTitle());
                        filtered.setText(dialog.getText());
                        filtered.setPortrait(dialog.getPortrait());
                        IScriptNetwork.sendToPlayer(new OpenDialogScreenPacket(filtered), player);
                    }
                }
                case SOUND -> {
                    try {
                        ResourceLocation id = new ResourceLocation(action.getStringValue());
                        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
                        if (sound != null) {
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    sound, SoundSource.PLAYERS, 1.0f, 1.0f);
                        }
                    } catch (Exception ignored) {}
                }
                case BLOCK -> {
                    try {
                        ResourceLocation id = new ResourceLocation(action.getStringValue());
                        Block block = ForgeRegistries.BLOCKS.getValue(id);
                        if (block != null) {
                            player.level().setBlockAndUpdate(
                                    new BlockPos((int) action.getX(), (int) action.getY(), (int) action.getZ()),
                                    block.defaultBlockState());
                        }
                    } catch (Exception ignored) {}
                }
                case NPC_MOVE -> {
                    Entity entity = player.level().getEntity(action.getIntValue());
                    if (entity instanceof com.iscript.iscript.entity.IScriptNPCEntity npc) {
                        npc.teleportTo(action.getX(), action.getY(), action.getZ());
                    }
                }
                case NPC_ANIMATION -> {
                    Entity entity = player.level().getEntity(action.getIntValue());
                    if (entity instanceof com.iscript.iscript.entity.IScriptNPCEntity npc) {
                        IScriptNetwork.sendToAll(new NPCAnimationPacket(npc.getId(), action.getStringValue()));
                    }
                }
                case SCRIPT -> {
                    var engine = com.iscript.iscript.script.ScriptEngine.getInstance();
                    if (engine.isAvailable()) {
                        try {
                            engine.execute(action.getStringValue(), player, (ServerLevel) player.level());
                        } catch (Exception e) {
                            IScriptMod.LOGGER.error("Cutscene script error: {}", e.getMessage());
                        }
                    }
                }
                default -> {}
            }
        }

        private void sendCameraIdle(CutsceneAction action) {
            CutscenePath path = new CutscenePath();
            path.durationTicks = action.getDuration();

            PathType segType;
            try {
                segType = PathType.valueOf(action.getPathType());
            } catch (Exception e) {
                segType = PathType.LINEAR;
            }

            path.keyframes.add(new CutscenePath.Keyframe(
                    new Vec3(action.getX(), action.getY(), action.getZ()),
                    action.getYaw(), action.getPitch(), action.getRoll(),
                    action.isUseFov() ? action.getFov() : 70f, 0, segType));
            path.keyframes.add(new CutscenePath.Keyframe(
                    new Vec3(action.getX(), action.getY(), action.getZ()),
                    action.getYaw(), action.getPitch(), action.getRoll(),
                    action.isUseFov() ? action.getFov() : 70f, action.getDuration(), segType));

            CutsceneCameraMode mode = buildMode(action);
            IScriptNetwork.sendToPlayer(new SplineCameraMovePacket(path, mode, frozen), player);
        }

        private void sendCameraPath(CutsceneAction action) {
            CutscenePath path = new CutscenePath();
            try {
                path.easing = Interpolation.valueOf(action.getInterpolation());
            } catch (Exception ignored) {}

            List<Vec3> points = action.getSplinePoints();
            List<Float> yaws = action.getSplineYaws();
            List<Float> pitches = action.getSplinePitches();

            if (points == null || points.isEmpty()) {
                points = new ArrayList<>();
                points.add(new Vec3(action.getX(), action.getY(), action.getZ()));
                yaws = new ArrayList<>();
                yaws.add(action.getYaw());
                pitches = new ArrayList<>();
                pitches.add(action.getPitch());
            }

            int totalDur = action.getDuration();
            for (int i = 0; i < points.size(); i++) {
                float tick = (i / (float)(Math.max(1, points.size() - 1))) * totalDur;
                float yaw = i < yaws.size() ? yaws.get(i) : action.getYaw();
                float pitch = i < pitches.size() ? pitches.get(i) : action.getPitch();
                PathType segType;
                try {
                    segType = PathType.valueOf(action.getPathType());
                } catch (Exception e) {
                    segType = PathType.CATMULL_ROM;
                }
                path.keyframes.add(new CutscenePath.Keyframe(
                        points.get(i), yaw, pitch, action.getRoll(),
                        action.isUseFov() ? action.getFov() : 70f, tick, segType));
            }
            path.durationTicks = totalDur;

            if (action.isConstantSpeed()) {
                path.recalculateForConstantSpeed(action.getSpeed());
            }

            CutsceneCameraMode mode = buildMode(action);
            IScriptNetwork.sendToPlayer(new SplineCameraMovePacket(path, mode, frozen), player);
        }

        private void updateCameraLook(CutsceneAction action) {
            CutsceneCameraMode mode = new CutsceneCameraMode();
            mode.useLookAt = true;
            mode.lookAtTarget = new Vec3(action.getLookAtX(), action.getLookAtY(), action.getLookAtZ());
            IScriptNetwork.sendToPlayer(new SplineCameraMovePacket(null, mode, frozen), player);
        }

        private void sendCameraFollow(CutsceneAction action) {
            Entity entity = player.level().getEntity(action.getIntValue());
            if (entity == null) return;
            Vec3 pos = entity.position();
            CutscenePath path = new CutscenePath();
            path.durationTicks = action.getDuration();
            PathType segType;
            try { segType = PathType.valueOf(action.getPathType()); } catch (Exception e) { segType = PathType.LINEAR; }
            path.keyframes.add(new CutscenePath.Keyframe(pos, action.getYaw(), action.getPitch(), action.getRoll(),
                    action.isUseFov() ? action.getFov() : 70f, 0, segType));
            path.keyframes.add(new CutscenePath.Keyframe(pos, action.getYaw(), action.getPitch(), action.getRoll(),
                    action.isUseFov() ? action.getFov() : 70f, action.getDuration(), segType));
            CutsceneCameraMode mode = buildMode(action);
            IScriptNetwork.sendToPlayer(new SplineCameraMovePacket(path, mode, frozen), player);
        }

        private void sendCameraOrbit(CutsceneAction action) {
            CutscenePath path = new CutscenePath();
            path.durationTicks = action.getDuration();
            PathType segType;
            try { segType = PathType.valueOf(action.getPathType()); } catch (Exception e) { segType = PathType.LINEAR; }
            path.keyframes.add(new CutscenePath.Keyframe(
                    new Vec3(action.getX(), action.getY(), action.getZ()),
                    action.getYaw(), action.getPitch(), action.getRoll(),
                    action.isUseFov() ? action.getFov() : 70f, 0, segType));

            CutsceneCameraMode mode = new CutsceneCameraMode();
            mode.orbitMode = true;
            mode.orbitCenter = new Vec3(action.getX(), action.getY(), action.getZ());
            mode.orbitRadius = action.getOrbitRadius();
            mode.orbitHeight = action.getOrbitHeight();
            mode.orbitSpeed = action.getOrbitSpeed();

            IScriptNetwork.sendToPlayer(new SplineCameraMovePacket(path, mode, frozen), player);
        }

        private void sendCameraDolly(CutsceneAction action) {
            CutscenePath path = new CutscenePath();
            path.durationTicks = action.getDuration();
            PathType segType;
            try { segType = PathType.valueOf(action.getPathType()); } catch (Exception e) { segType = PathType.LINEAR; }
            path.keyframes.add(new CutscenePath.Keyframe(
                    new Vec3(action.getX(), action.getY(), action.getZ()),
                    action.getYaw(), action.getPitch(), action.getRoll(),
                    action.isUseFov() ? action.getFov() : 70f, 0, segType));

            CutsceneCameraMode mode = new CutsceneCameraMode();
            mode.useDollyZoom = true;
            mode.dollyTarget = new Vec3(action.getLookAtX(), action.getLookAtY(), action.getLookAtZ());
            mode.dollyBaseFov = action.getDollyBaseFov();
            mode.dollyBaseDistance = action.getDollyTargetDistance();

            IScriptNetwork.sendToPlayer(new SplineCameraMovePacket(path, mode, frozen), player);
        }

        private void updateCameraShake(CutsceneAction action) {
            CutsceneCameraMode mode = new CutsceneCameraMode();
            mode.shake = new CameraShake();
            mode.shake.setTrauma(action.getShakeTrauma());
            mode.shake.setDecay(action.getShakeDecay());
            mode.shake.setMaxAngle(action.getShakeMaxAngle());
            mode.shake.setMaxOffset(action.getShakeMaxOffset());
            IScriptNetwork.sendToPlayer(new SplineCameraMovePacket(null, mode, frozen), player);
        }

        private CutsceneCameraMode buildMode(CutsceneAction action) {
            CutsceneCameraMode mode = new CutsceneCameraMode();
            if (action.isUseFov()) {
                mode.dollyBaseFov = action.getFov();
            }
            return mode;
        }
    }
}