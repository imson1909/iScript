package com.iscript.iscript.network.packet;

import com.iscript.iscript.client.ClientQuestCache;
import com.iscript.iscript.data.NPCManager;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.data.event.EventGraphData;
import com.iscript.iscript.data.event.EventGraphManager;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.quest.QuestProgress;
import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.data.state.ClientMachineData;
import com.iscript.iscript.data.state.ClientNodeData;
import com.iscript.iscript.data.state.ClientTransitionData;
import com.iscript.iscript.data.state.StateMachineManager;
import com.iscript.iscript.entity.IScriptNPCEntity;
import com.iscript.iscript.gui.screen.CutsceneEditorScreen;
import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.gui.screen.NPCListSubScreen;
import com.iscript.iscript.gui.screen.ScriptListSubScreen;
import com.iscript.iscript.gui.screen.StateListSubScreen;
import com.iscript.iscript.script.ScriptGraphManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
        STATE_MACHINE, STATE_MACHINES
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
                case STATE_MACHINE -> handleStateMachine(packet.data);
                case STATE_MACHINES -> handleStateMachines(packet.data);
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
            d.load(t);
            map.put(t.getString("Id"), d);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.setScreen(new CutsceneEditorScreen(map));
    }

    private static void handleEventGraphs(CompoundTag data) {
        Map<String, EventGraphData> map = new HashMap<>();
        ListTag list = data.getList("Graphs", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            EventGraphData g = new EventGraphData();
            g.load(t);
            map.put(t.getString("Id"), g);
        }
        EventGraphManager.updateClientCache(map);
    }

    private static void handleNPCData(CompoundTag data) {
        int entityId = data.getInt("EntityId");
        NPCData npcData = new NPCData();
        npcData.load(data.getCompound("Data"));
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
            d.load(tagList.getCompound(i));
            list.add(d);
        }
        NPCManager.updateClientCache(list);
        if (Minecraft.getInstance().screen instanceof DashboardScreen ds && ds.currentSubScreen instanceof NPCListSubScreen sub) {
            sub.receiveList(list);
        }
    }

    private static void handleQuestProgress(CompoundTag data) {
        ClientQuestCache.update(data);
    }

    private static void handleScriptGraphs(CompoundTag data) {
        Map<String, ScriptGraphData> map = new HashMap<>();
        ListTag list = data.getList("Graphs", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            ScriptGraphData g = new ScriptGraphData();
            g.load(t);
            map.put(t.getString("Id"), g);
        }
        ScriptGraphManager.updateClientCache(map);
    }

    private static void handleScriptContent(CompoundTag data) {
        String id = data.getString("Id");
        String js = data.getString("JsText");
        ScriptGraphData graph = new ScriptGraphData();
        graph.load(data.getCompound("Graph"));
        ScriptGraphManager.putClientCache(id, graph);
        ScriptGraphManager.updateClientJsCache(id, js);
        if (Minecraft.getInstance().screen instanceof DashboardScreen dash && dash.currentSubScreen instanceof ScriptListSubScreen sub) {
            sub.onContentReceived(id, js);
        }
    }

    private static void handleStateMachine(CompoundTag data) {
        String id = data.getString("Id");
        String name = data.getString("Name");
        String entry = data.getString("EntryNode");
        String nodesJson = data.getString("NodesJson");
        ClientMachineData machine = parseClientMachine(id, name, entry, nodesJson);
        StateMachineManager.setClientMachineData(id, machine);
        if (Minecraft.getInstance().screen instanceof DashboardScreen dash && dash.currentSubScreen instanceof StateListSubScreen sub) {
            sub.onMachineReceived(id, name, entry.isEmpty() ? null : entry, nodesJson);
        }
    }

    private static void handleStateMachines(CompoundTag data) {
        Map<String, String> map = new HashMap<>();
        ListTag list = data.getList("Machines", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            map.put(t.getString("Id"), t.getString("Name"));
        }
        StateMachineManager.setClientCache(map);
    }

    private static ClientMachineData parseClientMachine(String id, String name, String entry, String nodesJson) {
        ClientMachineData data = new ClientMachineData();
        data.id = id;
        data.name = name;
        data.entryNode = entry.isEmpty() ? null : entry;
        if (nodesJson != null && !nodesJson.isEmpty()) {
            try {
                JsonArray arr = JsonParser.parseString(nodesJson).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    ClientNodeData node = new ClientNodeData();
                    node.id = obj.get("id").getAsString();
                    node.name = obj.has("name") ? obj.get("name").getAsString() : node.id;
                    node.color = obj.has("color") ? obj.get("color").getAsInt() : 0xFF4488AA;
                    node.posX = obj.has("posX") ? obj.get("posX").getAsInt() : 0;
                    node.posY = obj.has("posY") ? obj.get("posY").getAsInt() : 0;
                    if (obj.has("transitions")) {
                        JsonArray tarr = obj.getAsJsonArray("transitions");
                        for (JsonElement tel : tarr) {
                            JsonObject tobj = tel.getAsJsonObject();
                            ClientTransitionData trans = new ClientTransitionData();
                            trans.targetNode = tobj.get("targetNode").getAsString();
                            trans.auto = tobj.has("auto") && tobj.get("auto").getAsBoolean();
                            node.transitions.add(trans);
                        }
                    }
                    data.nodes.add(node);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return data;
    }

    public static CompoundTag cutscenesToTag(Map<String, CutsceneData> map) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (var e : map.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", e.getKey());
            e.getValue().save(t);
            list.add(t);
        }
        data.put("Cutscenes", list);
        return data;
    }

    public static CompoundTag eventGraphsToTag(Map<String, EventGraphData> map) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (var e : map.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", e.getKey());
            e.getValue().save(t);
            list.add(t);
        }
        data.put("Graphs", list);
        return data;
    }

    public static CompoundTag npcDataToTag(int entityId, NPCData npcData) {
        CompoundTag data = new CompoundTag();
        data.putInt("EntityId", entityId);
        CompoundTag d = new CompoundTag();
        npcData.save(d);
        data.put("Data", d);
        return data;
    }

    public static CompoundTag npcListToTag(List<NPCData> list) {
        CompoundTag data = new CompoundTag();
        ListTag tagList = new ListTag();
        for (NPCData d : list) {
            CompoundTag t = new CompoundTag();
            d.save(t);
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

    public static CompoundTag scriptGraphsToTag(Map<String, ScriptGraphData> map) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (var e : map.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", e.getKey());
            e.getValue().save(t);
            list.add(t);
        }
        data.put("Graphs", list);
        return data;
    }

    public static CompoundTag scriptContentToTag(String id, String jsText, ScriptGraphData graph) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        data.putString("JsText", jsText);
        CompoundTag g = new CompoundTag();
        graph.save(g);
        data.put("Graph", g);
        return data;
    }

    public static CompoundTag stateMachineToTag(String id, String name, String entryNode, String nodesJson) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        data.putString("Name", name);
        data.putString("EntryNode", entryNode != null ? entryNode : "");
        data.putString("NodesJson", nodesJson);
        return data;
    }

    public static CompoundTag stateMachinesToTag(Map<String, String> map) {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (var e : map.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", e.getKey());
            t.putString("Name", e.getValue());
            list.add(t);
        }
        data.put("Machines", list);
        return data;
    }
}