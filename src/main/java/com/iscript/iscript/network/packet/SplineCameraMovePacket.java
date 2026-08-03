package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.camera.CameraShake;
import com.iscript.iscript.client.camera.CutsceneCameraHandler;
import com.iscript.iscript.client.camera.CutsceneCameraMode;
import com.iscript.iscript.client.camera.CutscenePath;
import com.iscript.iscript.client.camera.Interpolation;
import com.iscript.iscript.client.camera.PathType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SplineCameraMovePacket {
    private CutscenePath path;
    private CutsceneCameraMode mode;
    private boolean freeze;
    private float speed;

    public SplineCameraMovePacket() {}

    public SplineCameraMovePacket(CutscenePath path, CutsceneCameraMode mode, boolean freeze) {
        this(path, mode, freeze, 1.0f);
    }

    public SplineCameraMovePacket(CutscenePath path, CutsceneCameraMode mode, boolean freeze, float speed) {
        this.path = path;
        this.mode = mode;
        this.freeze = freeze;
        this.speed = speed;
    }

    public static void encode(SplineCameraMovePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.freeze);
        buf.writeFloat(packet.speed);

        buf.writeBoolean(packet.mode != null);
        if (packet.mode != null) {
            buf.writeBoolean(packet.mode.useLookAt);
            if (packet.mode.useLookAt) {
                buf.writeDouble(packet.mode.lookAtTarget.x);
                buf.writeDouble(packet.mode.lookAtTarget.y);
                buf.writeDouble(packet.mode.lookAtTarget.z);
            }
            buf.writeBoolean(packet.mode.orbitMode);
            if (packet.mode.orbitMode) {
                buf.writeDouble(packet.mode.orbitCenter.x);
                buf.writeDouble(packet.mode.orbitCenter.y);
                buf.writeDouble(packet.mode.orbitCenter.z);
                buf.writeFloat(packet.mode.orbitRadius);
                buf.writeFloat(packet.mode.orbitHeight);
                buf.writeFloat(packet.mode.orbitSpeed);
            }
            buf.writeBoolean(packet.mode.useDollyZoom);
            if (packet.mode.useDollyZoom) {
                buf.writeDouble(packet.mode.dollyTarget.x);
                buf.writeDouble(packet.mode.dollyTarget.y);
                buf.writeDouble(packet.mode.dollyTarget.z);
                buf.writeFloat(packet.mode.dollyBaseFov);
                buf.writeDouble(packet.mode.dollyBaseDistance);
            }
            buf.writeBoolean(packet.mode.shake != null);
            if (packet.mode.shake != null) {
                buf.writeFloat(packet.mode.shake.getTrauma());
                buf.writeFloat(packet.mode.shake.getDecay());
                buf.writeFloat(packet.mode.shake.getMaxAngle());
                buf.writeFloat(packet.mode.shake.getMaxOffset());
            }
        }

        buf.writeBoolean(packet.path != null);
        if (packet.path != null) {
            buf.writeFloat(packet.path.durationTicks);
            buf.writeEnum(packet.path.easing);
            buf.writeBoolean(packet.path.constantSpeed);

            buf.writeInt(packet.path.keyframes.size());
            for (var kf : packet.path.keyframes) {
                buf.writeDouble(kf.point.x);
                buf.writeDouble(kf.point.y);
                buf.writeDouble(kf.point.z);
                buf.writeFloat(kf.yaw);
                buf.writeFloat(kf.pitch);
                buf.writeFloat(kf.roll);
                buf.writeFloat(kf.fov);
                buf.writeFloat(kf.tick);
                buf.writeEnum(kf.segmentType);
            }
        }
    }

    public static SplineCameraMovePacket decode(FriendlyByteBuf buf) {
        boolean freeze = buf.readBoolean();
        float speed = buf.readFloat();

        CutsceneCameraMode mode = null;
        if (buf.readBoolean()) {
            mode = new CutsceneCameraMode();
            if (buf.readBoolean()) {
                mode.useLookAt = true;
                mode.lookAtTarget = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            }
            if (buf.readBoolean()) {
                mode.orbitMode = true;
                mode.orbitCenter = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                mode.orbitRadius = buf.readFloat();
                mode.orbitHeight = buf.readFloat();
                mode.orbitSpeed = buf.readFloat();
            }
            if (buf.readBoolean()) {
                mode.useDollyZoom = true;
                mode.dollyTarget = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                mode.dollyBaseFov = buf.readFloat();
                mode.dollyBaseDistance = buf.readDouble();
            }
            if (buf.readBoolean()) {
                CameraShake shake = new CameraShake();
                shake.setTrauma(buf.readFloat());
                shake.setDecay(buf.readFloat());
                shake.setMaxAngle(buf.readFloat());
                shake.setMaxOffset(buf.readFloat());
                mode.shake = shake;
            }
        }

        CutscenePath path = null;
        if (buf.readBoolean()) {
            path = new CutscenePath();
            path.durationTicks = buf.readFloat();
            path.easing = buf.readEnum(Interpolation.class);
            path.constantSpeed = buf.readBoolean();

            int count = buf.readInt();
            for (int i = 0; i < count; i++) {
                Vec3 point = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                float yaw = buf.readFloat();
                float pitch = buf.readFloat();
                float roll = buf.readFloat();
                float fov = buf.readFloat();
                float tick = buf.readFloat();
                PathType segType = buf.readEnum(PathType.class);
                path.keyframes.add(new CutscenePath.Keyframe(point, yaw, pitch, roll, fov, tick, segType));
            }
        }

        return new SplineCameraMovePacket(path, mode, freeze, speed);
    }

    public static void handle(SplineCameraMovePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (packet.path != null) {
                CutsceneCameraHandler.start(packet.path, packet.mode, packet.freeze, packet.speed);
            } else if (packet.mode != null) {
                CutsceneCameraHandler handler = null;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}