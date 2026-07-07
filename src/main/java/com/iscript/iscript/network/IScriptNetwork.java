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

        INSTANCE.messageBuilder(CameraMovePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CameraMovePacket::encode)
                .decoder(CameraMovePacket::decode)
                .consumerMainThread(CameraMovePacket::handle)
                .add();

        INSTANCE.messageBuilder(CameraResetPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CameraResetPacket::encode)
                .decoder(CameraResetPacket::decode)
                .consumerMainThread(CameraResetPacket::handle)
                .add();

        INSTANCE.messageBuilder(RequestDialogPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestDialogPacket::encode)
                .decoder(RequestDialogPacket::decode)
                .consumerMainThread(RequestDialogPacket::handle)
                .add();

        INSTANCE.messageBuilder(ScriptBlockSavePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ScriptBlockSavePacket::encode)
                .decoder(ScriptBlockSavePacket::decode)
                .consumerMainThread(ScriptBlockSavePacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenScriptBlockScreenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenScriptBlockScreenPacket::encode)
                .decoder(OpenScriptBlockScreenPacket::decode)
                .consumerMainThread(OpenScriptBlockScreenPacket::handle)
                .add();

        INSTANCE.messageBuilder(SaveDialogGraphPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveDialogGraphPacket::encode)
                .decoder(SaveDialogGraphPacket::decode)
                .consumerMainThread(SaveDialogGraphPacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenDialogGraphEditorPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenDialogGraphEditorPacket::encode)
                .decoder(OpenDialogGraphEditorPacket::decode)
                .consumerMainThread(OpenDialogGraphEditorPacket::handle)
                .add();

        INSTANCE.messageBuilder(FreezePlayerPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FreezePlayerPacket::encode)
                .decoder(FreezePlayerPacket::decode)
                .consumerMainThread(FreezePlayerPacket::handle)
                .add();

        INSTANCE.messageBuilder(UnfreezePlayerPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnfreezePlayerPacket::encode)
                .decoder(UnfreezePlayerPacket::decode)
                .consumerMainThread(UnfreezePlayerPacket::handle)
                .add();

        INSTANCE.messageBuilder(NPCAnimationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NPCAnimationPacket::encode)
                .decoder(NPCAnimationPacket::decode)
                .consumerMainThread(NPCAnimationPacket::handle)
                .add();

        INSTANCE.messageBuilder(SplineCameraMovePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SplineCameraMovePacket::encode)
                .decoder(SplineCameraMovePacket::decode)
                .consumerMainThread(SplineCameraMovePacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenNPCTradePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenNPCTradePacket::encode)
                .decoder(OpenNPCTradePacket::decode)
                .consumerMainThread(OpenNPCTradePacket::handle)
                .add();

        INSTANCE.messageBuilder(NPCTradeExecutePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NPCTradeExecutePacket::encode)
                .decoder(NPCTradeExecutePacket::decode)
                .consumerMainThread(NPCTradeExecutePacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenScriptGraphEditorPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenScriptGraphEditorPacket::encode)
                .decoder(OpenScriptGraphEditorPacket::decode)
                .consumerMainThread(OpenScriptGraphEditorPacket::handle)
                .add();

        INSTANCE.messageBuilder(SaveScriptGraphPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveScriptGraphPacket::encode)
                .decoder(SaveScriptGraphPacket::decode)
                .consumerMainThread(SaveScriptGraphPacket::handle)
                .add();

        INSTANCE.messageBuilder(RequestScriptGraphPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestScriptGraphPacket::encode)
                .decoder(RequestScriptGraphPacket::decode)
                .consumerMainThread(RequestScriptGraphPacket::handle)
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
