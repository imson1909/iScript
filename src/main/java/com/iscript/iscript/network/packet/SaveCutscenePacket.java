package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.CutsceneManager;
import com.iscript.iscript.data.cutscene.CutsceneData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SaveCutscenePacket {
    private final CutsceneData cutscene;

    public SaveCutscenePacket(CutsceneData cutscene) {
        this.cutscene = cutscene;
    }

    public static void encode(SaveCutscenePacket packet, FriendlyByteBuf buf) {
        CompoundTag tag = new CompoundTag();
        packet.cutscene.save(tag);
        buf.writeNbt(tag);
    }

    public static SaveCutscenePacket decode(FriendlyByteBuf buf) {
        CutsceneData data = new CutsceneData();
        data.load(buf.readNbt());
        return new SaveCutscenePacket(data);
    }

    public static void handle(SaveCutscenePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                CutsceneManager.add((ServerLevel) player.level(), packet.cutscene);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}