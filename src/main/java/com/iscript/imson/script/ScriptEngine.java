package com.iscript.imson.script;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class ScriptEngine {
    private static ScriptEngine instance;
    private final GraalScriptRuntime runtime;
    private final ScriptTaskScheduler scheduler;
    private final ScriptExecutionService exec;

    private ScriptEngine() {
        this.runtime = new GraalScriptRuntime();
        this.scheduler = new ScriptTaskScheduler();
        this.exec = new ScriptExecutionService(runtime, scheduler);
    }

    public static ScriptEngine getInstance() {
        if (instance == null) {
            instance = new ScriptEngine();
        }
        return instance;
    }

    public static void setActiveScriptId(String scriptId) {
        ScriptExecutionService.setActiveScriptId(scriptId);
    }

    public String getLastScriptIdFor(Player player) {
        return exec.getLastScriptIdFor(player);
    }

    public void setGlobal(String name, Object value) {
        runtime.setGlobal(name, value);
    }

    public Object execute(String script, Player player, ServerLevel level) {
        return exec.execute(script, player, level);
    }

    public Object execute(String scriptId, String script, Player player, ServerLevel level) {
        return exec.execute(scriptId, script, player, level);
    }

    public Object callFunction(String scriptId, String functionName, Player player, ServerLevel level, Object... args) {
        return exec.callFunction(scriptId, functionName, player, level, args);
    }

    public boolean isScriptLoaded(String scriptId) {
        return exec.isScriptLoaded(scriptId);
    }

    public void markScriptLoaded(String scriptId) {
        exec.markScriptLoaded(scriptId);
    }

    public void executeTrigger(String scriptId, String functionName, ScriptEventContext eventContext, Player player, ServerLevel level) {
        exec.executeTrigger(scriptId, functionName, eventContext, player, level);
    }

    public void runScriptMain(String scriptId, Player player, ServerLevel level) {
        exec.runScriptMain(scriptId, player, level);
    }

    public void runScriptFunction(String scriptId, String functionName, Player player, ServerLevel level) {
        exec.runScriptFunction(scriptId, functionName, player, level);
    }

    public boolean isAvailable() {
        return runtime.isAvailable();
    }

    public void reload() {
        runtime.reload();
        scheduler.cancelAllTasks();
    }

    public void shutdown() {
        scheduler.cancelAllTasks();
        runtime.shutdown();
    }

    public void onServerTick() {
        scheduler.onServerTick();
    }

    public void schedule(Runnable task, long delayMs) {
        scheduler.schedule(null, null, task, delayMs);
    }

    public void schedule(String scriptId, Player player, Runnable task, long delayMs) {
        scheduler.schedule(scriptId, player, task, delayMs);
    }

    public void scheduleRepeating(Runnable task, long delayMs, long periodMs) {
        scheduler.scheduleRepeating(null, null, task, delayMs, periodMs);
    }

    public void scheduleRepeating(String scriptId, Player player, Runnable task, long delayMs, long periodMs) {
        scheduler.scheduleRepeating(scriptId, player, task, delayMs, periodMs);
    }

    public void cancelAllTasks() {
        scheduler.cancelAllTasks();
    }
}