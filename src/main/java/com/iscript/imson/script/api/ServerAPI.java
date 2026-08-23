package com.iscript.imson.script.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.graalvm.polyglot.HostAccess;
import java.util.ArrayList;
import java.util.List;

public class ServerAPI {
    private final ScriptAPI root;

    public ServerAPI(ScriptAPI root) {
        this.root = root;
    }

    @HostAccess.Export
    public void send(String text) {
        if (root.level.getServer() != null) {
            for (ServerPlayer p : root.level.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(Component.literal(text));
            }
        }
    }

    @HostAccess.Export
    public net.minecraft.server.MinecraftServer getServer() {
        return root.level.getServer();
    }

    @HostAccess.Export
    public List<String> getAllPlayers() {
        List<String> result = new ArrayList<>();
        if (root.level.getServer() != null) {
            for (ServerPlayer p : root.level.getServer().getPlayerList().getPlayers()) {
                result.add(p.getGameProfile().getName());
            }
        }
        return result;
    }
}