package com.iscript.imson.script;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ClientEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScriptTaskScheduler {
    private final Queue<DelayedTask> taskQueue = new ConcurrentLinkedQueue<>();
    private long serverTickCounter = 0;

    public void onServerTick() {
        serverTickCounter++;
        DelayedTask task;
        while ((task = taskQueue.peek()) != null) {
            if (task.executeTick > serverTickCounter) break;
            taskQueue.poll();
            if (task.cancelled) continue;
            try {
                task.runnable.run();
            } catch (Exception e) {
                IScriptMod.LOGGER.error("Delayed task error: {}", e.getMessage());
                if (task.player instanceof ServerPlayer serverPlayer) {
                    IScriptNetwork.sendToPlayer(
                            new ClientEffectPacket(
                                    ClientEffectPacket.Type.LOG_MESSAGE,
                                    ClientEffectPacket.logMessageToTag("Delayed task error: " + e.getMessage(), "ERROR", task.scriptId, -1)
                            ),
                            serverPlayer
                    );
                }
            }
            if (task.repeating) {
                task.executeTick = serverTickCounter + task.delayTicks;
                taskQueue.offer(task);
            }
        }
    }

    public void schedule(String scriptId, Player player, Runnable task, long delayMs) {
        long delayTicks = Math.max(1, delayMs / 50);
        taskQueue.offer(new DelayedTask(scriptId, player, task, serverTickCounter + delayTicks, false, 0));
    }

    public void scheduleRepeating(String scriptId, Player player, Runnable task, long delayMs, long periodMs) {
        long delayTicks = Math.max(1, delayMs / 50);
        long periodTicks = Math.max(1, periodMs / 50);
        taskQueue.offer(new DelayedTask(scriptId, player, task, serverTickCounter + delayTicks, true, periodTicks));
    }

    public void cancelAllTasks() {
        for (DelayedTask task : taskQueue) {
            task.cancelled = true;
        }
        taskQueue.clear();
    }

    public long getServerTickCounter() {
        return serverTickCounter;
    }

    public static class DelayedTask {
        final String scriptId;
        final Player player;
        final Runnable runnable;
        long executeTick;
        final boolean repeating;
        final long delayTicks;
        volatile boolean cancelled = false;

        DelayedTask(String scriptId, Player player, Runnable runnable, long executeTick, boolean repeating, long delayTicks) {
            this.scriptId = scriptId;
            this.player = player;
            this.runnable = runnable;
            this.executeTick = executeTick;
            this.repeating = repeating;
            this.delayTicks = delayTicks;
        }
    }
}