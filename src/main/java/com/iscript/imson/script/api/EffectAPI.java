package com.iscript.imson.script.api;

import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ClientEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import org.graalvm.polyglot.HostAccess;

public class EffectAPI {
    private final ScriptAPI root;

    public EffectAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public void log(String message) {
        sendLog(message, "INFO", -1);
    }

    @HostAccess.Export
    public void logInfo(String message) {
        sendLog(message, "INFO", -1);
    }

    @HostAccess.Export
    public void logWarn(String message) {
        sendLog(message, "WARN", -1);
    }

    @HostAccess.Export
    public void logError(String message) {
        sendLog(message, "ERROR", -1);
    }

    @HostAccess.Export
    public void logDebug(String message) {
        sendLog(message, "DEBUG", -1);
    }

    private void sendLog(String message, String level, int sourceLine) {
        String sourceFile = root.scriptId;
        if (sourceFile == null || sourceFile.isEmpty()) {
            sourceFile = root.exec.getLastScriptIdFor(root.player);
        }
        if (root.player instanceof ServerPlayer serverPlayer) {
            IScriptNetwork.sendToPlayer(
                    new ClientEffectPacket(
                            ClientEffectPacket.Type.LOG_MESSAGE,
                            ClientEffectPacket.logMessageToTag(message, level, sourceFile, sourceLine)
                    ),
                    serverPlayer
            );
        }
    }
}