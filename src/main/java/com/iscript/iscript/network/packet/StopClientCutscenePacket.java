package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.camera.CutsceneCameraHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

public class StopClientCutscenePacket {
    public final boolean resetPosition;

    public StopClientCutscenePacket() {
        this(false);
    }

    public StopClientCutscenePacket(boolean resetPosition) {
        this.resetPosition = resetPosition;
    }

    public static void encode(StopClientCutscenePacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.resetPosition);
    }

    public static StopClientCutscenePacket decode(FriendlyByteBuf buf) {
        return new StopClientCutscenePacket(buf.readBoolean());
    }

    public static void handle(StopClientCutscenePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (pkt.resetPosition && CutsceneCameraHandler.isActive()) {
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
        });
        ctx.get().setPacketHandled(true);
    }
}