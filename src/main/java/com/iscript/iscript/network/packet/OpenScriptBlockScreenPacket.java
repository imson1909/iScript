package com.iscript.iscript.network.packet;

import com.iscript.iscript.gui.screen.ScriptBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenScriptBlockScreenPacket {
    private final BlockPos pos;
    private final String label;
    private final String scriptId;
    private final String script;

    public OpenScriptBlockScreenPacket(BlockPos pos, String label, String scriptId) {
        this(pos, label, scriptId, "");
    }

    public OpenScriptBlockScreenPacket(BlockPos pos, String label, String scriptId, String script) {
        this.pos = pos;
        this.label = label;
        this.scriptId = scriptId;
        this.script = script;
    }

    public OpenScriptBlockScreenPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.label = buf.readUtf(32767);
        this.scriptId = buf.readUtf(32767);
        this.script = buf.readUtf(32767);
    }

    public static OpenScriptBlockScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenScriptBlockScreenPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(label);
        buf.writeUtf(scriptId);
        buf.writeUtf(script);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new ScriptBlockScreen(pos, label, scriptId, script));
        });
        ctx.get().setPacketHandled(true);
    }
}