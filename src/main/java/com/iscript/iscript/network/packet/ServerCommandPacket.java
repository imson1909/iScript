package com.iscript.iscript.network.packet;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.block.ScriptBlockEntity;
import com.iscript.iscript.blockentities.RegionBlockEntity;
import com.iscript.iscript.data.CutsceneManager;
import com.iscript.iscript.data.DialogGraphManager;
import com.iscript.iscript.data.DialogManager;
import com.iscript.iscript.data.NPCManager;
import com.iscript.iscript.data.QuestManager;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.data.cutscene.ServerCutsceneHandler;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.dialog.DialogGraphData;
import com.iscript.iscript.data.event.EventGraphData;
import com.iscript.iscript.data.event.EventGraphManager;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.npc.NPCTradeData;
import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.data.script.ScriptNodeConnection;
import com.iscript.iscript.data.script.ScriptNodeData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.data.state.StateMachine;
import com.iscript.iscript.data.state.StateMachineData;
import com.iscript.iscript.data.state.StateNode;
import com.iscript.iscript.data.state.StateTransition;
import com.iscript.iscript.entity.IScriptNPCEntity;
import com.iscript.iscript.event.EventManager;
import com.iscript.iscript.event.EventType;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.registry.ModEntities;
import com.iscript.iscript.script.ScriptEngine;
import com.iscript.iscript.script.ScriptFileManager;
import com.iscript.iscript.script.ScriptGraphManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public class ServerCommandPacket {
    public enum Type {
        PLAY_CUTSCENE, DELETE_CUTSCENE, SAVE_CUTSCENE,
        PAUSE_CUTSCENE, RESUME_CUTSCENE, STOP_CUTSCENE, CUTSCENE_FINISHED,
        DELETE_NPC, SPAWN_NPC, SAVE_NPC_DATA, TRADE_EXECUTE,
        RUN_EVENT_GRAPH, RUN_SCRIPT,
        SAVE_SCRIPT_GRAPH, SAVE_SCRIPT_TEXT, SAVE_DIALOG_GRAPH, SAVE_EVENT_GRAPH, SAVE_STATE_MACHINE,
        DELETE_STATE_MACHINE, DUPLICATE_STATE_MACHINE, RENAME_STATE_MACHINE,
        REQUEST_CUTSCENES, REQUEST_DIALOG, REQUEST_EVENT_GRAPHS, REQUEST_NPC_LIST,
        REQUEST_SCRIPT_CONTENT, REQUEST_SCRIPT_GRAPHS, REQUEST_STATE_MACHINE, REQUEST_STATE_MACHINES,
        SCRIPT_BLOCK_SAVE, UPDATE_REGION_BLOCK,
        GIVE_QUEST
    }

    private final Type type;
    private final CompoundTag data;

    public ServerCommandPacket(Type type, CompoundTag data) {
        this.type = type;
        this.data = data != null ? data : new CompoundTag();
    }

    public static void encode(ServerCommandPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.type);
        buf.writeNbt(packet.data);
    }

    public static ServerCommandPacket decode(FriendlyByteBuf buf) {
        return new ServerCommandPacket(buf.readEnum(Type.class), buf.readNbt());
    }

    public static void handle(ServerCommandPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            switch (packet.type) {
                case PLAY_CUTSCENE -> handlePlayCutscene(player, packet.data);
                case DELETE_CUTSCENE -> handleDeleteCutscene(player, packet.data);
                case SAVE_CUTSCENE -> handleSaveCutscene(player, packet.data);
                case PAUSE_CUTSCENE -> ServerCutsceneHandler.pause(player);
                case RESUME_CUTSCENE -> ServerCutsceneHandler.resume(player, packet.data.getFloat("Speed"), packet.data.getInt("StartTick"));
                case STOP_CUTSCENE -> {
                    if (packet.data.getBoolean("ResetPosition")) ServerCutsceneHandler.stop(player, true);
                    else ServerCutsceneHandler.pause(player);
                }
                case CUTSCENE_FINISHED -> ServerCutsceneHandler.stop(player, false);
                case DELETE_NPC -> handleDeleteNPC(player, packet.data);
                case SPAWN_NPC -> handleSpawnNPC(player, packet.data);
                case SAVE_NPC_DATA -> handleSaveNPCData(player, packet.data);
                case TRADE_EXECUTE -> handleTradeExecute(player, packet.data);
                case RUN_EVENT_GRAPH -> handleRunEventGraph(player, packet.data);
                case RUN_SCRIPT -> handleRunScript(player, packet.data);
                case SAVE_SCRIPT_GRAPH -> handleSaveScriptGraph(player, packet.data);
                case SAVE_SCRIPT_TEXT -> handleSaveScriptText(player, packet.data);
                case SAVE_DIALOG_GRAPH -> handleSaveDialogGraph(player, packet.data);
                case SAVE_EVENT_GRAPH -> handleSaveEventGraph(player, packet.data);
                case SAVE_STATE_MACHINE -> handleSaveStateMachine(player, packet.data);
                case DELETE_STATE_MACHINE -> handleDeleteStateMachine(player, packet.data);
                case DUPLICATE_STATE_MACHINE -> handleDuplicateStateMachine(player, packet.data);
                case RENAME_STATE_MACHINE -> handleRenameStateMachine(player, packet.data);
                case REQUEST_CUTSCENES -> handleRequestCutscenes(player);
                case REQUEST_DIALOG -> handleRequestDialog(player, packet.data);
                case REQUEST_EVENT_GRAPHS -> handleRequestEventGraphs(player);
                case REQUEST_NPC_LIST -> handleRequestNPCList(player);
                case REQUEST_SCRIPT_CONTENT -> handleRequestScriptContent(player, packet.data);
                case REQUEST_SCRIPT_GRAPHS -> handleRequestScriptGraphs(player);
                case REQUEST_STATE_MACHINE -> handleRequestStateMachine(player, packet.data);
                case REQUEST_STATE_MACHINES -> handleRequestStateMachines(player);
                case SCRIPT_BLOCK_SAVE -> handleScriptBlockSave(player, packet.data);
                case UPDATE_REGION_BLOCK -> handleUpdateRegionBlock(player, packet.data);
                case GIVE_QUEST -> handleGiveQuest(player, packet.data);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handlePlayCutscene(ServerPlayer player, CompoundTag data) {
        String id = data.getString("Id");
        float speed = data.getFloat("Speed");
        int startTick = data.getInt("StartTick");
        CutsceneData cutscene = CutsceneManager.get(player.serverLevel(), id);
        if (cutscene == null) { IScriptMod.LOGGER.warn("Cutscene not found: {}", id); return; }
        ServerCutsceneHandler.play(player, cutscene, speed, startTick);
    }

    private static void handleDeleteCutscene(ServerPlayer player, CompoundTag data) {
        if (player.hasPermissions(2)) CutsceneManager.remove(player.serverLevel(), data.getString("Id"));
    }

    private static void handleSaveCutscene(ServerPlayer player, CompoundTag data) {
        if (!player.hasPermissions(2)) return;
        CutsceneData cutscene = new CutsceneData();
        cutscene.load(data.getCompound("Cutscene"));
        CutsceneManager.add(player.serverLevel(), cutscene);
    }

    private static void handleDeleteNPC(ServerPlayer player, CompoundTag data) {
        ServerLevel sl = player.server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (sl != null) NPCManager.delete(sl, data.getString("Id"));
    }

    private static void handleSpawnNPC(ServerPlayer player, CompoundTag data) {
        ServerLevel sl = player.serverLevel();
        String id = data.getString("Id");
        NPCData npcData = NPCManager.load(sl, id);
        if (npcData == null) { npcData = new NPCData(); npcData.setName(id); npcData.setHealth(20); npcData.setMaxHealth(20); }
        var npc = ModEntities.ISCRIPT_NPC.get().create(sl);
        if (npc == null) return;
        npc.moveTo(player.getX(), player.getY() + 0.1, player.getZ(), player.getYRot(), 0);
        npc.setYHeadRot(player.getYRot());
        npc.setYBodyRot(player.getYRot());
        npc.setNPCData(npcData);
        npc.setOwner(player);
        npc.setNoGravity(false);
        npc.setHealth(npc.getMaxHealth());
        npc.finalizeSpawn(sl, sl.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.COMMAND, null, null);
        sl.addFreshEntity(npc);
    }

    private static void handleSaveNPCData(ServerPlayer player, CompoundTag data) {
        int entityId = data.getInt("EntityId");
        NPCData npcData = new NPCData();
        npcData.load(data.getCompound("Data"));
        if (npcData.getHealth() <= 0) npcData.setHealth(Math.max(npcData.getMaxHealth(), 20.0f));
        if (npcData.getMaxHealth() <= 0) npcData.setMaxHealth(20.0f);
        NPCManager.save(player.serverLevel(), npcData.getId(), npcData);
        if (entityId >= 0) {
            for (ServerLevel sl : player.server.getAllLevels()) {
                var entity = sl.getEntity(entityId);
                if (entity instanceof IScriptNPCEntity npc) { npc.setNPCData(npcData); break; }
            }
        }
        List<NPCData> list = new ArrayList<>();
        for (String nid : NPCManager.listIds(player.serverLevel())) {
            NPCData d = NPCManager.load(player.serverLevel(), nid);
            if (d != null) list.add(d);
        }
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.NPC_LIST, SyncDataPacket.npcListToTag(list)), player);
    }

    private static void handleTradeExecute(ServerPlayer player, CompoundTag data) {
        int entityId = data.getInt("EntityId");
        int offerIndex = data.getInt("OfferIndex");
        if (player.level().getEntity(entityId) instanceof IScriptNPCEntity npc) {
            NPCTradeData tradeData = npc.getNPCData().getTradeData();
            if (offerIndex < 0 || offerIndex >= tradeData.getOffers().size()) return;
            NPCTradeData.TradeOffer offer = tradeData.getOffers().get(offerIndex);
            if (!offer.isAvailable()) return;
            ItemStack input = offer.getInput();
            boolean has = false;
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, input) && stack.getCount() >= input.getCount()) {
                    stack.shrink(input.getCount());
                    if (stack.isEmpty()) player.getInventory().items.set(i, ItemStack.EMPTY);
                    has = true;
                    break;
                }
            }
            if (has) {
                ItemStack output = offer.getOutput().copy();
                player.getInventory().add(output);
                offer.use();
            }
        }
    }

    private static void handleRunEventGraph(ServerPlayer player, CompoundTag data) {
        String graphId = data.getString("Id");
        if (graphId.isEmpty()) return;
        ServerLevel overworld = player.getServer().overworld();
        EventGraphData graph = EventGraphManager.get(overworld, graphId);
        if (graph == null) graph = com.iscript.iscript.data.EventSavedData.get(overworld).getGraph(graphId);
        if (graph == null) {
            player.sendSystemMessage(Component.literal("§c[EventGraph] Graph '" + graphId + "' not found"));
            return;
        }
        new GraphExecutor(graph, player, overworld).execute();
    }

    private static void handleRunScript(ServerPlayer player, CompoundTag data) {
        String scriptId = data.getString("Id");
        ServerLevel level = player.serverLevel();
        String script = ScriptFileManager.load(level, scriptId);
        if (script == null || script.trim().isEmpty()) { IScriptMod.LOGGER.warn("Script {} not found", scriptId); return; }
        ScriptEngine engine = ScriptEngine.getInstance();
        if (!engine.isAvailable()) { IScriptMod.LOGGER.warn("ScriptEngine not available"); return; }
        try { engine.execute(script, player, level); IScriptMod.LOGGER.info("Executed script: {}", scriptId); }
        catch (Exception e) { IScriptMod.LOGGER.error("Script error: {}", e.getMessage()); }
    }

    private static void handleSaveScriptGraph(ServerPlayer player, CompoundTag data) {
        ScriptGraphData graph = new ScriptGraphData();
        graph.load(data.getCompound("Graph"));
        ScriptGraphManager.add(player.serverLevel(), graph, "");
    }

    private static void handleSaveScriptText(ServerPlayer player, CompoundTag data) {
        String scriptId = data.getString("Id");
        String text = data.getString("Text");
        if (text == null) return;
        ServerLevel level = player.serverLevel();
        ScriptGraphData existing = ScriptGraphManager.get(level, scriptId);
        String name = existing != null ? existing.getName() : scriptId;
        ScriptGraphData parsed = parseScriptText(scriptId, name, text);
        ScriptGraphManager.add(level, parsed, text);
        IScriptMod.LOGGER.info("Script saved: {} ({} chars)", scriptId, text.length());
    }

    private static ScriptGraphData parseScriptText(String id, String name, String text) {
        ScriptGraphData graph = new ScriptGraphData();
        graph.setId(id);
        graph.setName(name);
        ScriptNodeData currentNode = null;
        int[] counter = {0};
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            if (line.endsWith("{")) {
                String[] parts = line.substring(0, line.length() - 1).trim().split("\\s+", 2);
                String currentType = parts[0];
                String currentNodeId = parts.length > 1 ? parts[1] : "node_" + (counter[0]++);
                currentNode = new ScriptNodeData();
                currentNode.setId(currentNodeId);
                try { currentNode.setType(ScriptNodeType.valueOf(currentType)); } catch (IllegalArgumentException e) { currentNode.setType(ScriptNodeType.SCRIPT_JS); }
            } else if (line.equals("}")) {
                if (currentNode != null) { graph.addNode(currentNode); currentNode = null; }
            } else if (currentNode != null && line.contains("=")) {
                int eq = line.indexOf("=");
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"")) value = value.substring(1, value.length() - 1);
                currentNode.setParam(key, value);
            }
        }
        if (currentNode != null) graph.addNode(currentNode);
        for (var node : graph.getNodes().values()) {
            if (node.getType() == ScriptNodeType.START) { graph.setStartNodeId(node.getId()); break; }
        }
        return graph;
    }

    private static void handleSaveDialogGraph(ServerPlayer player, CompoundTag data) {
        DialogGraphData graph = new DialogGraphData();
        graph.load(data.getCompound("Graph"));
        DialogGraphManager.add(player.serverLevel(), graph);
    }

    private static void handleSaveEventGraph(ServerPlayer player, CompoundTag data) {
        EventGraphData graph = new EventGraphData();
        graph.load(data.getCompound("Graph"));
        EventGraphManager.add(player.serverLevel(), graph);
        com.iscript.iscript.data.EventSavedData.get(player.serverLevel()).setGraph(graph.getId(), graph);
    }

    private static void handleSaveStateMachine(ServerPlayer player, CompoundTag data) {
        String machineId = data.getString("Id");
        String name = data.getString("Name");
        String entryNode = data.getString("EntryNode");
        String nodesJson = data.getString("NodesJson");
        StateMachineData smData = StateMachineData.get(player.serverLevel());
        StateMachine machine = smData.getMachine(machineId);
        boolean isNew = false;
        if (machine == null) { machine = new StateMachine(machineId, name); isNew = true; } else { machine.name = name; }
        machine.entryNode = entryNode.isEmpty() ? null : entryNode;
        if (!nodesJson.isEmpty()) {
            machine.nodes.clear();
            try {
                JsonArray arr = JsonParser.parseString(nodesJson).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    StateNode node = new StateNode();
                    node.id = obj.get("id").getAsString();
                    node.name = obj.has("name") ? obj.get("name").getAsString() : node.id;
                    node.color = obj.has("color") ? obj.get("color").getAsInt() : 0xFF4488AA;
                    node.posX = obj.has("posX") ? obj.get("posX").getAsInt() : 0;
                    node.posY = obj.has("posY") ? obj.get("posY").getAsInt() : 0;
                    if (obj.has("transitions")) {
                        JsonArray tarr = obj.getAsJsonArray("transitions");
                        for (JsonElement tel : tarr) {
                            JsonObject tobj = tel.getAsJsonObject();
                            StateTransition trans = new StateTransition();
                            trans.targetNode = tobj.get("targetNode").getAsString();
                            trans.auto = tobj.has("auto") && tobj.get("auto").getAsBoolean();
                            node.transitions.add(trans);
                        }
                    }
                    machine.nodes.put(node.id, node);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        smData.putMachine(machine);
        syncStateMachines(player);
    }

    private static void handleDeleteStateMachine(ServerPlayer player, CompoundTag data) {
        StateMachineData smData = StateMachineData.get(player.serverLevel());
        smData.removeMachine(data.getString("Id"));
        syncStateMachines(player);
    }

    private static void handleDuplicateStateMachine(ServerPlayer player, CompoundTag data) {
        String sourceId = data.getString("SourceId");
        String newId = data.getString("NewId");
        StateMachineData smData = StateMachineData.get(player.serverLevel());
        StateMachine source = smData.getMachine(sourceId);
        if (source == null) return;
        StateMachine copy = new StateMachine(newId, source.name + " (Copy)");
        copy.entryNode = source.entryNode;
        for (StateNode n : source.nodes.values()) {
            StateNode nc = new StateNode(n.id, n.name, n.color);
            nc.posX = n.posX + 20;
            nc.posY = n.posY + 20;
            for (StateTransition t : n.transitions) {
                StateTransition tc = new StateTransition(t.targetNode, t.auto);
                nc.transitions.add(tc);
            }
            copy.nodes.put(nc.id, nc);
        }
        smData.putMachine(copy);
        syncStateMachines(player);
    }

    private static void handleRenameStateMachine(ServerPlayer player, CompoundTag data) {
        String oldId = data.getString("OldId");
        String newId = data.getString("NewId");
        String newName = data.getString("NewName");
        StateMachineData smData = StateMachineData.get(player.serverLevel());
        smData.renameMachine(oldId, newId);
        StateMachine m = smData.getMachine(newId);
        if (m != null) m.name = newName;
        syncStateMachines(player);
    }

    private static void syncStateMachines(ServerPlayer player) {
        StateMachineData smData = StateMachineData.get(player.serverLevel());
        Map<String, String> cache = new HashMap<>();
        for (StateMachine m : smData.getMachines()) cache.put(m.id, m.name);
        IScriptNetwork.sendToAll(new SyncDataPacket(SyncDataPacket.Type.STATE_MACHINES, SyncDataPacket.stateMachinesToTag(cache)));
    }

    private static void handleRequestCutscenes(ServerPlayer player) {
        var cutscenes = CutsceneManager.getAll(player.serverLevel());
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.CUTSCENES, SyncDataPacket.cutscenesToTag(cutscenes)), player);
    }

    private static void handleRequestDialog(ServerPlayer player, CompoundTag data) {
        String dialogId = data.getString("Id");
        IScriptMod.LOGGER.info("RequestDialog: {}", dialogId);
        DialogData dialog;
        if (dialogId.contains(":")) {
            String[] parts = dialogId.split(":", 2);
            IScriptMod.LOGGER.info("Graph request: graph={}, node={}", parts[0], parts[1]);
            dialog = DialogGraphManager.convertToDialogData(player.serverLevel(), parts[0], parts[1]);
        } else {
            dialog = DialogManager.get(player.serverLevel(), dialogId);
        }
        if (dialog == null) { IScriptMod.LOGGER.warn("Dialog not found: {}", dialogId); return; }
        DialogData filtered = new DialogData();
        filtered.setId(dialog.getId());
        filtered.setTitle(dialog.getTitle());
        filtered.setText(dialog.getText());
        filtered.setPortrait(dialog.getPortrait());
        for (DialogData.DialogOption opt : dialog.getAvailableOptions(player)) filtered.getOptions().add(opt);
        IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG, OpenGuiPacket.dialogToTag(filtered)), player);
    }

    private static void handleRequestEventGraphs(ServerPlayer player) {
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.EVENT_GRAPHS,
                SyncDataPacket.eventGraphsToTag(EventGraphManager.getAll(player.serverLevel()))), player);
    }

    private static void handleRequestNPCList(ServerPlayer player) {
        List<NPCData> list = new ArrayList<>();
        ServerLevel sl = player.server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (sl != null) {
            for (String id : NPCManager.listIds(sl)) {
                NPCData d = NPCManager.load(sl, id);
                if (d != null) list.add(d);
            }
        }
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.NPC_LIST, SyncDataPacket.npcListToTag(list)), player);
    }

    private static void handleRequestScriptContent(ServerPlayer player, CompoundTag data) {
        String scriptId = data.getString("Id");
        ServerLevel level = player.serverLevel();
        ScriptGraphData graph = ScriptGraphManager.get(level, scriptId);
        String jsText = ScriptFileManager.loadScriptJs(level, scriptId);
        if (graph == null || !ScriptFileManager.scriptExists(level, scriptId)) {
            graph = new ScriptGraphData();
            graph.setId(scriptId);
            graph.setName(scriptId);
            ScriptNodeData start = new ScriptNodeData();
            start.setId("start");
            start.setType(ScriptNodeType.START);
            start.setX(100);
            start.setY(100);
            graph.addNode(start);
            ScriptGraphManager.add(level, graph, "");
            jsText = "";
        }
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.SCRIPT_CONTENT,
                SyncDataPacket.scriptContentToTag(scriptId, jsText, graph)), player);
    }

    private static void handleRequestScriptGraphs(ServerPlayer player) {
        IScriptMod.LOGGER.info("[SERVER] RequestScriptGraphs from {}", player.getName().getString());
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.SCRIPT_GRAPHS,
                SyncDataPacket.scriptGraphsToTag(ScriptGraphManager.getAll(player.serverLevel()))), player);
    }

    private static void handleRequestStateMachine(ServerPlayer player, CompoundTag data) {
        String machineId = data.getString("Id");
        StateMachineData smData = StateMachineData.get(player.serverLevel());
        StateMachine machine = smData.getMachine(machineId);
        if (machine == null) return;
        StringBuilder nodesJson = new StringBuilder();
        nodesJson.append("[");
        boolean first = true;
        for (StateNode n : machine.nodes.values()) {
            if (!first) nodesJson.append(",");
            first = false;
            nodesJson.append("{\"id\":\"").append(escape(n.id)).append("\",");
            nodesJson.append("\"name\":\"").append(escape(n.name)).append("\",");
            nodesJson.append("\"color\":").append(n.color).append(",");
            nodesJson.append("\"posX\":").append(n.posX).append(",");
            nodesJson.append("\"posY\":").append(n.posY).append(",");
            nodesJson.append("\"transitions\":[");
            boolean tfirst = true;
            for (StateTransition t : n.transitions) {
                if (!tfirst) nodesJson.append(",");
                tfirst = false;
                nodesJson.append("{\"targetNode\":\"").append(escape(t.targetNode)).append("\",\"auto\":").append(t.auto).append("}");
            }
            nodesJson.append("]}");
        }
        nodesJson.append("]");
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.STATE_MACHINE,
                SyncDataPacket.stateMachineToTag(machine.id, machine.name, machine.entryNode, nodesJson.toString())), player);
    }

    private static void handleRequestStateMachines(ServerPlayer player) {
        StateMachineData smData = StateMachineData.get(player.serverLevel());
        Map<String, String> cache = new HashMap<>();
        for (StateMachine m : smData.getMachines()) cache.put(m.id, m.name);
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.STATE_MACHINES,
                SyncDataPacket.stateMachinesToTag(cache)), player);
    }

    private static void handleScriptBlockSave(ServerPlayer player, CompoundTag data) {
        BlockPos pos = new BlockPos(data.getInt("X"), data.getInt("Y"), data.getInt("Z"));
        String label = data.getString("Label");
        String scriptId = data.getString("ScriptId");
        String script = data.getString("Script");
        BlockEntity be = player.serverLevel().getBlockEntity(pos);
        if (be instanceof ScriptBlockEntity scriptBE) {
            scriptBE.setLabel(label);
            scriptBE.setScriptId(scriptId);
            ScriptFileManager.save(player.serverLevel(), scriptId, script);
        }
    }

    private static void handleUpdateRegionBlock(ServerPlayer player, CompoundTag data) {
        BlockPos pos = new BlockPos(data.getInt("X"), data.getInt("Y"), data.getInt("Z"));
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof RegionBlockEntity rbe) {
            RegionData d = RegionData.fromNetworkTag(data.getCompound("RegionData"));
            rbe.setData(d);
        }
    }

    private static void handleGiveQuest(ServerPlayer player, CompoundTag data) {
        String questId = data.getString("QuestId");
        String targetName = data.getString("TargetName");
        if (targetName.isEmpty() || targetName.equals(player.getGameProfile().getName())) {
            QuestManager.startQuest(player.getServer().overworld(), player.getUUID(), questId);
        } else {
            ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(targetName);
            if (target != null) QuestManager.startQuest(player.getServer().overworld(), target.getUUID(), questId);
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    public static CompoundTag playCutsceneToTag(String id, float speed, int startTick) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        data.putFloat("Speed", speed);
        data.putInt("StartTick", startTick);
        return data;
    }

    public static CompoundTag deleteCutsceneToTag(String id) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        return data;
    }

    public static CompoundTag saveCutsceneToTag(CutsceneData cutscene) {
        CompoundTag data = new CompoundTag();
        CompoundTag t = new CompoundTag();
        cutscene.save(t);
        data.put("Cutscene", t);
        return data;
    }

    public static CompoundTag resumeCutsceneToTag(float speed, int startTick) {
        CompoundTag data = new CompoundTag();
        data.putFloat("Speed", speed);
        data.putInt("StartTick", startTick);
        return data;
    }

    public static CompoundTag stopCutsceneToTag(boolean resetPosition) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("ResetPosition", resetPosition);
        return data;
    }

    public static CompoundTag deleteNPCToTag(String id) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        return data;
    }

    public static CompoundTag spawnNPCToTag(String id) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        return data;
    }

    public static CompoundTag saveNPCToTag(int entityId, NPCData npcData) {
        CompoundTag data = new CompoundTag();
        data.putInt("EntityId", entityId);
        CompoundTag d = new CompoundTag();
        npcData.save(d);
        data.put("Data", d);
        return data;
    }

    public static CompoundTag tradeToTag(int entityId, int offerIndex) {
        CompoundTag data = new CompoundTag();
        data.putInt("EntityId", entityId);
        data.putInt("OfferIndex", offerIndex);
        return data;
    }

    public static CompoundTag runEventToTag(String id) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        return data;
    }

    public static CompoundTag runScriptToTag(String id) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        return data;
    }

    public static CompoundTag saveScriptGraphToTag(ScriptGraphData graph) {
        CompoundTag data = new CompoundTag();
        CompoundTag t = new CompoundTag();
        graph.save(t);
        data.put("Graph", t);
        return data;
    }

    public static CompoundTag saveScriptTextToTag(String id, String text) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        data.putString("Text", text);
        return data;
    }

    public static CompoundTag saveDialogGraphToTag(DialogGraphData graph) {
        CompoundTag data = new CompoundTag();
        CompoundTag t = new CompoundTag();
        graph.save(t);
        data.put("Graph", t);
        return data;
    }

    public static CompoundTag saveEventGraphToTag(EventGraphData graph) {
        CompoundTag data = new CompoundTag();
        CompoundTag t = new CompoundTag();
        graph.save(t);
        data.put("Graph", t);
        return data;
    }

    public static CompoundTag saveStateMachineToTag(String id, String name, String entryNode, String nodesJson) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        data.putString("Name", name);
        data.putString("EntryNode", entryNode != null ? entryNode : "");
        data.putString("NodesJson", nodesJson);
        return data;
    }

    public static CompoundTag deleteStateMachineToTag(String id) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        return data;
    }

    public static CompoundTag dupStateMachineToTag(String sourceId, String newId) {
        CompoundTag data = new CompoundTag();
        data.putString("SourceId", sourceId);
        data.putString("NewId", newId);
        return data;
    }

    public static CompoundTag renameStateMachineToTag(String oldId, String newId, String newName) {
        CompoundTag data = new CompoundTag();
        data.putString("OldId", oldId);
        data.putString("NewId", newId);
        data.putString("NewName", newName);
        return data;
    }

    public static CompoundTag requestDialogToTag(String id) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        return data;
    }

    public static CompoundTag requestScriptToTag(String id) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        return data;
    }

    public static CompoundTag requestStateMachineToTag(String id) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
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

    public static CompoundTag updateRegionToTag(BlockPos pos, RegionData regionData) {
        CompoundTag data = new CompoundTag();
        data.putInt("X", pos.getX());
        data.putInt("Y", pos.getY());
        data.putInt("Z", pos.getZ());
        data.put("RegionData", regionData.toNetworkTag());
        return data;
    }

    public static CompoundTag giveQuestToTag(String questId, String targetName) {
        CompoundTag data = new CompoundTag();
        data.putString("QuestId", questId);
        data.putString("TargetName", targetName != null ? targetName : "");
        return data;
    }

    private static class GraphExecutor {
        private final EventGraphData graph;
        private final ServerPlayer player;
        private final ServerLevel level;
        private final Set<String> visited;
        private final Random random;

        GraphExecutor(EventGraphData graph, ServerPlayer player, ServerLevel level) {
            this.graph = graph;
            this.player = player;
            this.level = level;
            this.visited = new HashSet<>();
            this.random = new Random();
        }

        void execute() {
            String startId = graph.getStartNodeId();
            if (startId == null || startId.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] No start node set"));
                return;
            }
            ScriptNodeData startNode = graph.getNode(startId);
            if (startNode == null) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Start node not found"));
                return;
            }
            executeNode(startNode, 0);
        }

        private void executeNode(ScriptNodeData node, int depth) {
            if (node == null) return;
            if (depth > 1000) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Max depth exceeded at node '" + node.getId() + "'"));
                return;
            }
            if (visited.contains(node.getId())) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Cycle detected at node '" + node.getId() + "'"));
                return;
            }
            visited.add(node.getId());

            switch (node.getType()) {
                case START, TRIGGER -> executeStartTrigger(node);
                case SCRIPT_JS -> executeScript(node);
                case IF -> executeIf(node, depth);
                case DELAY -> executeDelay(node, depth);
                case RANDOM -> executeRandom(node, depth);
                case LOOP -> executeLoop(node, depth);
                case STOP -> {
                    player.sendSystemMessage(Component.literal("§7[EventGraph] STOP reached"));
                    return;
                }
                default -> player.sendSystemMessage(Component.literal("§c[EventGraph] Unknown node type: " + node.getType()));
            }

            if (node.getType() != ScriptNodeType.IF && node.getType() != ScriptNodeType.RANDOM &&
                    node.getType() != ScriptNodeType.LOOP && node.getType() != ScriptNodeType.STOP) {
                followConnections(node, 0, depth);
            }
        }

        private void executeStartTrigger(ScriptNodeData node) {
            String eventTypeStr = node.getParam("eventType");
            if (eventTypeStr == null || eventTypeStr.isEmpty()) return;
            try {
                EventType type = EventType.valueOf(eventTypeStr);
                EventManager.trigger(type, player, level);
            } catch (IllegalArgumentException e) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Invalid eventType: " + eventTypeStr));
            }
        }

        private void executeScript(ScriptNodeData node) {
            String script = node.getParam("script");
            if (script == null || script.isEmpty()) return;
            ScriptEngine engine = ScriptEngine.getInstance();
            if (!engine.isAvailable()) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] ScriptEngine not available"));
                return;
            }
            try {
                engine.execute(script, player, level);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                player.sendSystemMessage(Component.literal("§c[Script Error] " + msg));
            }
        }

        private void executeIf(ScriptNodeData node, int depth) {
            String condition = node.getParam("condition");
            boolean result = false;
            if (condition != null && !condition.isEmpty()) result = evaluateCondition(condition);
            followConnections(node, result ? 0 : 1, depth);
        }

        private void executeDelay(ScriptNodeData node, int depth) {
            followConnections(node, 0, depth);
        }

        private void executeRandom(ScriptNodeData node, int depth) {
            String branchesStr = node.getParam("branches");
            int branches = 2;
            try {
                if (branchesStr != null) branches = Math.max(2, Integer.parseInt(branchesStr));
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Invalid branches: " + branchesStr));
            }
            int slot = random.nextInt(branches);
            followConnections(node, slot, depth);
        }

        private void executeLoop(ScriptNodeData node, int depth) {
            String countStr = node.getParam("count");
            int count = 3;
            try {
                if (countStr != null) count = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.literal("§c[EventGraph] Invalid count: " + countStr));
            }
            for (int i = 0; i < count; i++) followConnections(node, 0, depth);
            followConnections(node, 1, depth);
        }

        private void followConnections(ScriptNodeData node, int slot, int depth) {
            List<ScriptNodeConnection> connections = node.getConnections();
            for (ScriptNodeConnection conn : connections) {
                if (conn.getSourceSlot() == slot) {
                    ScriptNodeData target = graph.getNode(conn.getTargetNodeId());
                    if (target != null) executeNode(target, depth + 1);
                }
            }
        }

        private boolean evaluateCondition(String condition) {
            if (condition == null || condition.isEmpty()) return false;
            String lower = condition.trim().toLowerCase();
            if (lower.equals("true")) return true;
            if (lower.equals("false")) return false;
            ScriptEngine engine = ScriptEngine.getInstance();
            if (!engine.isAvailable()) return false;
            try {
                Object result = engine.execute(condition, player, level);
                if (result instanceof Boolean) return (Boolean) result;
                if (result instanceof Number) return ((Number) result).doubleValue() != 0;
                if (result instanceof String) return Boolean.parseBoolean((String) result);
                return result != null;
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                player.sendSystemMessage(Component.literal("§c[Condition Error] " + msg));
                return false;
            }
        }
    }
}