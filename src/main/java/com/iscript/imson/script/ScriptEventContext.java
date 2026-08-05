package com.iscript.imson.script;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.graalvm.polyglot.HostAccess;

import java.util.HashMap;
import java.util.Map;

public class ScriptEventContext {
    private final Player player;
    private final LivingEntity subject;
    private final Entity object;
    private final ServerLevel level;
    private final MinecraftServer server;
    private final Map<String, Object> values = new HashMap<>();
    private boolean canceled = false;

    public ScriptEventContext(Player player, LivingEntity subject, Entity object, ServerLevel level) {
        this.player = player;
        this.subject = subject;
        this.object = object;
        this.level = level;
        this.server = level != null ? level.getServer() : null;
    }

    @HostAccess.Export
    public Player getPlayer() { return player; }

    @HostAccess.Export
    public LivingEntity getSubject() { return subject; }

    @HostAccess.Export
    public Entity getObject() { return object; }

    @HostAccess.Export
    public ServerLevel getLevel() { return level; }

    @HostAccess.Export
    public MinecraftServer getServer() { return server; }

    @HostAccess.Export
    public Object getValue(String key) { return values.get(key); }

    @HostAccess.Export
    public void setValue(String key, Object value) { values.put(key, value); }

    @HostAccess.Export
    public Map<String, Object> getValues() { return values; }

    @HostAccess.Export
    public void cancel() { this.canceled = true; }

    public boolean isCanceled() { return canceled; }
}