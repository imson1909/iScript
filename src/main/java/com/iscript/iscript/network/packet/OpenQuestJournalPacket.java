package com.iscript.iscript.network.packet;

import com.iscript.iscript.gui.screen.QuestJournalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenQuestJournalPacket {
    private final CompoundTag progressTag;
    private final CompoundTag metaTag;

    public OpenQuestJournalPacket(CompoundTag progressTag, CompoundTag metaTag) {
        this.progressTag = progressTag;
        this.metaTag = metaTag;
    }

    public OpenQuestJournalPacket(FriendlyByteBuf buf) {
        this.progressTag = buf.readNbt();
        this.metaTag = buf.readNbt();
    }

    public static OpenQuestJournalPacket decode(FriendlyByteBuf buf) {
        return new OpenQuestJournalPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(progressTag);
        buf.writeNbt(metaTag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance() != null) {
                Minecraft.getInstance().setScreen(new QuestJournalScreen(progressTag, metaTag));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}