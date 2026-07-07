package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.CameraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CameraMovePacket {
    private final double x, y, z;
    private final float yaw, pitch;
    private final int duration;

    public CameraMovePacket(double x, double y, double z, float yaw, float pitch, int duration) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.duration = duration;
    }

    public CameraMovePacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.yaw = buf.readFloat();
        this.pitch = buf.readFloat();
        this.duration = buf.readInt();
    }

    public static CameraMovePacket decode(FriendlyByteBuf buf) {
        return new CameraMovePacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeInt(duration);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CameraHandler.startCamera(x, y, z, yaw, pitch, duration));
        ctx.get().setPacketHandled(true);
    }
}