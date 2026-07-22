package com.iscript.iscript.data.cutscene;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.client.camera.*;
import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.ClientEffectPacket;
import com.iscript.iscript.network.packet.OpenGuiPacket;
import com.iscript.iscript.network.packet.SplineCameraMovePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CutscenePlayer {
    private static final Map<ServerPlayer, Session> active = new HashMap<>();

    public static void play(ServerPlayer player, CutsceneData cutscene) {
        if (active.containsKey(player)) return;
        active.put(player, new Session(player, cutscene));
    }

    public static void stop(ServerPlayer player) {
        Session s = active.remove(player);
        if (s != null) s.stop();
    }

    public static boolean isPlaying(ServerPlayer player) {
        return active.containsKey(player);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        active.values().removeIf(s -> {
            s.tick();
            return s.isFinished();
        });
    }

    private static class Session {
        private final ServerPlayer player;
        private final CutsceneData cutscene;
        private int actionIndex;
        private int actionTick;
        private boolean waiting;
        private boolean frozen;
        private boolean stopped;

        Session(ServerPlayer player, CutsceneData cutscene) {
            this.player = player;
            this.cutscene = cutscene;
        }

        void stop() {
            if (stopped) return;
            stopped = true;
            if (frozen) sendEffect(ClientEffectPacket.Type.UNFREEZE_PLAYER);
            sendEffect(ClientEffectPacket.Type.CAMERA_RESET);
        }

        void tick() {
            if (stopped) return;
            if (actionIndex >= cutscene.getActions().size()) {
                if (cutscene.isLoop()) { actionIndex = 0; actionTick = 0; waiting = false; return; }
                stop();
                return;
            }
            CutsceneAction a = cutscene.getActions().get(actionIndex);
            if (!waiting) {
                execute(a);
                if (a.isCameraAction() || a.getType() == CutsceneActionType.DELAY || a.getType() == CutsceneActionType.NPC_MOVE) {
                    waiting = true;
                    actionTick = a.getDuration();
                } else actionIndex++;
            } else {
                actionTick--;
                if (actionTick <= 0) { waiting = false; actionIndex++; }
            }
        }

        boolean isFinished() { return stopped; }

        private void execute(CutsceneAction a) {
            switch (a.getType()) {
                case CAMERA_IDLE -> sendCamera(a, buildPath(a, false));
                case CAMERA_PATH -> sendCamera(a, buildPath(a, true));
                case CAMERA_LOOK -> {
                    CutsceneCameraMode m = new CutsceneCameraMode();
                    m.useLookAt = true;
                    m.lookAtTarget = a.getLookAt();
                    sendPacket(new SplineCameraMovePacket(null, m, frozen));
                }
                case CAMERA_FOLLOW -> {
                    Entity e = player.level().getEntity(a.getIntValue());
                    if (e == null) return;
                    sendCamera(a, buildFollowPath(a, e.position()));
                }
                case CAMERA_ORBIT -> {
                    CutsceneCameraMode m = new CutsceneCameraMode();
                    m.orbitMode = true;
                    m.orbitCenter = a.getPosition();
                    m.orbitRadius = a.getOrbitRadius();
                    m.orbitHeight = a.getOrbitHeight();
                    m.orbitSpeed = a.getOrbitSpeed();
                    sendPacket(new SplineCameraMovePacket(buildPath(a, false), m, frozen));
                }
                case CAMERA_DOLLY -> {
                    CutsceneCameraMode m = new CutsceneCameraMode();
                    m.useDollyZoom = true;
                    m.dollyTarget = a.getLookAt();
                    m.dollyBaseFov = a.getDollyBaseFov();
                    m.dollyBaseDistance = a.getDollyTargetDistance();
                    sendPacket(new SplineCameraMovePacket(buildPath(a, false), m, frozen));
                }
                case CAMERA_SHAKE -> {
                    CutsceneCameraMode m = new CutsceneCameraMode();
                    m.shake = new CameraShake();
                    m.shake.setTrauma(a.getShakeTrauma());
                    m.shake.setDecay(a.getShakeDecay());
                    m.shake.setMaxAngle(a.getShakeMaxAngle());
                    m.shake.setMaxOffset(a.getShakeMaxOffset());
                    sendPacket(new SplineCameraMovePacket(null, m, frozen));
                }
                case FREEZE_PLAYER -> { frozen = true; sendEffect(ClientEffectPacket.Type.FREEZE_PLAYER); }
                case UNFREEZE_PLAYER -> { frozen = false; sendEffect(ClientEffectPacket.Type.UNFREEZE_PLAYER); }
                case DIALOG -> {
                    var d = DataAccess.dialog(a.getStringValue());
                    if (d != null) {
                        var f = new com.iscript.iscript.data.dialog.DialogData();
                        f.setId(d.getId());
                        f.setTitle(d.getTitle());
                        f.setText(d.getText());
                        f.setPortrait(d.getPortrait());
                        IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG, OpenGuiPacket.dialogToTag(f)), player);
                    }
                }
                case SOUND -> {
                    try {
                        SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(a.getStringValue()));
                        if (s != null) player.level().playSound(null, player.getX(), player.getY(), player.getZ(), s, SoundSource.PLAYERS, 1.0f, 1.0f);
                    } catch (Exception ignored) {}
                }
                case BLOCK -> {
                    try {
                        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(a.getStringValue()));
                        if (b != null) player.level().setBlockAndUpdate(new BlockPos((int) a.getX(), (int) a.getY(), (int) a.getZ()), b.defaultBlockState());
                    } catch (Exception ignored) {}
                }
                case NPC_MOVE -> {
                    Entity e = player.level().getEntity(a.getIntValue());
                    if (e instanceof com.iscript.iscript.entity.IScriptNPCEntity npc) npc.teleportTo(a.getX(), a.getY(), a.getZ());
                }
                case NPC_ANIMATION -> {
                    Entity e = player.level().getEntity(a.getIntValue());
                    if (e instanceof com.iscript.iscript.entity.IScriptNPCEntity npc)
                        IScriptNetwork.sendToAll(new ClientEffectPacket(ClientEffectPacket.Type.NPC_ANIMATION, ClientEffectPacket.npcAnimToTag(npc.getId(), a.getStringValue())));
                }
                case SCRIPT -> {
                    var engine = com.iscript.iscript.script.ScriptEngine.getInstance();
                    if (engine.isAvailable()) try { engine.execute(a.getStringValue(), player, (ServerLevel) player.level()); }
                    catch (Exception e) { IScriptMod.LOGGER.error("Cutscene script error: {}", e.getMessage()); }
                }
                default -> {}
            }
        }

        private CutscenePath buildPath(CutsceneAction a, boolean useSpline) {
            CutscenePath path = new CutscenePath();
            path.durationTicks = a.getDuration();
            try { path.easing = Interpolation.valueOf(a.getInterpolation()); } catch (Exception ignored) {}
            PathType segType;
            try { segType = PathType.valueOf(a.getPathType()); } catch (Exception e) { segType = PathType.LINEAR; }

            List<Vec3> pts = useSpline && !a.getSplinePoints().isEmpty() ? a.getSplinePoints() : List.of(a.getPosition());
            List<Float> yws = useSpline && !a.getSplineYaws().isEmpty() ? a.getSplineYaws() : List.of(a.getYaw());
            List<Float> ptsList = useSpline && !a.getSplinePitches().isEmpty() ? a.getSplinePitches() : List.of(a.getPitch());

            for (int i = 0; i < pts.size(); i++) {
                float tick = (i / (float) Math.max(1, pts.size() - 1)) * a.getDuration();
                path.keyframes.add(new CutscenePath.Keyframe(pts.get(i),
                        i < yws.size() ? yws.get(i) : a.getYaw(),
                        i < ptsList.size() ? ptsList.get(i) : a.getPitch(),
                        a.getRoll(), a.isUseFov() ? a.getFov() : 70f, tick, segType));
            }
            if (a.isConstantSpeed()) path.recalculateForConstantSpeed(a.getSpeed());
            return path;
        }

        private CutscenePath buildFollowPath(CutsceneAction a, Vec3 pos) {
            CutscenePath path = new CutscenePath();
            path.durationTicks = a.getDuration();
            PathType segType;
            try { segType = PathType.valueOf(a.getPathType()); } catch (Exception e) { segType = PathType.LINEAR; }
            path.keyframes.add(new CutscenePath.Keyframe(pos, a.getYaw(), a.getPitch(), a.getRoll(), a.isUseFov() ? a.getFov() : 70f, 0, segType));
            path.keyframes.add(new CutscenePath.Keyframe(pos, a.getYaw(), a.getPitch(), a.getRoll(), a.isUseFov() ? a.getFov() : 70f, a.getDuration(), segType));
            return path;
        }

        private void sendCamera(CutsceneAction a, CutscenePath path) {
            CutsceneCameraMode m = new CutsceneCameraMode();
            if (a.isUseFov()) m.dollyBaseFov = a.getFov();
            sendPacket(new SplineCameraMovePacket(path, m, frozen));
        }

        private void sendPacket(SplineCameraMovePacket p) { IScriptNetwork.sendToPlayer(p, player); }
        private void sendEffect(ClientEffectPacket.Type type) { IScriptNetwork.sendToPlayer(new ClientEffectPacket(type, new net.minecraft.nbt.CompoundTag()), player); }
    }
}