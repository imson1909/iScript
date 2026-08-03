package com.iscript.iscript.network;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.morph.network.MorphSyncPacket;
import com.iscript.iscript.morph.network.MorphUpdatePacket;
import com.iscript.iscript.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.nbt.CompoundTag;

public class IScriptNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(IScriptMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        INSTANCE.messageBuilder(SyncDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDataPacket::encode)
                .decoder(SyncDataPacket::decode)
                .consumerMainThread(SyncDataPacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenGuiPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenGuiPacket::encode)
                .decoder(OpenGuiPacket::decode)
                .consumerMainThread(OpenGuiPacket::handle)
                .add();

        INSTANCE.messageBuilder(ClientEffectPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientEffectPacket::encode)
                .decoder(ClientEffectPacket::decode)
                .consumerMainThread(ClientEffectPacket::handle)
                .add();

        INSTANCE.messageBuilder(ServerCommandPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerCommandPacket::encode)
                .decoder(ServerCommandPacket::decode)
                .consumerMainThread(ServerCommandPacket::handle)
                .add();

        INSTANCE.messageBuilder(SplineCameraMovePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SplineCameraMovePacket::encode)
                .decoder(SplineCameraMovePacket::decode)
                .consumerMainThread(SplineCameraMovePacket::handle)
                .add();

        INSTANCE.messageBuilder(MorphSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MorphSyncPacket::encode)
                .decoder(MorphSyncPacket::decode)
                .consumerMainThread(MorphSyncPacket::handle)
                .add();

        INSTANCE.messageBuilder(MorphUpdatePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MorphUpdatePacket::encode)
                .decoder(MorphUpdatePacket::decode)
                .consumerMainThread(MorphUpdatePacket::handle)
                .add();
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        if (msg instanceof CompoundTag) {
            throw new IllegalArgumentException("Attempted to send raw CompoundTag to player! Wrap it in a packet.");
        }
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAll(Object msg) {
        if (msg instanceof CompoundTag) {
            throw new IllegalArgumentException("Attempted to send raw CompoundTag to all! Wrap it in a packet.");
        }
        INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static void sendToServer(Object msg) {
        if (msg instanceof CompoundTag) {
            throw new IllegalArgumentException("Attempted to send raw CompoundTag to server! Wrap it in a ServerCommandPacket.");
        }
        INSTANCE.sendToServer(msg);
    }

    public static void sendToTracking(Entity entity, Object msg) {
        if (msg instanceof CompoundTag) {
            throw new IllegalArgumentException("Attempted to send raw CompoundTag to tracking! Wrap it in a packet.");
        }
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }
}