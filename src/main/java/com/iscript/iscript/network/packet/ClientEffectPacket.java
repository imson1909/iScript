package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.CameraHandler;
import com.iscript.iscript.client.ClientQuestCache;
import com.iscript.iscript.client.camera.CameraShake;
import com.iscript.iscript.client.camera.CutsceneCameraHandler;
import com.iscript.iscript.client.camera.CutsceneCameraMode;
import com.iscript.iscript.client.camera.CutscenePath;
import com.iscript.iscript.client.camera.Interpolation;
import com.iscript.iscript.client.camera.PathType;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientEffectPacket {
    public enum Type {
        CAMERA_MOVE, CAMERA_RESET,
        FREEZE_PLAYER, UNFREEZE_PLAYER,
        START_CUTSCENE_CLIENT, STOP_CUTSCENE_CLIENT,
        PAUSE_CUTSCENE_CLIENT, RESUME_CUTSCENE_CLIENT,
        NPC_ANIMATION, QUEST_OBJ_UPDATE
    }

    private final Type type;
    private final CompoundTag data;

    public ClientEffectPacket(Type type, CompoundTag data) {
        this.type = type;
        this.data = data != null ? data : new CompoundTag();
    }

    public static void encode(ClientEffectPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.type);
        buf.writeNbt(packet.data);
    }

    public static ClientEffectPacket decode(FriendlyByteBuf buf) {
        return new ClientEffectPacket(buf.readEnum(Type.class), buf.readNbt());
    }

    public static void handle(ClientEffectPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            switch (packet.type) {
                case CAMERA_MOVE -> CameraHandler.startCamera(
                        packet.data.getDouble("X"), packet.data.getDouble("Y"), packet.data.getDouble("Z"),
                        packet.data.getFloat("Yaw"), packet.data.getFloat("Pitch"), packet.data.getInt("Duration")
                );
                case CAMERA_RESET -> CutsceneCameraHandler.stop();
                case FREEZE_PLAYER -> CameraHandler.setFrozen(true);
                case UNFREEZE_PLAYER -> CameraHandler.resetCamera();
                case START_CUTSCENE_CLIENT -> handleStartCutscene(packet.data);
                case STOP_CUTSCENE_CLIENT -> handleStopCutscene(packet.data);
                case PAUSE_CUTSCENE_CLIENT -> CutsceneCameraHandler.pause();
                case RESUME_CUTSCENE_CLIENT -> CutsceneCameraHandler.resume(
                        packet.data.getFloat("Speed"), packet.data.getInt("StartTick")
                );
                case NPC_ANIMATION -> handleNPCAnimation(packet.data);
                case QUEST_OBJ_UPDATE -> ClientQuestCache.updateObjective(
                        packet.data.getString("QuestId"),
                        packet.data.getInt("StageIndex"),
                        packet.data.getInt("ObjectiveIndex"),
                        packet.data.getInt("CurrentCount"),
                        packet.data.getInt("RequiredCount"),
                        packet.data.getBoolean("StageComplete"),
                        packet.data.getBoolean("QuestComplete")
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleStartCutscene(CompoundTag data) {
        CutsceneData cutscene = new CutsceneData();
        cutscene.load(data.getCompound("Cutscene"));
        float speed = data.getFloat("Speed");
        CutscenePath path = buildPathFromData(cutscene);
        CutsceneCameraMode mode = buildModeFromData(cutscene);
        if (path != null && path.keyframes.size() >= 2) {
            if (mode.useDollyZoom && mode.dollyTarget != null && !path.keyframes.isEmpty()) {
                mode.dollyBaseDistance = path.keyframes.get(0).point.distanceTo(mode.dollyTarget);
            }
            CutsceneCameraHandler.start(path, mode, true, speed);
        }
    }

    private static void handleStopCutscene(CompoundTag data) {
        boolean reset = data.getBoolean("ResetPosition");
        if (reset && CutsceneCameraHandler.isActive()) {
            Vec3 start = CutsceneCameraHandler.getStartPosition();
            float yaw = CutsceneCameraHandler.getStartYaw();
            float pitch = CutsceneCameraHandler.getStartPitch();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && start != null && !start.equals(Vec3.ZERO)) {
                mc.player.setPos(start.x, start.y, start.z);
                mc.player.setYRot(yaw);
                mc.player.setXRot(pitch);
            }
        }
        CutsceneCameraHandler.stop();
    }

    private static void handleNPCAnimation(CompoundTag data) {
        int entityId = data.getInt("EntityId");
        String animation = data.getString("Animation");
        if (Minecraft.getInstance().level != null) {
            var entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity instanceof IScriptNPCEntity npc) npc.playAnimation(animation);
        }
    }

    private static CutsceneCameraMode buildModeFromData(CutsceneData data) {
        CutsceneCameraMode mode = new CutsceneCameraMode();
        for (var action : data.getActions()) {
            if (!action.isCameraAction()) continue;
            switch (action.getType()) {
                case CAMERA_LOOK, CAMERA_FOLLOW -> {
                    mode.useLookAt = true;
                    mode.lookAtTarget = new Vec3(action.getLookAtX(), action.getLookAtY(), action.getLookAtZ());
                }
                case CAMERA_ORBIT -> {
                    mode.orbitMode = true;
                    mode.orbitRadius = action.getOrbitRadius();
                    mode.orbitHeight = action.getOrbitHeight();
                    mode.orbitSpeed = action.getOrbitSpeed();
                    mode.orbitCenter = new Vec3(action.getLookAtX(), action.getLookAtY(), action.getLookAtZ());
                }
                case CAMERA_DOLLY -> {
                    mode.useDollyZoom = true;
                    mode.dollyTarget = new Vec3(action.getLookAtX(), action.getLookAtY(), action.getLookAtZ());
                    mode.dollyBaseFov = action.getFov();
                }
                case CAMERA_SHAKE -> {
                    mode.shake = new CameraShake();
                    mode.shake.setTrauma(action.getShakeTrauma());
                    mode.shake.setDecay(action.getShakeDecay());
                    mode.shake.setMaxAngle(action.getShakeMaxAngle());
                    mode.shake.setMaxOffset(action.getShakeMaxOffset());
                }
            }
        }
        return mode;
    }

    private static CutscenePath buildPathFromData(CutsceneData data) {
        CutscenePath path = new CutscenePath();
        float currentTick = 0;
        for (var action : data.getActions()) {
            if (action.isCameraAction()) {
                try { path.easing = Interpolation.valueOf(action.getInterpolation()); } catch (Exception e) {}
                PathType segmentType;
                try { segmentType = PathType.valueOf(action.getPathType()); } catch (Exception e) { segmentType = PathType.CATMULL_ROM; }
                if (!action.getSplinePoints().isEmpty()) {
                    for (int i = 0; i < action.getSplinePoints().size(); i++) {
                        Vec3 p = action.getSplinePoints().get(i);
                        float yaw = i < action.getSplineYaws().size() ? action.getSplineYaws().get(i) : action.getYaw();
                        float pitch = i < action.getSplinePitches().size() ? action.getSplinePitches().get(i) : action.getPitch();
                        path.keyframes.add(new CutscenePath.Keyframe(p, yaw, pitch, action.getRoll(), action.getFov(), currentTick, segmentType));
                    }
                } else {
                    path.keyframes.add(new CutscenePath.Keyframe(
                            new Vec3(action.getX(), action.getY(), action.getZ()),
                            action.getYaw(), action.getPitch(), action.getRoll(), action.getFov(), currentTick, segmentType
                    ));
                }
            }
            currentTick += action.getDuration();
        }
        float totalDur = 0;
        for (var a : data.getActions()) totalDur += a.getDuration();
        path.durationTicks = Math.max(totalDur, 1);
        return path.keyframes.size() < 2 ? null : path;
    }

    public static CompoundTag cameraMoveToTag(double x, double y, double z, float yaw, float pitch, int duration) {
        CompoundTag data = new CompoundTag();
        data.putDouble("X", x);
        data.putDouble("Y", y);
        data.putDouble("Z", z);
        data.putFloat("Yaw", yaw);
        data.putFloat("Pitch", pitch);
        data.putInt("Duration", duration);
        return data;
    }

    public static CompoundTag startCutsceneToTag(CutsceneData cutscene, float speed) {
        CompoundTag data = new CompoundTag();
        CompoundTag t = new CompoundTag();
        cutscene.save(t);
        data.put("Cutscene", t);
        data.putFloat("Speed", speed);
        return data;
    }

    public static CompoundTag stopCutsceneToTag(boolean resetPosition) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("ResetPosition", resetPosition);
        return data;
    }

    public static CompoundTag resumeCutsceneToTag(float speed, int startTick) {
        CompoundTag data = new CompoundTag();
        data.putFloat("Speed", speed);
        data.putInt("StartTick", startTick);
        return data;
    }

    public static CompoundTag npcAnimToTag(int entityId, String animation) {
        CompoundTag data = new CompoundTag();
        data.putInt("EntityId", entityId);
        data.putString("Animation", animation);
        return data;
    }

    public static CompoundTag questObjToTag(String questId, int stageIndex, int objectiveIndex, int currentCount, int requiredCount, boolean stageComplete, boolean questComplete) {
        CompoundTag data = new CompoundTag();
        data.putString("QuestId", questId);
        data.putInt("StageIndex", stageIndex);
        data.putInt("ObjectiveIndex", objectiveIndex);
        data.putInt("CurrentCount", currentCount);
        data.putInt("RequiredCount", requiredCount);
        data.putBoolean("StageComplete", stageComplete);
        data.putBoolean("QuestComplete", questComplete);
        return data;
    }
}