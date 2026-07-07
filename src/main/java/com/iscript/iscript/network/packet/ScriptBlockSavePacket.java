package com.iscript.iscript.network.packet;

import com.iscript.iscript.block.ScriptBlockEntity;
import com.iscript.iscript.script.ScriptFileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScriptBlockSavePacket {
    private final BlockPos pos;
    private final String label;
    private final String scriptId;
    private final String script;

    public ScriptBlockSavePacket(BlockPos pos, String label, String scriptId, String script) {
        this.pos = pos;
        this.label = label;
        this.scriptId = scriptId;
        this.script = script;
    }

    public ScriptBlockSavePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.label = buf.readUtf(32767);
        this.scriptId = buf.readUtf(32767);
        this.script = buf.readUtf(32767);
    }

    public static ScriptBlockSavePacket decode(FriendlyByteBuf buf) {
        return new ScriptBlockSavePacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(label);
        buf.writeUtf(scriptId);
        buf.writeUtf(script);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level() instanceof ServerLevel serverLevel) {
                BlockEntity be = serverLevel.getBlockEntity(pos);
                if (be instanceof ScriptBlockEntity scriptBE) {
                    scriptBE.setLabel(label);
                    scriptBE.setScriptId(scriptId);
                    ScriptFileManager.save(serverLevel, scriptId, script);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}