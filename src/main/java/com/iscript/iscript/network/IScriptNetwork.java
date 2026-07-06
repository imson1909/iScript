package com.iscript.iscript.network;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

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
        INSTANCE.messageBuilder(SyncNPCDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncNPCDataPacket::encode)
                .decoder(SyncNPCDataPacket::decode)
                .consumerMainThread(SyncNPCDataPacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenNPCEditGuiPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenNPCEditGuiPacket::encode)
                .decoder(OpenNPCEditGuiPacket::decode)
                .consumerMainThread(OpenNPCEditGuiPacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenDialogScreenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenDialogScreenPacket::encode)
                .decoder(OpenDialogScreenPacket::decode)
                .consumerMainThread(OpenDialogScreenPacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenQuestJournalPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenQuestJournalPacket::encode)
                .decoder(OpenQuestJournalPacket::decode)
                .consumerMainThread(OpenQuestJournalPacket::handle)
                .add();

        INSTANCE.messageBuilder(SaveNPCDataPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveNPCDataPacket::encode)
                .decoder(SaveNPCDataPacket::decode)
                .consumerMainThread(SaveNPCDataPacket::handle)
                .add();
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAll(Object msg) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static void sendToServer(Object msg) {
        INSTANCE.sendToServer(msg);
    }
}
