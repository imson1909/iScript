package com.iscript.iscript.network.packet;

import com.iscript.iscript.gui.screen.QuestJournalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenQuestJournalPacket {
    private final CompoundTag tag;

    public OpenQuestJournalPacket(CompoundTag tag) {
        this.tag = tag;
    }

    public OpenQuestJournalPacket(FriendlyByteBuf buf) {
        this.tag = buf.readNbt();
    }

    public static OpenQuestJournalPacket decode(FriendlyByteBuf buf) {
        return new OpenQuestJournalPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance() != null) {
                Minecraft.getInstance().setScreen(new QuestJournalScreen(tag));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
