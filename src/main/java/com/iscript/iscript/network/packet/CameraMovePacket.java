package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.CameraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CameraMovePacket {
    private double x, y, z;
    private float yaw, pitch;
    private int duration;

    public CameraMovePacket() {}

    public CameraMovePacket(double x, double y, double z, float yaw, float pitch, int duration) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.duration = duration;
    }

    public static void encode(CameraMovePacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeFloat(packet.yaw);
        buf.writeFloat(packet.pitch);
        buf.writeInt(packet.duration);
    }

    public static CameraMovePacket decode(FriendlyByteBuf buf) {
        return new CameraMovePacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readInt());
    }

    public static void handle(CameraMovePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CameraHandler.startCamera(packet.x, packet.y, packet.z, packet.yaw, packet.pitch, packet.duration);
        });
        ctx.get().setPacketHandled(true);
    }
}