package com.iscript.iscript.data.cutscene;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerCutsceneHandler {
    private static final Map<UUID, CutsceneInstance> active = new ConcurrentHashMap<>();

    public static void play(ServerPlayer player, CutsceneData data, float speed, int startTick) {
        stop(player, false);
        CutsceneInstance inst = new CutsceneInstance(player, data, speed, startTick);
        active.put(player.getUUID(), inst);
        IScriptNetwork.sendToPlayer(new StartClientCutscenePacket(data, speed), player);
        player.sendSystemMessage(Component.literal("Playing cutscene: " + data.getName()));
    }

    public static void pause(ServerPlayer player) {
        CutsceneInstance inst = active.get(player.getUUID());
        if (inst != null) {
            inst.paused = true;
            IScriptNetwork.sendToPlayer(new PauseClientCutscenePacket(), player);
        }
    }

    public static void resume(ServerPlayer player, float speed, int startTick) {
        CutsceneInstance inst = active.get(player.getUUID());
        if (inst != null) {
            inst.paused = false;
            inst.speed = speed;
            inst.currentTick = startTick;
            IScriptNetwork.sendToPlayer(new ResumeClientCutscenePacket(speed, startTick), player);
        }
    }

    public static void stop(ServerPlayer player, boolean resetPosition) {
        CutsceneInstance inst = active.remove(player.getUUID());
        if (inst != null) {
            IScriptNetwork.sendToPlayer(new StopClientCutscenePacket(resetPosition), player);
            if (resetPosition) {
                teleportBack(player, inst);
            }
            player.sendSystemMessage(Component.literal("Cutscene stopped"));
        }
    }

    private static void teleportBack(ServerPlayer player, CutsceneInstance inst) {
        Vec3 firstPos = inst.getFirstPosition();
        float[] firstRot = inst.getFirstRotation();
        if (firstPos != null) {
            player.teleportTo(firstPos.x, firstPos.y, firstPos.z);
            player.setYRot(firstRot[0]);
            player.setXRot(firstRot[1]);
            player.connection.teleport(firstPos.x, firstPos.y, firstPos.z, firstRot[0], firstRot[1]);
        } else {
            player.teleportTo(inst.returnPos.x, inst.returnPos.y, inst.returnPos.z);
            player.setYRot(inst.returnYaw);
            player.setXRot(inst.returnPitch);
            player.connection.teleport(inst.returnPos.x, inst.returnPos.y, inst.returnPos.z, inst.returnYaw, inst.returnPitch);
        }
    }

    public static boolean isPlaying(ServerPlayer player) {
        return active.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        active.values().removeIf(inst -> {
            inst.tick();
            return inst.isFinished();
        });
    }

    private static class CutsceneInstance {
        private final ServerPlayer player;
        private final CutsceneData data;
        private float speed;
        private int currentTick;
        private final int totalDuration;
        private boolean finished = false;
        private boolean paused = false;

        private final Vec3 returnPos;
        private final float returnYaw;
        private final float returnPitch;

        private int lastActionIndex = -1;

        CutsceneInstance(ServerPlayer player, CutsceneData data, float speed, int startTick) {
            this.player = player;
            this.data = data;
            this.speed = speed;
            this.currentTick = startTick;

            this.returnPos = player.position();
            this.returnYaw = player.getYRot();
            this.returnPitch = player.getXRot();

            int total = 0;
            for (var a : data.getActions()) total += a.getDuration();
            this.totalDuration = total;
        }

        Vec3 getFirstPosition() {
            for (var action : data.getActions()) {
                if (action.isCameraAction()) {
                    return new Vec3(action.getX(), action.getY(), action.getZ());
                }
            }
            return null;
        }

        float[] getFirstRotation() {
            for (var action : data.getActions()) {
                if (action.isCameraAction()) {
                    return new float[]{action.getYaw(), action.getPitch()};
                }
            }
            return new float[]{returnYaw, returnPitch};
        }

        void tick() {
            if (finished || totalDuration <= 0 || paused) return;

            int steps = (int) Math.max(1, Math.round(speed));
            for (int s = 0; s < steps; s++) {
                currentTick++;
                if (currentTick >= totalDuration) {
                    if (data.isLoop()) {
                        currentTick = 0;
                        lastActionIndex = -1;
                        continue;
                    } else {
                        finished = true;
                        return;
                    }
                }
                processActionsAtTick(currentTick);
            }
        }

        private void processActionsAtTick(int tick) {
            int accumulated = 0;
            for (int i = 0; i < data.getActions().size(); i++) {
                var action = data.getActions().get(i);
                int start = accumulated;
                int end = accumulated + action.getDuration();
                if (tick >= start && tick < end) {
                    if (!action.isCameraAction()) {
                        executeAction(action);
                    }
                    break;
                }
                accumulated = end;
            }
        }

        private void executeAction(CutsceneAction action) {
            switch (action.getType()) {
                case DIALOG -> player.sendSystemMessage(Component.literal(action.getStringValue()));
                case SOUND -> {
                }
                case BLOCK -> {
                }
                case SCRIPT -> {
                }
                case FREEZE_PLAYER -> {
                    player.setNoGravity(true);
                    player.setInvulnerable(true);
                }
                case UNFREEZE_PLAYER -> {
                    player.setNoGravity(false);
                    player.setInvulnerable(false);
                }
                case NPC_MOVE, NPC_ANIMATION -> {
                }
                default -> {
                }
            }
        }

        boolean isFinished() {
            return finished;
        }
    }
}