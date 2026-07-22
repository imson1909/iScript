package com.iscript.iscript.network.packet;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.block.ScriptBlockEntity;
import com.iscript.iscript.blockentities.RegionBlockEntity;
import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.data.Graph;
import com.iscript.iscript.data.ModData;
import com.iscript.iscript.data.Node;
import com.iscript.iscript.data.cutscene.CutsceneData;
import com.iscript.iscript.data.cutscene.CutscenePlayer;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.npc.NPCTradeData;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.entity.IScriptNPCEntity;
import com.iscript.iscript.event.EventManager;
import com.iscript.iscript.event.EventType;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.registry.ModEntities;
import com.iscript.iscript.script.ScriptEngine;
import com.iscript.iscript.script.ScriptFileManager;
import com.iscript.iscript.script.ScriptGraphManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import com.iscript.iscript.data.GlobalStates;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public class ServerCommandPacket {
    public enum Type {
        PLAY_CUTSCENE, DELETE_CUTSCENE, SAVE_CUTSCENE,
        PAUSE_CUTSCENE, RESUME_CUTSCENE, STOP_CUTSCENE, CUTSCENE_FINISHED,
        DELETE_NPC, SPAWN_NPC, SAVE_NPC_DATA, TRADE_EXECUTE,
        RUN_EVENT_GRAPH, RUN_SCRIPT,
        SAVE_SCRIPT_GRAPH, DELETE_SCRIPT_GRAPH, SAVE_SCRIPT_TEXT,
        SAVE_DIALOG_GRAPH, SAVE_EVENT_GRAPH, SAVE_STATE_GRAPH,
        REQUEST_CUTSCENES, REQUEST_DIALOG, REQUEST_NPC_LIST,
        REQUEST_SCRIPT_CONTENT, REQUEST_SCRIPT_GRAPHS,
        REQUEST_EVENT_GRAPHS, REQUEST_STATE_GRAPHS,
        DELETE_STATE_GRAPH,
        SCRIPT_BLOCK_SAVE, UPDATE_REGION_BLOCK,
        GIVE_QUEST, REQUEST_STATES, SAVE_STATES,
        REQUEST_DASHBOARD_LIST, SAVE_DASHBOARD_ITEM, DELETE_DASHBOARD_ITEM
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
                case PAUSE_CUTSCENE -> CutscenePlayer.stop(player);
                case RESUME_CUTSCENE -> CutscenePlayer.play(player, DataAccess.cutscene(packet.data.getString("CutsceneId")));
                case STOP_CUTSCENE -> CutscenePlayer.stop(player);
                case CUTSCENE_FINISHED -> CutscenePlayer.stop(player);
                case DELETE_NPC -> handleDeleteNPC(player, packet.data);
                case SPAWN_NPC -> handleSpawnNPC(player, packet.data);
                case SAVE_NPC_DATA -> handleSaveNPCData(player, packet.data);
                case TRADE_EXECUTE -> handleTradeExecute(player, packet.data);
                case RUN_EVENT_GRAPH -> handleRunEventGraph(player, packet.data);
                case RUN_SCRIPT -> handleRunScript(player, packet.data);
                case SAVE_SCRIPT_GRAPH -> handleSaveScriptGraph(player, packet.data);
                case DELETE_SCRIPT_GRAPH -> {
                    if (player.hasPermissions(2)) ScriptGraphManager.remove(player.serverLevel(), packet.data.getString("Id"));
                }
                case SAVE_SCRIPT_TEXT -> handleSaveScriptText(player, packet.data);
                case SAVE_DIALOG_GRAPH -> handleSaveDialogGraph(player, packet.data);
                case SAVE_EVENT_GRAPH -> handleSaveEventGraph(player, packet.data);
                case SAVE_STATE_GRAPH -> handleSaveStateGraph(player, packet.data);
                case DELETE_STATE_GRAPH -> {
                    if (player.hasPermissions(2)) DataAccess.removeState(packet.data.getString("Id"));
                }
                case REQUEST_CUTSCENES -> handleRequestCutscenes(player);
                case REQUEST_DIALOG -> handleRequestDialog(player, packet.data);
                case REQUEST_NPC_LIST -> handleRequestNPCList(player);
                case REQUEST_SCRIPT_CONTENT -> handleRequestScriptContent(player, packet.data);
                case REQUEST_SCRIPT_GRAPHS -> handleRequestScriptGraphs(player);
                case REQUEST_EVENT_GRAPHS -> handleRequestEventGraphs(player);
                case REQUEST_STATE_GRAPHS -> handleRequestStateGraphs(player);
                case SCRIPT_BLOCK_SAVE -> handleScriptBlockSave(player, packet.data);
                case UPDATE_REGION_BLOCK -> handleUpdateRegionBlock(player, packet.data);
                case GIVE_QUEST -> handleGiveQuest(player, packet.data);
                case REQUEST_STATES -> handleRequestStates(player);
                case SAVE_STATES -> handleSaveStates(player, packet.data);
                case REQUEST_DASHBOARD_LIST -> handleRequestDashboardList(player, packet.data);
                case SAVE_DASHBOARD_ITEM -> handleSaveDashboardItem(player, packet.data);
                case DELETE_DASHBOARD_ITEM -> handleDeleteDashboardItem(player, packet.data);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handlePlayCutscene(ServerPlayer player, CompoundTag data) {
        String id = data.getString("Id");
        CutsceneData cutscene = DataAccess.cutscene(id);
        if (cutscene == null) {
            IScriptMod.LOGGER.warn("Cutscene not found: {}", id);
            return;
        }
        CutscenePlayer.play(player, cutscene);
    }

    private static void handleDeleteCutscene(ServerPlayer player, CompoundTag data) {
        if (player.hasPermissions(2)) DataAccess.removeCutscene(data.getString("Id"));
    }

    private static void handleSaveCutscene(ServerPlayer player, CompoundTag data) {
        if (!player.hasPermissions(2)) return;
        CutsceneData cutscene = new CutsceneData();
        cutscene.fromJson(JsonParser.parseString(data.getString("json")).getAsJsonObject());
        DataAccess.putCutscene(cutscene);
    }

    private static void handleDeleteNPC(ServerPlayer player, CompoundTag data) {
        DataAccess.removeNpc(data.getString("Id"));
    }

    private static void handleSpawnNPC(ServerPlayer player, CompoundTag data) {
        ServerLevel sl = player.serverLevel();
        String id = data.getString("Id");
        NPCData npcData = DataAccess.npc(id);
        if (npcData == null) {
            npcData = new NPCData();
            npcData.setName(id);
        }
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
        npcData.fromJson(JsonParser.parseString(data.getString("json")).getAsJsonObject());
        DataAccess.putNpc(npcData);
        if (entityId >= 0) {
            for (ServerLevel sl : player.server.getAllLevels()) {
                var entity = sl.getEntity(entityId);
                if (entity instanceof IScriptNPCEntity npc) {
                    npc.setNPCData(npcData);
                    break;
                }
            }
        }
        List<NPCData> list = new ArrayList<>(DataAccess.npcs().values());
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
        Graph graph = DataAccess.event(graphId);
        if (graph == null) {
            player.sendSystemMessage(Component.translatable("iscript.error.graph_not_found", graphId));
            return;
        }
        new GraphExecutor(graph, player, overworld).execute();
    }

    private static void handleRunScript(ServerPlayer player, CompoundTag data) {
        String scriptId = data.getString("Id");
        ServerLevel level = player.serverLevel();
        String script = ScriptFileManager.load(level, scriptId);
        if (script == null || script.trim().isEmpty()) {
            IScriptMod.LOGGER.warn("Script {} not found", scriptId);
            return;
        }
        ScriptEngine engine = ScriptEngine.getInstance();
        if (!engine.isAvailable()) {
            IScriptMod.LOGGER.warn("ScriptEngine not available");
            return;
        }
        try {
            engine.execute(scriptId, script, player, level);
            IScriptMod.LOGGER.info("Executed script: {}", scriptId);
        } catch (Exception e) {
            IScriptMod.LOGGER.error("Script error: {}", e.getMessage());
        }
    }

    private static void handleSaveScriptGraph(ServerPlayer player, CompoundTag data) {
        Graph graph = new Graph(ScriptNodeType.class);
        graph.fromJson(JsonParser.parseString(data.getString("json")).getAsJsonObject());
        ScriptGraphManager.add(player.serverLevel(), graph, "");
    }

    private static void handleSaveScriptText(ServerPlayer player, CompoundTag data) {
        String scriptId = data.getString("Id");
        String text = data.getString("Text");
        if (text == null) return;
        ServerLevel level = player.serverLevel();
        Graph existing = ScriptGraphManager.get(level, scriptId);
        String name = existing != null ? existing.getName() : scriptId;
        Graph parsed = parseScriptText(scriptId, name, text);
        ScriptGraphManager.add(level, parsed, text);
        IScriptMod.LOGGER.info("Script saved: {} ({} chars)", scriptId, text.length());
    }

    private static Graph parseScriptText(String id, String name, String text) {
        Graph graph = new Graph(ScriptNodeType.class);
        graph.setId(id);
        graph.setName(name);
        Node currentNode = null;
        int[] counter = {0};
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            if (line.endsWith("{")) {
                String[] parts = line.substring(0, line.length() - 1).trim().split("\s+", 2);
                String currentType = parts[0];
                String currentNodeId = parts.length > 1 ? parts[1] : "node_" + (counter[0]++);
                currentNode = new Node(ScriptNodeType.class);
                currentNode.setId(currentNodeId);
                try {
                    currentNode.setType(ScriptNodeType.valueOf(currentType).name());
                } catch (IllegalArgumentException e) {
                    currentNode.setType(ScriptNodeType.SCRIPT_JS.name());
                }
            } else if (line.equals("}")) {
                if (currentNode != null) {
                    graph.addNode(currentNode);
                    currentNode = null;
                }
            } else if (currentNode != null && line.contains("=")) {
                int eq = line.indexOf("=");
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (value.startsWith("\"\"") && value.endsWith("\"\"")) value = value.substring(1, value.length() - 1);
                currentNode.setParam(key, value);
            }
        }
        if (currentNode != null) graph.addNode(currentNode);
        for (Node node : graph.getNodes().values()) {
            if (node.getType().equals(ScriptNodeType.START.name())) {
                graph.setStartNodeId(node.getId());
                break;
            }
        }
        return graph;
    }

    private static void handleSaveDialogGraph(ServerPlayer player, CompoundTag data) {
        Graph graph = new Graph(null);
        graph.fromJson(JsonParser.parseString(data.getString("json")).getAsJsonObject());
        DataAccess.putDialogGraph(graph);
    }

    private static void handleSaveEventGraph(ServerPlayer player, CompoundTag data) {
        Graph graph = new Graph(ScriptNodeType.class);
        graph.fromJson(JsonParser.parseString(data.getString("json")).getAsJsonObject());
        DataAccess.putEvent(graph);
    }

    private static void handleSaveStateGraph(ServerPlayer player, CompoundTag data) {
        Graph graph = new Graph(null);
        graph.fromJson(JsonParser.parseString(data.getString("json")).getAsJsonObject());
        DataAccess.putState(graph);
    }

    private static void handleRequestCutscenes(ServerPlayer player) {
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.CUTSCENES, SyncDataPacket.cutscenesToTag(DataAccess.cutscenes())), player);
    }

    private static void handleRequestDialog(ServerPlayer player, CompoundTag data) {
        String dialogId = data.getString("Id");
        IScriptMod.LOGGER.info("RequestDialog: {}", dialogId);
        DialogData dialog = DataAccess.dialog(dialogId);
        if (dialog == null) {
            IScriptMod.LOGGER.warn("Dialog not found: {}", dialogId);
            return;
        }
        DialogData filtered = new DialogData();
        filtered.setId(dialog.getId());
        filtered.setTitle(dialog.getTitle());
        filtered.setText(dialog.getText());
        filtered.setPortrait(dialog.getPortrait());
        for (DialogData.DialogOption opt : dialog.getAvailableOptions(player)) filtered.getOptions().add(opt);
        IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG, OpenGuiPacket.dialogToTag(filtered)), player);
    }

    private static void handleRequestNPCList(ServerPlayer player) {
        List<NPCData> list = new ArrayList<>(DataAccess.npcs().values());
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.NPC_LIST, SyncDataPacket.npcListToTag(list)), player);
    }

    private static void handleRequestScriptContent(ServerPlayer player, CompoundTag data) {
        String scriptId = data.getString("Id");
        ServerLevel level = player.serverLevel();
        Graph graph = ScriptGraphManager.get(level, scriptId);
        String jsText = ScriptFileManager.loadScriptJs(level, scriptId);
        if (graph == null || !ScriptFileManager.scriptExists(level, scriptId)) {
            graph = new Graph(ScriptNodeType.class);
            graph.setId(scriptId);
            graph.setName(scriptId);
            Node start = new Node(ScriptNodeType.class);
            start.setId("start");
            start.setType(ScriptNodeType.START.name());
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

    private static void handleRequestEventGraphs(ServerPlayer player) {
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.EVENT_GRAPHS,
                SyncDataPacket.eventGraphsToTag(DataAccess.events())), player);
    }

    private static void handleRequestStateGraphs(ServerPlayer player) {
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.STATE_GRAPHS,
                SyncDataPacket.stateGraphsToTag(DataAccess.states())), player);
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
            RegionData d = new RegionData();
            d.fromJson(JsonParser.parseString(data.getString("json")).getAsJsonObject());
            rbe.setData(d);
        }
    }

    private static void handleGiveQuest(ServerPlayer player, CompoundTag data) {
        String questId = data.getString("QuestId");
        String targetName = data.getString("TargetName");
        if (targetName.isEmpty() || targetName.equals(player.getGameProfile().getName())) {
            DataAccess.startQuest(player.getServer().overworld(), player.getUUID(), questId);
        } else {
            ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(targetName);
            if (target != null) DataAccess.startQuest(player.getServer().overworld(), target.getUUID(), questId);
        }
    }

    private static void handleRequestStates(ServerPlayer player) {
        CompoundTag data = new CompoundTag();
        data.put("states", GlobalStates.get().serialize());
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.STATES, data), player);
    }

    private static void handleSaveStates(ServerPlayer player, CompoundTag data) {
        if (!player.hasPermissions(2)) return;
        GlobalStates.get().deserialize(data.getCompound("states"));
        GlobalStates.save();
        ModData.setDirty();
    }

    private static ListTag buildDashboardList(String category, ServerLevel level) {
        ListTag list = new ListTag();
        switch (category) {
            case "dialogs" -> {
                for (var e : DataAccess.dialogs().entrySet()) {
                    CompoundTag t = new CompoundTag();
                    t.putString("Id", e.getKey());
                    t.putString("Title", e.getValue().getTitle().isEmpty() ? e.getKey() : e.getValue().getTitle());
                    list.add(t);
                }
            }
            case "quests" -> {
                for (var e : DataAccess.quests().entrySet()) {
                    CompoundTag t = new CompoundTag();
                    t.putString("Id", e.getKey());
                    t.putString("Title", e.getValue().getTitle().isEmpty() ? e.getKey() : e.getValue().getTitle());
                    list.add(t);
                }
            }
            case "events" -> {
                for (var e : DataAccess.events().entrySet()) {
                    CompoundTag t = new CompoundTag();
                    t.putString("Id", e.getKey());
                    t.putString("Title", e.getValue().getName().isEmpty() ? e.getKey() : e.getValue().getName());
                    list.add(t);
                }
            }
            case "states" -> {
                for (var e : DataAccess.states().entrySet()) {
                    CompoundTag t = new CompoundTag();
                    t.putString("Id", e.getKey());
                    t.putString("Title", e.getValue().getName().isEmpty() ? e.getKey() : e.getValue().getName());
                    list.add(t);
                }
            }
            case "regions" -> {
                for (var e : DataAccess.regions().entrySet()) {
                    CompoundTag t = new CompoundTag();
                    t.putString("Id", e.getKey());
                    t.putString("Title", e.getValue().toJson().has("name") ? e.getValue().toJson().get("name").getAsString() : e.getKey());
                    list.add(t);
                }
            }
            case "scripts" -> {
                for (String id : ScriptFileManager.listScriptIds(level)) {
                    CompoundTag t = new CompoundTag();
                    t.putString("Id", id);
                    t.putString("Title", id);
                    list.add(t);
                }
            }
            case "npcs" -> {
                for (var e : DataAccess.npcs().entrySet()) {
                    CompoundTag t = new CompoundTag();
                    t.putString("Id", e.getKey());
                    t.putString("Title", e.getValue().toJson().has("name") ? e.getValue().toJson().get("name").getAsString() : e.getKey());
                    list.add(t);
                }
            }
            case "cutscenes" -> {
                for (var e : DataAccess.cutscenes().entrySet()) {
                    CompoundTag t = new CompoundTag();
                    t.putString("Id", e.getKey());
                    t.putString("Title", e.getValue().getName().isEmpty() ? e.getKey() : e.getValue().getName());
                    list.add(t);
                }
            }
        }
        return list;
    }

    private static void handleRequestDashboardList(ServerPlayer player, CompoundTag data) {
        String category = data.getString("Category");
        CompoundTag resp = new CompoundTag();
        resp.putString("Category", category);
        resp.put("Items", buildDashboardList(category, player.serverLevel()));
        IScriptNetwork.sendToPlayer(new SyncDataPacket(SyncDataPacket.Type.DASHBOARD_LIST, resp), player);
    }

    private static void handleSaveDashboardItem(ServerPlayer player, CompoundTag data) {
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("iscript.error.no_permission"));
            return;
        }
        String category = data.getString("Category");
        String id = data.getString("Id");
        String jsonStr = data.getString("Json");
        if (id.isEmpty() || jsonStr.isEmpty()) {
            player.sendSystemMessage(Component.translatable("iscript.error.invalid_data"));
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
            switch (category) {
                case "dialogs" -> {
                    DialogData d = new DialogData();
                    d.fromJson(json);
                    DataAccess.putDialog(d);
                }
                case "quests" -> {
                    QuestData q = new QuestData();
                    q.fromJson(json);
                    DataAccess.putQuest(q);
                }
                case "events" -> {
                    Graph g = new Graph(ScriptNodeType.class);
                    g.fromJson(json);
                    DataAccess.putEvent(g);
                }
                case "states" -> {
                    Graph g = new Graph(null);
                    g.fromJson(json);
                    DataAccess.putState(g);
                }
                case "regions" -> {
                    RegionData r = new RegionData();
                    r.fromJson(json);
                    DataAccess.putRegion(r);
                }
                case "cutscenes" -> {
                    CutsceneData c = new CutsceneData();
                    c.fromJson(json);
                    DataAccess.putCutscene(c);
                }
                case "scripts" -> {
                    Graph g = new Graph(ScriptNodeType.class);
                    g.fromJson(json);
                    ScriptGraphManager.add(player.serverLevel(), g, "");
                }
                default -> {
                    player.sendSystemMessage(Component.translatable("iscript.error.invalid_data"));
                    return;
                }
            }
            ModData.setDirty();
            broadcastDashboardUpdate(category, player);
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("iscript.error.save_failed", e.getMessage()));
        }
    }

    private static void handleDeleteDashboardItem(ServerPlayer player, CompoundTag data) {
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("iscript.error.no_permission"));
            return;
        }
        String category = data.getString("Category");
        String id = data.getString("Id");
        if (id.isEmpty()) {
            player.sendSystemMessage(Component.translatable("iscript.error.invalid_data"));
            return;
        }
        try {
            switch (category) {
                case "dialogs" -> DataAccess.removeDialog(id);
                case "quests" -> DataAccess.removeQuest(id);
                case "events" -> DataAccess.removeEvent(id);
                case "states" -> DataAccess.removeState(id);
                case "regions" -> DataAccess.removeRegion(id);
                case "cutscenes" -> DataAccess.removeCutscene(id);
                case "scripts" -> ScriptGraphManager.remove(player.serverLevel(), id);
                case "npcs" -> DataAccess.removeNpc(id);
                default -> {
                    player.sendSystemMessage(Component.translatable("iscript.error.invalid_data"));
                    return;
                }
            }
            ModData.setDirty();
            broadcastDashboardUpdate(category, player);
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("iscript.error.delete_failed", e.getMessage()));
        }
    }

    private static void broadcastDashboardUpdate(String category, ServerPlayer exclude) {
        CompoundTag data = new CompoundTag();
        data.putString("Category", category);
        data.put("Items", buildDashboardList(category, exclude.serverLevel()));
        SyncDataPacket packet = new SyncDataPacket(SyncDataPacket.Type.DASHBOARD_LIST, data);
        for (ServerPlayer p : exclude.server.getPlayerList().getPlayers()) {
            if (p != exclude) IScriptNetwork.sendToPlayer(packet, p);
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"\"", "\\\"").replace("\n", "\\n").replace("\r", "");
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

    public static CompoundTag deleteScriptGraphToTag(String id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        return tag;
    }

    public static CompoundTag saveStateGraphToTag(Graph graph) {
        CompoundTag data = new CompoundTag();
        data.putString("json", graph.toJson().toString());
        return data;
    }

    public static CompoundTag deleteStateGraphToTag(String id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        return tag;
    }

    public static CompoundTag saveCutsceneToTag(CutsceneData cutscene) {
        CompoundTag data = new CompoundTag();
        data.putString("json", cutscene.toJson().toString());
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
        data.putString("json", npcData.toJson().toString());
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

    public static CompoundTag saveScriptGraphToTag(Graph graph) {
        CompoundTag data = new CompoundTag();
        data.putString("json", graph.toJson().toString());
        return data;
    }

    public static CompoundTag saveScriptTextToTag(String id, String text) {
        CompoundTag data = new CompoundTag();
        data.putString("Id", id);
        data.putString("Text", text);
        return data;
    }

    public static CompoundTag saveDialogGraphToTag(Graph graph) {
        CompoundTag data = new CompoundTag();
        data.putString("json", graph.toJson().toString());
        return data;
    }

    public static CompoundTag saveEventGraphToTag(Graph graph) {
        CompoundTag data = new CompoundTag();
        data.putString("json", graph.toJson().toString());
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
        data.putString("json", regionData.toJson().toString());
        return data;
    }

    public static CompoundTag giveQuestToTag(String questId, String targetName) {
        CompoundTag data = new CompoundTag();
        data.putString("QuestId", questId);
        data.putString("TargetName", targetName != null ? targetName : "");
        return data;
    }

    public static CompoundTag requestDashboardListToTag(String category) {
        CompoundTag data = new CompoundTag();
        data.putString("Category", category);
        return data;
    }

    public static CompoundTag saveDashboardItemToTag(String category, String id, String json) {
        CompoundTag data = new CompoundTag();
        data.putString("Category", category);
        data.putString("Id", id);
        data.putString("Json", json);
        return data;
    }

    public static CompoundTag deleteDashboardItemToTag(String category, String id) {
        CompoundTag data = new CompoundTag();
        data.putString("Category", category);
        data.putString("Id", id);
        return data;
    }

    private static class GraphExecutor {
        private final Graph graph;
        private final ServerPlayer player;
        private final ServerLevel level;
        private final Set<String> visited;
        private final Random random;
        private static final int MAX_DEPTH = 1000;

        GraphExecutor(Graph graph, ServerPlayer player, ServerLevel level) {
            this.graph = graph;
            this.player = player;
            this.level = level;
            this.visited = new HashSet<>();
            this.random = new Random();
        }

        void execute() {
            String startId = graph.getStartNodeId();
            if (startId == null || startId.isEmpty()) {
                player.sendSystemMessage(Component.translatable("iscript.error.no_start_node"));
                return;
            }
            Node startNode = graph.getNode(startId);
            if (startNode == null) {
                player.sendSystemMessage(Component.translatable("iscript.error.start_node_not_found"));
                return;
            }

            ArrayDeque<StackEntry> stack = new ArrayDeque<>();
            stack.push(new StackEntry(startNode, 0));

            while (!stack.isEmpty()) {
                StackEntry entry = stack.pop();
                Node node = entry.node;
                int depth = entry.depth;

                if (node == null) continue;
                if (depth > MAX_DEPTH) {
                    player.sendSystemMessage(Component.translatable("iscript.error.max_depth", node.getId()));
                    continue;
                }
                if (visited.contains(node.getId())) {
                    player.sendSystemMessage(Component.translatable("iscript.error.cycle_detected", node.getId()));
                    continue;
                }
                visited.add(node.getId());

                ScriptNodeType type;
                try {
                    type = ScriptNodeType.valueOf(node.getType());
                } catch (Exception e) {
                    type = ScriptNodeType.STOP;
                }

                switch (type) {
                    case START, TRIGGER -> executeStartTrigger(node);
                    case SCRIPT_JS -> executeScript(node);
                    case IF -> pushIfBranches(node, depth, stack);
                    case DELAY -> pushConnections(node, 0, depth, stack);
                    case RANDOM -> pushRandomBranch(node, depth, stack);
                    case LOOP -> pushLoopBranches(node, depth, stack);
                    case STOP -> {
                        player.sendSystemMessage(Component.translatable("iscript.info.stop_reached"));
                        continue;
                    }
                    default -> player.sendSystemMessage(Component.translatable("iscript.error.unknown_node_type", node.getType()));
                }

                if (type != ScriptNodeType.IF && type != ScriptNodeType.RANDOM &&
                        type != ScriptNodeType.LOOP && type != ScriptNodeType.STOP) {
                    pushConnections(node, 0, depth, stack);
                }
            }
        }

        private void pushConnections(Node node, int slot, int depth, ArrayDeque<StackEntry> stack) {
            List<Node.Connection> connections = node.getConnections();
            for (Node.Connection conn : connections) {
                if (conn.getSourceSlot() == slot) {
                    Node target = graph.getNode(conn.getTarget());
                    if (target != null) stack.push(new StackEntry(target, depth + 1));
                }
            }
        }

        private void pushIfBranches(Node node, int depth, ArrayDeque<StackEntry> stack) {
            String condition = node.getParam("condition");
            boolean result = false;
            if (condition != null && !condition.isEmpty()) result = evaluateCondition(condition);
            pushConnections(node, result ? 0 : 1, depth, stack);
        }

        private void pushRandomBranch(Node node, int depth, ArrayDeque<StackEntry> stack) {
            String branchesStr = node.getParam("branches");
            int branches = 2;
            try {
                if (branchesStr != null) branches = Math.max(2, Integer.parseInt(branchesStr));
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.translatable("iscript.error.invalid_branches", branchesStr));
            }
            int slot = random.nextInt(branches);
            pushConnections(node, slot, depth, stack);
        }

        private void pushLoopBranches(Node node, int depth, ArrayDeque<StackEntry> stack) {
            String countStr = node.getParam("count");
            int count = 3;
            try {
                if (countStr != null) count = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.translatable("iscript.error.invalid_count", countStr));
            }
            for (int i = 0; i < count; i++) {
                pushConnections(node, 0, depth, stack);
            }
            pushConnections(node, 1, depth, stack);
        }

        private void executeStartTrigger(Node node) {
            String eventTypeStr = node.getParam("eventType");
            if (eventTypeStr == null || eventTypeStr.isEmpty()) return;
            try {
                EventType type = EventType.valueOf(eventTypeStr);
                EventManager.trigger(type, player, level);
            } catch (IllegalArgumentException e) {
                player.sendSystemMessage(Component.translatable("iscript.error.invalid_event_type", eventTypeStr));
            }
        }

        private void executeScript(Node node) {
            String script = node.getParam("script");
            if (script == null || script.isEmpty()) return;
            ScriptEngine engine = ScriptEngine.getInstance();
            if (!engine.isAvailable()) {
                player.sendSystemMessage(Component.translatable("iscript.error.script_engine_unavailable"));
                return;
            }
            try {
                engine.execute(graph.getId(), script, player, level);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                player.sendSystemMessage(Component.translatable("iscript.error.script_error", msg));
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
                player.sendSystemMessage(Component.translatable("iscript.error.condition_error", msg));
                return false;
            }
        }

        private static class StackEntry {
            final Node node;
            final int depth;

            StackEntry(Node node, int depth) {
                this.node = node;
                this.depth = depth;
            }
        }
    }
}