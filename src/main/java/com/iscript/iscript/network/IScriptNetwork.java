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

        INSTANCE.messageBuilder(SaveScriptGraphPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveScriptGraphPacket::encode)
                .decoder(SaveScriptGraphPacket::decode)
                .consumerMainThread(SaveScriptGraphPacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenDashboardPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenDashboardPacket::encode)
                .decoder(OpenDashboardPacket::decode)
                .consumerMainThread(OpenDashboardPacket::handle)
                .add();

        INSTANCE.messageBuilder(SaveScriptTextPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveScriptTextPacket::encode)
                .decoder(SaveScriptTextPacket::decode)
                .consumerMainThread(SaveScriptTextPacket::handle)
                .add();

        INSTANCE.messageBuilder(RequestScriptGraphsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestScriptGraphsPacket::encode)
                .decoder(RequestScriptGraphsPacket::decode)
                .consumerMainThread(RequestScriptGraphsPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncScriptGraphsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncScriptGraphsPacket::encode)
                .decoder(SyncScriptGraphsPacket::decode)
                .consumerMainThread(SyncScriptGraphsPacket::handle)
                .add();

        INSTANCE.messageBuilder(RequestScriptContentPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestScriptContentPacket::encode)
                .decoder(RequestScriptContentPacket::decode)
                .consumerMainThread(RequestScriptContentPacket::handle)
                .add();

        INSTANCE.messageBuilder(ScriptGraphContentPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ScriptGraphContentPacket::encode)
                .decoder(ScriptGraphContentPacket::decode)
                .consumerMainThread(ScriptGraphContentPacket::handle)
                .add();

        INSTANCE.messageBuilder(RunScriptPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RunScriptPacket::encode)
                .decoder(RunScriptPacket::decode)
                .consumerMainThread(RunScriptPacket::handle)
                .add();

        INSTANCE.messageBuilder(GiveQuestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GiveQuestPacket::encode)
                .decoder(GiveQuestPacket::decode)
                .consumerMainThread(GiveQuestPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncQuestProgressPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncQuestProgressPacket::encode)
                .decoder(SyncQuestProgressPacket::decode)
                .consumerMainThread(SyncQuestProgressPacket::handle)
                .add();

        INSTANCE.messageBuilder(QuestObjectiveUpdatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(QuestObjectiveUpdatePacket::encode)
                .decoder(QuestObjectiveUpdatePacket::decode)
                .consumerMainThread(QuestObjectiveUpdatePacket::handle)
                .add();

        INSTANCE.messageBuilder(RequestEventGraphsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestEventGraphsPacket::encode)
                .decoder(RequestEventGraphsPacket::decode)
                .consumerMainThread(RequestEventGraphsPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncEventGraphsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncEventGraphsPacket::encode)
                .decoder(SyncEventGraphsPacket::decode)
                .consumerMainThread(SyncEventGraphsPacket::handle)
                .add();

        INSTANCE.messageBuilder(SaveEventGraphPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveEventGraphPacket::encode)
                .decoder(SaveEventGraphPacket::decode)
                .consumerMainThread(SaveEventGraphPacket::handle)
                .add();

        INSTANCE.messageBuilder(RunEventGraphPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RunEventGraphPacket::encode)
                .decoder(RunEventGraphPacket::decode)
                .consumerMainThread(RunEventGraphPacket::handle)
                .add();

        INSTANCE.registerMessage(id++, OpenRegionScreenPacket.class, OpenRegionScreenPacket::encode, OpenRegionScreenPacket::decode, OpenRegionScreenPacket::handle);
        INSTANCE.registerMessage(id++, UpdateRegionBlockPacket.class, UpdateRegionBlockPacket::encode, UpdateRegionBlockPacket::decode, UpdateRegionBlockPacket::handle);

        INSTANCE.messageBuilder(RequestNPCListPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestNPCListPacket::encode)
                .decoder(RequestNPCListPacket::decode)
                .consumerMainThread(RequestNPCListPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncNPCListPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncNPCListPacket::encode)
                .decoder(SyncNPCListPacket::decode)
                .consumerMainThread(SyncNPCListPacket::handle)
                .add();

        INSTANCE.messageBuilder(DeleteNPCPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteNPCPacket::encode)
                .decoder(DeleteNPCPacket::decode)
                .consumerMainThread(DeleteNPCPacket::handle)
                .add();

        INSTANCE.messageBuilder(SpawnNPCPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SpawnNPCPacket::encode)
                .decoder(SpawnNPCPacket::decode)
                .consumerMainThread(SpawnNPCPacket::handle)
                .add();

        INSTANCE.messageBuilder(SaveCutscenePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveCutscenePacket::encode)
                .decoder(SaveCutscenePacket::decode)
                .consumerMainThread(SaveCutscenePacket::handle)
                .add();

        INSTANCE.messageBuilder(DeleteCutscenePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteCutscenePacket::encode)
                .decoder(DeleteCutscenePacket::decode)
                .consumerMainThread(DeleteCutscenePacket::handle)
                .add();

        INSTANCE.messageBuilder(PlayCutscenePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PlayCutscenePacket::encode)
                .decoder(PlayCutscenePacket::decode)
                .consumerMainThread(PlayCutscenePacket::handle)
                .add();

        INSTANCE.messageBuilder(CutsceneFinishedPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CutsceneFinishedPacket::encode)
                .decoder(CutsceneFinishedPacket::decode)
                .consumerMainThread(CutsceneFinishedPacket::handle)
                .add();

        INSTANCE.messageBuilder(StartClientCutscenePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StartClientCutscenePacket::encode)
                .decoder(StartClientCutscenePacket::decode)
                .consumerMainThread(StartClientCutscenePacket::handle)
                .add();

        INSTANCE.messageBuilder(StopClientCutscenePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StopClientCutscenePacket::encode)
                .decoder(StopClientCutscenePacket::decode)
                .consumerMainThread(StopClientCutscenePacket::handle)
                .add();

        INSTANCE.messageBuilder(PauseClientCutscenePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PauseClientCutscenePacket::encode)
                .decoder(PauseClientCutscenePacket::decode)
                .consumerMainThread(PauseClientCutscenePacket::handle)
                .add();

        INSTANCE.messageBuilder(ResumeClientCutscenePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ResumeClientCutscenePacket::encode)
                .decoder(ResumeClientCutscenePacket::decode)
                .consumerMainThread(ResumeClientCutscenePacket::handle)
                .add();

        INSTANCE.messageBuilder(RequestCutscenesPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestCutscenesPacket::encode)
                .decoder(RequestCutscenesPacket::decode)
                .consumerMainThread(RequestCutscenesPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncCutscenesPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCutscenesPacket::encode)
                .decoder(SyncCutscenesPacket::decode)
                .consumerMainThread(SyncCutscenesPacket::handle)
                .add();

        INSTANCE.messageBuilder(StopCutscenePacket.class, id++)
                .encoder(StopCutscenePacket::encode)
                .decoder(StopCutscenePacket::decode)
                .consumerMainThread(StopCutscenePacket::handle)
                .add();

        INSTANCE.messageBuilder(PauseCutscenePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PauseCutscenePacket::encode)
                .decoder(PauseCutscenePacket::decode)
                .consumerMainThread(PauseCutscenePacket::handle)
                .add();

        INSTANCE.messageBuilder(ResumeCutscenePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ResumeCutscenePacket::encode)
                .decoder(ResumeCutscenePacket::decode)
                .consumerMainThread(ResumeCutscenePacket::handle)
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