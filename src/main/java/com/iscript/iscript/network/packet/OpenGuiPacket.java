package com.iscript.iscript.network.packet;

import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.npc.NPCTradeData;
import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.screen.DialogScreen;
import com.iscript.iscript.gui.screen.NPCEditScreen;
import com.iscript.iscript.gui.screen.NPCTradeScreen;
import com.iscript.iscript.gui.screen.QuestJournalScreen;
import com.iscript.iscript.gui.screen.RegionEditScreen;
import com.iscript.iscript.gui.screen.ScriptBlockScreen;
import com.iscript.iscript.morph.gui.MorphScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenGuiPacket {
    public enum Type {
        DASHBOARD, DIALOG, NPC_EDIT, NPC_TRADE,
        QUEST_JOURNAL, REGION_EDIT, SCRIPT_BLOCK, MORPH
    }

    private final Type type;
    private final CompoundTag data;

    public OpenGuiPacket(Type type, CompoundTag data) {
        this.type = type;
        this.data = data != null ? data : new CompoundTag();
    }

    public static void encode(OpenGuiPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.type);
        buf.writeNbt(packet.data);
    }

    public static OpenGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenGuiPacket(buf.readEnum(Type.class), buf.readNbt());
    }

    public static void handle(OpenGuiPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            switch (packet.type) {
                case DASHBOARD -> Minecraft.getInstance().setScreen(new DashboardScreen());
                case DIALOG -> handleDialog(packet.data);
                case NPC_EDIT -> handleNPCEdit(packet.data);
                case NPC_TRADE -> handleNPCTrade(packet.data);
                case QUEST_JOURNAL -> handleQuestJournal(packet.data);
                case REGION_EDIT -> handleRegionEdit(packet.data);
                case SCRIPT_BLOCK -> handleScriptBlock(packet.data);
                case MORPH -> handleMorph();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleDialog(CompoundTag data) {
        DialogData d = new DialogData();
        d.setTitle(data.getString("Title"));
        d.setText(data.getString("Text"));
        d.setPortrait(data.getString("Portrait"));
        int count = data.getInt("OptionCount");
        for (int i = 0; i < count; i++) {
            DialogData.DialogOption opt = new DialogData.DialogOption();
            opt.setText(data.getString("OptText" + i));
            opt.setCommand(data.getString("OptCmd" + i));
            opt.setTargetDialogId(data.getString("OptTarget" + i));
            d.getOptions().add(opt);
        }
        Minecraft.getInstance().setScreen(new DialogScreen(d));
    }

    private static void handleNPCEdit(CompoundTag data) {
        int entityId = data.getInt("EntityId");
        NPCData npcData = new NPCData();
        npcData.load(data.getCompound("Data"));
        Minecraft.getInstance().setScreen(new NPCEditScreen(entityId, npcData));
    }

    private static void handleNPCTrade(CompoundTag data) {
        int entityId = data.getInt("EntityId");
        NPCTradeData tradeData = new NPCTradeData();
        tradeData.load(data.getCompound("Data"));
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().setScreen(new NPCTradeScreen(entityId, tradeData));
        }
    }

    private static void handleQuestJournal(CompoundTag data) {
        Minecraft.getInstance().setScreen(new QuestJournalScreen(data.getCompound("Progress"), data.getCompound("Meta")));
    }

    private static void handleRegionEdit(CompoundTag data) {
        BlockPos pos = new BlockPos(data.getInt("X"), data.getInt("Y"), data.getInt("Z"));
        RegionData d = new RegionData();
        d.load(data.getCompound("Data"));
        Minecraft.getInstance().setScreen(new RegionEditScreen(pos, d));
    }

    private static void handleScriptBlock(CompoundTag data) {
        BlockPos pos = new BlockPos(data.getInt("X"), data.getInt("Y"), data.getInt("Z"));
        Minecraft.getInstance().setScreen(new ScriptBlockScreen(
                pos, data.getString("Label"), data.getString("ScriptId"), data.getString("Script")
        ));
    }

    private static void handleMorph() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().setScreen(new MorphScreen(Minecraft.getInstance().player));
        }
    }

    public static CompoundTag dialogToTag(DialogData dialog) {
        CompoundTag data = new CompoundTag();
        data.putString("Title", dialog.getTitle());
        data.putString("Text", dialog.getText());
        data.putString("Portrait", dialog.getPortrait());
        int count = dialog.getOptions().size();
        data.putInt("OptionCount", count);
        for (int i = 0; i < count; i++) {
            DialogData.DialogOption opt = dialog.getOptions().get(i);
            data.putString("OptText" + i, opt.getText());
            data.putString("OptCmd" + i, opt.getCommand());
            data.putString("OptTarget" + i, opt.getTargetDialogId());
        }
        return data;
    }

    public static CompoundTag npcEditToTag(int entityId, NPCData npcData) {
        CompoundTag data = new CompoundTag();
        data.putInt("EntityId", entityId);
        CompoundTag d = new CompoundTag();
        npcData.save(d);
        data.put("Data", d);
        return data;
    }

    public static CompoundTag npcTradeToTag(int entityId, NPCTradeData tradeData) {
        CompoundTag data = new CompoundTag();
        data.putInt("EntityId", entityId);
        CompoundTag d = new CompoundTag();
        tradeData.save(d);
        data.put("Data", d);
        return data;
    }

    public static CompoundTag questJournalToTag(CompoundTag progress, CompoundTag meta) {
        CompoundTag data = new CompoundTag();
        data.put("Progress", progress);
        data.put("Meta", meta);
        return data;
    }

    public static CompoundTag regionEditToTag(BlockPos pos, RegionData regionData) {
        CompoundTag data = new CompoundTag();
        data.putInt("X", pos.getX());
        data.putInt("Y", pos.getY());
        data.putInt("Z", pos.getZ());
        CompoundTag d = new CompoundTag();
        regionData.save(d);
        data.put("Data", d);
        return data;
    }

    public static CompoundTag scriptBlockToTag(BlockPos pos, String label, String scriptId, String script) {
        CompoundTag data = new CompoundTag();
        data.putInt("X", pos.getX());
        data.putInt("Y", pos.getY());
        data.putInt("Z", pos.getZ());
        data.putString("Label", label);
        data.putString("ScriptId", scriptId);
        data.putString("Script", script);
        return data;
    }
}