package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.ClientQuestCache;
import com.iscript.iscript.data.Graph;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.NPCManager;
import com.iscript.iscript.data.quest.QuestProgress;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.entity.IScriptNPCEntity;
import com.iscript.iscript.gui.screen.CutsceneEditorScreen;
import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.screen.NPCListSubScreen;
import com.iscript.iscript.gui.screen.ScriptListSubScreen;
import com.iscript.iscript.gui.screen.StateListSubScreen;
import com.iscript.iscript.script.ScriptGraphManager;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SyncDataPacket {
    public enum Type {
        CUTSCENES, EVENT_GRAPHS, NPC_DATA, NPC_LIST,
        QUEST_PROGRESS, SCRIPT_GRAPHS, SCRIPT_CONTENT,
        STATES, DASHBOARD_LIST, STATE_GRAPHS
    }

    private final Type type;
    private final CompoundTag data;

    public SyncDataPacket(Type type, CompoundTag data) {
        this.type = type;
        this.data = data != null ? data : new CompoundTag();
    }

    public static void encode(SyncDataPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.type);
        buf.writeNbt(packet.data);
    }

    public static SyncDataPacket decode(FriendlyByteBuf buf) {
        return new SyncDataPacket(buf.readEnum(Type.class), buf.readNbt());
    }

    public static void handle(SyncDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            switch (packet.type) {
                case CUTSCENES -> handleCutscenes(packet.data);
                case EVENT_GRAPHS -> handleEventGraphs(packet.data);
                case NPC_DATA -> handleNPCData(packet.data);
                case NPC_LIST -> handleNPCList(packet.data);
                case QUEST_PROGRESS -> handleQuestProgress(packet.data);
                case SCRIPT_GRAPHS -> handleScriptGraphs(packet.data);
                case SCRIPT_CONTENT -> handleScriptContent(packet.data);
                case STATES -> {
                    if (Minecraft.getInstance().screen instanceof DashboardScreen ds && ds.currentSubScreen instanceof StateListSubScreen sub) {
                        sub.receiveStates(packet.data);
                    }
                }
                case DASHBOARD_LIST -> handleDashboardList(packet.data);
                case STATE_GRAPHS -> handleStateGraphs(packet.data);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleCutscenes(CompoundTag data) {
        Map<String, CutsceneData> map = new HashMap<>();
        ListTag list = data.getList("Cutscenes", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            CutsceneData d = new CutsceneData();
            d.fromJson(JsonParser.parseString(t.getString("json")).getAsJsonObject());
            map.put(t.getString("Id"), d);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.setScreen(new CutsceneEditorScreen(map));
    }

    private static void handleEventGraphs(CompoundTag data) {
        Map<String, Graph> map = new HashMap<>();
        ListTag list = data.getList("Graphs", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Graph g = new Graph(ScriptNodeType.class);
            g.fromJson(JsonParser.parseString(t.getString("json")).getAsJsonObject());
            map.put(t.getString("Id"), g);
        }
    }

    private static void handleStateGraphs(CompoundTag data) {
        Map<String, Graph> map = new HashMap<>();
        ListTag list = data.getList("Graphs", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Graph g = new Graph(null);
            g.fromJson(JsonParser.parseString(t.getString("json")).getAsJsonObject());
            map.put(t.getString("Id"), g);
        }
    }

    private static void handleNPCData(CompoundTag data) {
        int entityId = data.getInt("EntityId");
        NPCData npcData = new NPCData();
        npcData.fromJson(JsonParser.parseString(data.getString("json")).getAsJsonObject());
        if (Minecraft.getInstance().level != null) {
            var entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity instanceof IScriptNPCEntity npc) npc.applyNPCDataClient(npcData);
        }
    }

    private static void handleNPCList(CompoundTag data) {
        List<NPCData> list = new ArrayList<>();
        ListTag tagList = data.getList("NPCs", 10);
        for (int i = 0; i < tagList.size(); i++) {
            NPCData d = new NPCData();
            d.fromJson(JsonParser.parseString(tagList.getCompound(i).getString("json")).getAsJsonObject());
            list.add(d);
        }
        com.iscript.iscript.data.NPCManager.updateClientCache(list);
        if (Minecraft.getInstance().screen instanceof DashboardScreen ds && ds.currentSubScreen instanceof NPCListSubScreen sub) {
            sub.receiveList(list);
        }
    }

    private static void handleQuestProgress(CompoundTag data) {
        ClientQuestCache.update(data);
    }

    private static void handleScriptGraphs(CompoundTag data) {
        Map<String, Graph> map = new HashMap<>();
        ListTag list = data.getList("Graphs", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Graph g = new Graph(ScriptNodeType.class);
            g.fromJson(JsonParser.parseString(t.getString("json")).getAsJsonObject());
            map.put(t.getString("Id"), g);
        }
        ScriptGraphManager.updateClientCache(map);
    }

    private static void handleScriptContent(CompoundTag data) {
        String id = data.getString("Id");
        String js = data.getString("JsText");
        Graph graph = new Graph(ScriptNodeType.class);
        graph.fromJson(JsonParser.parseString(data.getString("json")).getAsJsonObject());
        ScriptGraphManager.putClientCache(id, graph);
        ScriptGraphManager.updateClientJsCache(id, js);
        if (Minecraft.getInstance().screen instanceof DashboardScreen dash) {
            dash.onScriptContentReceived(id, js);
        }
    }

    private static void handleDashboardList(CompoundTag data) {
        if (Minecraft.getInstance().screen instanceof DashboardScreen ds) {
            ds.receiveDashboardList(data);
        }
    }

    public static CompoundTag stateGraphsToTag(Map<String, Graph> map) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (var e : map.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", e.getKey());
            t.putString("json", e.getValue().toJson().toString());
            list.add(t);
        }
        data.put("Graphs", list);
        return data;
    }

    public static CompoundTag cutscenesToTag(Map<String, CutsceneData> map) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (var e : map.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", e.getKey());
            t.putString("json", e.getValue().toJson().toString());
            list.add(t);
        }
        data.put("Cutscenes", list);
        return data;
    }

    public static CompoundTag eventGraphsToTag(Map<String, Graph> map) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (var e : map.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", e.getKey());
            t.putString("json", e.getValue().toJson().toString());
            list.add(t);
        }
        data.put("Graphs", list);
        return data;
    }

    public static CompoundTag npcDataToTag(int entityId, NPCData npcData) {
        CompoundTag data = new CompoundTag();
        data.putInt("EntityId", entityId);
        data.putString("json", npcData.toJson().toString());
        return data;
    }

    public static CompoundTag npcListToTag(List<NPCData> list) {
        CompoundTag data = new CompoundTag();
        ListTag tagList = new ListTag();
        for (NPCData d : list) {
            CompoundTag t = new CompoundTag();
            t.putString("json", d.toJson().toString());
            tagList.add(t);
        }
        data.put("NPCs", tagList);
        return data;
    }

    public static CompoundTag questProgressToTag(Map<String, QuestProgress> active, java.util.Set<String> completed) {
        CompoundTag data = new CompoundTag();
        ListTag activeList = new ListTag();
        for (QuestProgress p : active.values()) {
            CompoundTag t = new CompoundTag();
            p.save(t);
            activeList.add(t);
        }
        data.put("Active", activeList);
        ListTag completedList = new ListTag();
        for (String id : completed) completedList.add(StringTag.valueOf(id));
        data.put("Completed", completedList);
        return data;
    }

    public static CompoundTag scriptGraphsToTag(Map<String, Graph> map) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (var e : map.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", e.getKey());
            t.putString("json", e.getValue().toJson().toString());
            list.add(t);
        }
        data.put("Graphs", list);
        return data;
    }

    public static CompoundTag scriptContentToTag(String id, String jsText, Graph graph) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        data.putString("JsText", jsText);
        data.putString("json", graph.toJson().toString());
        return data;
    }

    public static CompoundTag dashboardListToTag(String category, List<CompoundTag> items) {
        CompoundTag data = new CompoundTag();
        data.putString("Category", category);
        ListTag list = new ListTag();
        for (CompoundTag t : items) list.add(t);
        data.put("Items", list);
        return data;
    }
}