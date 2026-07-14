package com.iscript.iscript.network.packet;

import com.iscript.iscript.gui.screen.CutsceneEditorScreen;
import com.iscript.iscript.data.cutscene.CutsceneData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncCutscenesPacket {
    private final Map<String, CutsceneData> cutscenes;

    public SyncCutscenesPacket(Map<String, CutsceneData> cutscenes) {
        this.cutscenes = cutscenes;
    }

    public static void encode(SyncCutscenesPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.cutscenes.size());
        for (var entry : packet.cutscenes.entrySet()) {
            buf.writeUtf(entry.getKey());
            CompoundTag tag = new CompoundTag();
            entry.getValue().save(tag);
            buf.writeNbt(tag);
        }
    }

    public static SyncCutscenesPacket decode(FriendlyByteBuf buf) {
        Map<String, CutsceneData> map = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            CutsceneData data = new CutsceneData();
            data.load(buf.readNbt());
            map.put(id, data);
        }
        return new SyncCutscenesPacket(map);
    }

    public static void handle(SyncCutscenesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.setScreen(new CutsceneEditorScreen(packet.cutscenes));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}