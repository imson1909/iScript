package com.iscript.iscript.network.packet;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.script.ScriptEngine;
import com.iscript.iscript.script.ScriptFileManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RunScriptPacket {
    private final String scriptId;

    public RunScriptPacket(String scriptId) {
        this.scriptId = scriptId != null ? scriptId : "";
    }

    public RunScriptPacket(FriendlyByteBuf buf) {
        this.scriptId = buf.readUtf(32767);
    }

    public static RunScriptPacket decode(FriendlyByteBuf buf) {
        return new RunScriptPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(scriptId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.level() instanceof ServerLevel serverLevel)) return;

            String script = ScriptFileManager.load(serverLevel, scriptId);
            if (script == null || script.trim().isEmpty()) {
                IScriptMod.LOGGER.warn("[SERVER] RunScriptPacket: script {} is empty or not found", scriptId);
                return;
            }

            ScriptEngine engine = ScriptEngine.getInstance();
            if (!engine.isAvailable()) {
                IScriptMod.LOGGER.warn("[SERVER] ScriptEngine not available");
                return;
            }

            try {
                engine.execute(script, player, serverLevel);
                IScriptMod.LOGGER.info("[SERVER] Executed script: {}", scriptId);
            } catch (Exception e) {
                IScriptMod.LOGGER.error("[SERVER] Script execution error: {}", e.getMessage());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}