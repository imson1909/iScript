package com.iscript.imson.script.api;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.data.DataAccess;
import com.iscript.imson.data.Graph;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ClientEffectPacket;
import com.iscript.imson.script.ScriptFileManager;
import com.iscript.imson.script.ScriptFunctionExtractor;
import com.iscript.imson.script.ScriptGraphExecutor;
import net.minecraft.server.level.ServerPlayer;
import org.graalvm.polyglot.HostAccess;

import java.util.List;

public class ScriptControlAPI {
    private final ScriptAPI root;

    public ScriptControlAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public void execute(String targetScriptId) {
        execute(targetScriptId, null, 0);
    }

    @HostAccess.Export
    public void execute(String targetScriptId, String functionName) {
        execute(targetScriptId, functionName, 0);
    }

    @HostAccess.Export
    public void execute(String targetScriptId, int delayTicks) {
        execute(targetScriptId, null, delayTicks);
    }

    @HostAccess.Export
    public void execute(String targetScriptId, String functionName, int delayTicks) {
        if (targetScriptId == null || targetScriptId.isEmpty()) {
            sendLog("execute: scriptId is empty", "ERROR", -1);
            return;
        }

        String funcToCall = (functionName == null || functionName.isEmpty()) ? "main" : functionName;

        if (targetScriptId.equals(root.scriptId)) {
            try {
                root.exec.callFunction(targetScriptId, funcToCall, root.player, root.level);
            } catch (Exception e) {
                IScriptMod.LOGGER.error("execute error: {}", e.getMessage());
                sendErrorToPlayer("execute error: " + e.getMessage(), targetScriptId, -1);
            }
            return;
        }

        Runnable task = () -> {
            try {
                if (!root.exec.isScriptLoaded(targetScriptId)) {
                    String js = ScriptFileManager.loadScriptJs(root.level, targetScriptId);
                    if (js == null || js.isEmpty()) {
                        sendLog("execute: script not found: " + targetScriptId, "ERROR", -1);
                        return;
                    }
                    String funcsOnly = ScriptFunctionExtractor.extractFunctionDeclarations(js);
                    root.exec.execute(targetScriptId, funcsOnly, root.player, root.level);
                }
                root.exec.callFunction(targetScriptId, funcToCall, root.player, root.level);
            } catch (Exception e) {
                IScriptMod.LOGGER.error("execute error: {}", e.getMessage());
                sendErrorToPlayer("execute error: " + e.getMessage(), targetScriptId, -1);
            }
        };

        if (delayTicks > 0) {
            root.scheduler.schedule(targetScriptId, root.player, task, delayTicks * 50L);
        } else {
            task.run();
        }
    }

    @HostAccess.Export
    public boolean scriptExists(String targetScriptId) {
        return ScriptFileManager.scriptExists(root.level, targetScriptId);
    }

    @HostAccess.Export
    public List<String> getScriptIds() {
        return ScriptFileManager.listScriptIds(root.level);
    }

    @HostAccess.Export
    public void setTimeout(String script, int delayMs) {
        root.scheduler.schedule(root.scriptId, root.player, () -> {
            try {
                root.exec.execute(root.scriptId, script, root.player, root.level);
            } catch (Exception e) {
                IScriptMod.LOGGER.error("setTimeout error: {}", e.getMessage());
                sendErrorToPlayer("setTimeout error: " + e.getMessage(), root.scriptId, -1);
            }
        }, delayMs * 50L);
    }

    @HostAccess.Export
    public void setInterval(String script, int delayMs) {
        root.scheduler.scheduleRepeating(root.scriptId, root.player, () -> {
            try {
                root.exec.execute(root.scriptId, script, root.player, root.level);
            } catch (Exception e) {
                IScriptMod.LOGGER.error("setInterval error: {}", e.getMessage());
                sendErrorToPlayer("setInterval error: " + e.getMessage(), root.scriptId, -1);
            }
        }, delayMs * 50L, delayMs * 50L);
    }

    @HostAccess.Export
    public void scriptGraphRun(String graphId) {
        if (root.player instanceof ServerPlayer serverPlayer) {
            Graph graph = DataAccess.scriptGraph(graphId);
            if (graph != null) {
                ScriptGraphExecutor executor = new ScriptGraphExecutor(graph, serverPlayer, root.level);
                executor.start();
            }
        }
    }

    private void sendLog(String message, String level, int line) {
        String sourceFile = root.scriptId;
        if (sourceFile == null || sourceFile.isEmpty()) {
            sourceFile = root.exec.getLastScriptIdFor(root.player);
        }
        if (root.player instanceof ServerPlayer serverPlayer) {
            IScriptNetwork.sendToPlayer(
                    new ClientEffectPacket(
                            ClientEffectPacket.Type.LOG_MESSAGE,
                            ClientEffectPacket.logMessageToTag(message, level, sourceFile, line)
                    ),
                    serverPlayer
            );
        }
    }

    private void sendErrorToPlayer(String msg, String scriptId, int line) {
        if (root.player instanceof ServerPlayer serverPlayer) {
            IScriptNetwork.sendToPlayer(
                    new ClientEffectPacket(
                            ClientEffectPacket.Type.LOG_MESSAGE,
                            ClientEffectPacket.logMessageToTag(msg, "ERROR", scriptId, line)
                    ),
                    serverPlayer
            );
        }
    }
}