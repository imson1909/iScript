package com.iscript.iscript.data.cutscene;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.client.SplineCameraHandler;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.CameraMovePacket;
import com.iscript.iscript.network.packet.CameraResetPacket;
import com.iscript.iscript.network.packet.FreezePlayerPacket;
import com.iscript.iscript.network.packet.NPCAnimationPacket;
import com.iscript.iscript.network.packet.OpenDialogScreenPacket;
import com.iscript.iscript.network.packet.SplineCameraMovePacket;
import com.iscript.iscript.network.packet.UnfreezePlayerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CutscenePlayer {
    private static final Map<ServerPlayer, CutsceneInstance> activeCutscenes = new HashMap<>();

    public static void play(ServerPlayer player, CutsceneData cutscene) {
        if (activeCutscenes.containsKey(player)) return;
        activeCutscenes.put(player, new CutsceneInstance(player, cutscene));
    }

    public static void stop(ServerPlayer player) {
        CutsceneInstance inst = activeCutscenes.remove(player);
        if (inst != null) {
            inst.stop();
        }
    }

    public static boolean isPlaying(ServerPlayer player) {
        return activeCutscenes.containsKey(player);
    }

    public static void tick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            CutsceneInstance inst = activeCutscenes.get(player);
            if (inst != null) {
                if (!inst.tick()) {
                    activeCutscenes.remove(player);
                }
            }
        }
    }

    private static class CutsceneInstance {
        private final ServerPlayer player;
        private final CutsceneData cutscene;
        private int actionIndex = 0;
        private int actionTick = 0;
        private boolean finished = false;
        private boolean waiting = false;
        private boolean frozen = false;
        private boolean splineActive = false;
        private List<CutsceneAction> splineBuffer = new ArrayList<>();

        CutsceneInstance(ServerPlayer player, CutsceneData cutscene) {
            this.player = player;
            this.cutscene = cutscene;
        }

        void stop() {
            if (frozen) {
                IScriptNetwork.sendToPlayer(new UnfreezePlayerPacket(), player);
            }
            IScriptNetwork.sendToPlayer(new CameraResetPacket(), player);
            finished = true;
        }

        boolean tick() {
            if (finished) return false;
            if (actionIndex >= cutscene.getActions().size()) {
                if (frozen) {
                    IScriptNetwork.sendToPlayer(new UnfreezePlayerPacket(), player);
                }
                IScriptNetwork.sendToPlayer(new CameraResetPacket(), player);
                return false;
            }

            CutsceneAction action = cutscene.getActions().get(actionIndex);

            if (!waiting) {
                if (action.getType() == CutsceneActionType.CAMERA_MOVE && action.isUseSpline()) {
                    collectSplinePoints();
                    executeSplineCamera();
                    waiting = true;
                    actionTick = calculateSplineDuration();
                } else {
                    executeAction(action);
                    if (action.getType() == CutsceneActionType.DELAY ||
                            action.getType() == CutsceneActionType.CAMERA_MOVE ||
                            action.getType() == CutsceneActionType.NPC_MOVE) {
                        waiting = true;
                        actionTick = action.getDuration();
                    } else {
                        actionIndex++;
                    }
                }
            } else {
                actionTick--;
                if (actionTick <= 0) {
                    waiting = false;
                    if (splineActive) {
                        splineActive = false;
                        splineBuffer.clear();
                    }
                    actionIndex++;
                }
            }

            return true;
        }

        private void collectSplinePoints() {
            splineBuffer.clear();
            for (int i = actionIndex; i < cutscene.getActions().size(); i++) {
                CutsceneAction a = cutscene.getActions().get(i);
                if (a.getType() == CutsceneActionType.CAMERA_MOVE && a.isUseSpline()) {
                    splineBuffer.add(a);
                } else {
                    break;
                }
            }
        }

        private int calculateSplineDuration() {
            int total = 0;
            for (CutsceneAction a : splineBuffer) {
                total += a.getDuration();
            }
            return total;
        }

        private void executeSplineCamera() {
            if (splineBuffer.isEmpty()) return;
            List<net.minecraft.world.phys.Vec3> points = new ArrayList<>();
            List<Float> yaws = new ArrayList<>();
            List<Float> pitches = new ArrayList<>();
            for (CutsceneAction a : splineBuffer) {
                points.add(new net.minecraft.world.phys.Vec3(a.getX(), a.getY(), a.getZ()));
                yaws.add(a.getYaw());
                pitches.add(a.getPitch());
            }
            CutsceneAction first = splineBuffer.get(0);
            SplineCameraMovePacket packet = new SplineCameraMovePacket(
                    points, yaws, pitches, calculateSplineDuration(),
                    first.getSplineTension(), first.isAutoLook(), first.getFov()
            );
            IScriptNetwork.sendToPlayer(packet, player);
            splineActive = true;
        }

        private void executeAction(CutsceneAction action) {
            switch (action.getType()) {
                case CAMERA_MOVE -> {
                    if (!action.isUseSpline()) {
                        IScriptNetwork.sendToPlayer(
                                new CameraMovePacket(action.getX(), action.getY(), action.getZ(),
                                        action.getYaw(), action.getPitch(), action.getDuration()), player);
                    }
                }
                case FREEZE_PLAYER -> {
                    frozen = true;
                    IScriptNetwork.sendToPlayer(new FreezePlayerPacket(), player);
                }
                case UNFREEZE_PLAYER -> {
                    frozen = false;
                    IScriptNetwork.sendToPlayer(new UnfreezePlayerPacket(), player);
                }
                case DIALOG -> {
                    var dialog = com.iscript.iscript.data.DialogManager.get((ServerLevel) player.level(), action.getStringValue());
                    if (dialog != null) {
                        var filtered = new com.iscript.iscript.data.dialog.DialogData();
                        filtered.setId(dialog.getId());
                        filtered.setTitle(dialog.getTitle());
                        filtered.setText(dialog.getText());
                        filtered.setPortrait(dialog.getPortrait());
                        IScriptNetwork.sendToPlayer(new OpenDialogScreenPacket(filtered), player);
                    }
                }
                case SOUND -> {
                    try {
                        ResourceLocation id = new ResourceLocation(action.getStringValue());
                        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
                        if (sound != null) {
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    sound, SoundSource.PLAYERS, 1.0f, 1.0f);
                        }
                    } catch (Exception ignored) {}
                }
                case BLOCK -> {
                    try {
                        ResourceLocation id = new ResourceLocation(action.getStringValue());
                        Block block = ForgeRegistries.BLOCKS.getValue(id);
                        if (block != null) {
                            player.level().setBlockAndUpdate(
                                    new BlockPos((int) action.getX(), (int) action.getY(), (int) action.getZ()),
                                    block.defaultBlockState());
                        }
                    } catch (Exception ignored) {}
                }
                case NPC_MOVE -> {
                    Entity entity = player.level().getEntity(action.getIntValue());
                    if (entity instanceof com.iscript.iscript.entity.IScriptNPCEntity npc) {
                        npc.teleportTo(action.getX(), action.getY(), action.getZ());
                    }
                }
                case NPC_ANIMATION -> {
                    Entity entity = player.level().getEntity(action.getIntValue());
                    if (entity instanceof com.iscript.iscript.entity.IScriptNPCEntity npc) {
                        IScriptNetwork.sendToAll(new NPCAnimationPacket(npc.getId(), action.getStringValue()));
                    }
                }
                case SCRIPT -> {
                    var engine = com.iscript.iscript.script.ScriptEngine.getInstance();
                    if (engine.isAvailable()) {
                        try {
                            engine.execute(action.getStringValue(), player, (ServerLevel) player.level());
                        } catch (Exception e) {
                            IScriptMod.LOGGER.error("Cutscene script error: {}", e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
