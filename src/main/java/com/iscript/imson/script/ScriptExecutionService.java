package com.iscript.imson.script;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.config.IScriptConfig;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ClientEffectPacket;
import com.iscript.imson.script.api.ScriptAPI;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptExecutionService {
    private static final ThreadLocal<String> ACTIVE_SCRIPT_ID = new ThreadLocal<>();
    private final Map<UUID, String> lastScriptIdByPlayer = new ConcurrentHashMap<>();
    private final Set<String> loadedScripts = ConcurrentHashMap.newKeySet();
    private final GraalScriptRuntime runtime;
    private final ScriptTaskScheduler scheduler;
    private final Object lock = new Object();

    public ScriptExecutionService(GraalScriptRuntime runtime, ScriptTaskScheduler scheduler) {
        this.runtime = runtime;
        this.scheduler = scheduler;
    }

    public static void setActiveScriptId(String scriptId) {
        ACTIVE_SCRIPT_ID.set(scriptId);
    }

    public String getLastScriptIdFor(Player player) {
        return player != null ? lastScriptIdByPlayer.get(player.getUUID()) : null;
    }

    public boolean isScriptLoaded(String scriptId) {
        return loadedScripts.contains(scriptId);
    }

    public void markScriptLoaded(String scriptId) {
        loadedScripts.add(scriptId);
    }

    public Object execute(String script, Player player, ServerLevel level) {
        String id = ACTIVE_SCRIPT_ID.get();
        ACTIVE_SCRIPT_ID.remove();
        if (id == null && player != null) {
            id = lastScriptIdByPlayer.get(player.getUUID());
        }
        if (id == null || id.isEmpty()) {
            id = "<unknown>";
        }
        return execute(id, script, player, level);
    }

    public Object execute(String scriptId, String script, Player player, ServerLevel level) {
        if (scriptId != null && !scriptId.isEmpty() && player != null) {
            lastScriptIdByPlayer.put(player.getUUID(), scriptId);
        }
        if (!IScriptConfig.ENABLE_SCRIPTING.get()) {
            throw new IllegalStateException("Scripting disabled in config");
        }
        if (runtime.getContext() == null) {
            throw new IllegalStateException("GraalJS context not initialized");
        }
        synchronized (lock) {
            try {
                ScriptAPI api = new ScriptAPI(player, level, this, scheduler, scriptId);
                prepareBindings(api, player, level, null);
                Source source = Source.newBuilder("js", script, scriptId != null ? scriptId : "<unknown>").build();
                Value result = runtime.getContext().eval(source);
                if (scriptId != null && !scriptId.isEmpty()) {
                    loadedScripts.add(scriptId);
                }
                return result.isNull() ? null : result.as(Object.class);
            } catch (PolyglotException e) {
                int line = -1;
                if (e.getSourceLocation() != null) {
                    line = e.getSourceLocation().getStartLine();
                }
                String msg = "JS Error: " + e.getMessage();
                sendErrorToPlayer(player, msg, scriptId, line);
                throw new RuntimeException(msg, e);
            } catch (IOException e) {
                throw new RuntimeException("Failed to build script source", e);
            }
        }
    }

    public Object callFunction(String scriptId, String functionName, Player player, ServerLevel level, Object... args) {
        if (scriptId != null && !scriptId.isEmpty() && player != null) {
            lastScriptIdByPlayer.put(player.getUUID(), scriptId);
        }
        if (!IScriptConfig.ENABLE_SCRIPTING.get()) {
            throw new IllegalStateException("Scripting disabled in config");
        }
        if (runtime.getContext() == null) {
            throw new IllegalStateException("GraalJS context not initialized");
        }
        synchronized (lock) {
            try {
                ScriptAPI api = new ScriptAPI(player, level, this, scheduler, scriptId);
                prepareBindings(api, player, level, null);
                Value fn = runtime.getContext().getBindings("js").getMember(functionName);
                if (fn == null || !fn.canExecute()) {
                    throw new RuntimeException("Function '" + functionName + "' not found or not executable");
                }
                Value result = fn.execute(args);
                return result.isNull() ? null : result.as(Object.class);
            } catch (PolyglotException e) {
                int line = -1;
                if (e.getSourceLocation() != null) {
                    line = e.getSourceLocation().getStartLine();
                }
                String msg = "JS Error in '" + functionName + "': " + e.getMessage();
                sendErrorToPlayer(player, msg, scriptId, line);
                throw new RuntimeException(msg, e);
            }
        }
    }

    public void executeTrigger(String scriptId, String functionName, ScriptEventContext eventContext, Player player, ServerLevel level) {
        if (scriptId == null || scriptId.isEmpty()) return;
        if (!runtime.isAvailable()) return;
        synchronized (lock) {
            try {
                if (!isScriptLoaded(scriptId)) {
                    String js = ScriptFileManager.loadScriptJs(level, scriptId);
                    if (js == null || js.isEmpty()) {
                        throw new RuntimeException("Script not found: " + scriptId);
                    }
                    String funcsOnly = ScriptFunctionExtractor.extractFunctionDeclarations(js);
                    execute(scriptId, funcsOnly, player, level);
                }
                ScriptAPI api = new ScriptAPI(player, level, this, scheduler, scriptId);
                prepareBindings(api, player, level, eventContext);
                if (functionName != null && !functionName.isEmpty()) {
                    Value fn = runtime.getContext().getBindings("js").getMember(functionName);
                    if (fn != null && fn.canExecute()) {
                        fn.execute();
                    } else {
                        throw new RuntimeException("Function '" + functionName + "' not found in script " + scriptId);
                    }
                }
            } catch (Exception e) {
                String msg = "Trigger error in '" + scriptId + "': " + e.getMessage();
                IScriptMod.LOGGER.error(msg, e);
                sendErrorToPlayer(player, msg, scriptId, -1);
            }
        }
    }

    public void runScriptMain(String scriptId, Player player, ServerLevel level) {
        runScriptFunction(scriptId, "main", player, level);
    }

    public void runScriptFunction(String scriptId, String functionName, Player player, ServerLevel level) {
        if (scriptId == null || scriptId.isEmpty()) return;
        if (!runtime.isAvailable()) return;
        if (functionName == null || functionName.isEmpty()) functionName = "main";
        synchronized (lock) {
            try {
                String js = ScriptFileManager.loadScriptJs(level, scriptId);
                if (js == null || js.isEmpty()) {
                    throw new RuntimeException("Script not found: " + scriptId);
                }
                IScriptMod.LOGGER.info("[ScriptExecution] Reloading script '{}' from disk ({} chars)", scriptId, js.length());
                String funcsOnly = ScriptFunctionExtractor.extractFunctionDeclarations(js);
                execute(scriptId, funcsOnly, player, level);
                callFunction(scriptId, functionName, player, level);
            } catch (Exception e) {
                String msg = "Run error in '" + scriptId + "': " + e.getMessage();
                IScriptMod.LOGGER.error(msg, e);
                sendErrorToPlayer(player, msg, scriptId, -1);
            }
        }
    }

    private void prepareBindings(ScriptAPI api, Player player, ServerLevel level, ScriptEventContext eventContext) {
        var bindings = runtime.getContext().getBindings("js");
        bindings.putMember("api", api);
        bindings.putMember("player", player);
        bindings.putMember("level", level);
        bindings.putMember("server", level != null ? level.getServer() : null);
        if (eventContext != null) {
            bindings.putMember("event", eventContext);
        }
        for (Map.Entry<String, Object> entry : runtime.getGlobals().entrySet()) {
            bindings.putMember(entry.getKey(), entry.getValue());
        }
    }

    private void sendErrorToPlayer(Player player, String msg, String scriptId, int line) {
        if (player instanceof ServerPlayer serverPlayer) {
            IScriptNetwork.sendToPlayer(
                    new ClientEffectPacket(
                            ClientEffectPacket.Type.LOG_MESSAGE,
                            ClientEffectPacket.logMessageToTag(msg, "ERROR", scriptId, line)
                    ),
                    serverPlayer
            );
        }
    }

    public GraalScriptRuntime getRuntime() {
        return runtime;
    }

    public ScriptTaskScheduler getScheduler() {
        return scheduler;
    }
}