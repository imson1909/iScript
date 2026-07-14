package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.camera.*;
import com.iscript.iscript.data.cutscene.CutsceneActionType;
import com.iscript.iscript.data.cutscene.CutsceneData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StartClientCutscenePacket {
    private final CutsceneData data;
    private final float speed;

    public StartClientCutscenePacket(CutsceneData data, float speed) {
        this.data = data;
        this.speed = speed;
    }

    public static void encode(StartClientCutscenePacket pkt, FriendlyByteBuf buf) {
        CompoundTag tag = new CompoundTag();
        pkt.data.save(tag);
        buf.writeNbt(tag);
        buf.writeFloat(pkt.speed);
    }

    public static StartClientCutscenePacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        CutsceneData data = new CutsceneData();
        if (tag != null) data.load(tag);
        return new StartClientCutscenePacket(data, buf.readFloat());
    }

    public static void handle(StartClientCutscenePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CutscenePath path = buildPathFromData(pkt.data);
            CutsceneCameraMode mode = buildModeFromData(pkt.data);
            if (path != null && path.keyframes.size() >= 2) {
                if (mode.useDollyZoom && mode.dollyTarget != null && !path.keyframes.isEmpty()) {
                    mode.dollyBaseDistance = path.keyframes.get(0).point.distanceTo(mode.dollyTarget);
                }
                CutsceneCameraHandler.start(path, mode, true, pkt.speed);
            }
        });
        ctx.get().setPacketHandled(true);
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
                try {
                    path.easing = Interpolation.valueOf(action.getInterpolation());
                } catch (Exception e) {}

                PathType segmentType;
                try {
                    segmentType = PathType.valueOf(action.getPathType());
                } catch (Exception e) {
                    segmentType = PathType.CATMULL_ROM;
                }

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
}