package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.SplineCameraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SplineCameraMovePacket {
    private final List<Vec3> points;
    private final List<Float> yaws;
    private final List<Float> pitches;
    private final int duration;
    private final double tension;
    private final boolean autoLook;
    private final float fov;
    private final boolean useFov;

    public SplineCameraMovePacket(List<Vec3> points, List<Float> yaws, List<Float> pitches, int duration, double tension, boolean autoLook) {
        this.points = points;
        this.yaws = yaws;
        this.pitches = pitches;
        this.duration = duration;
        this.tension = tension;
        this.autoLook = autoLook;
        this.fov = 70.0f;
        this.useFov = false;
    }

    public SplineCameraMovePacket(List<Vec3> points, List<Float> yaws, List<Float> pitches, int duration, double tension, boolean autoLook, float fov) {
        this.points = points;
        this.yaws = yaws;
        this.pitches = pitches;
        this.duration = duration;
        this.tension = tension;
        this.autoLook = autoLook;
        this.fov = fov;
        this.useFov = true;
    }

    public SplineCameraMovePacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.points = new ArrayList<>();
        this.yaws = new ArrayList<>();
        this.pitches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
            yaws.add(buf.readFloat());
            pitches.add(buf.readFloat());
        }
        this.duration = buf.readInt();
        this.tension = buf.readDouble();
        this.autoLook = buf.readBoolean();
        this.useFov = buf.readBoolean();
        this.fov = buf.readFloat();
    }

    public static SplineCameraMovePacket decode(FriendlyByteBuf buf) {
        return new SplineCameraMovePacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(points.size());
        for (int i = 0; i < points.size(); i++) {
            buf.writeDouble(points.get(i).x);
            buf.writeDouble(points.get(i).y);
            buf.writeDouble(points.get(i).z);
            buf.writeFloat(yaws.get(i));
            buf.writeFloat(pitches.get(i));
        }
        buf.writeInt(duration);
        buf.writeDouble(tension);
        buf.writeBoolean(autoLook);
        buf.writeBoolean(useFov);
        buf.writeFloat(fov);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            SplineCameraHandler.startSpline(points, yaws, pitches, duration, tension, autoLook);
            if (useFov) {
                SplineCameraHandler.setFov(fov);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
