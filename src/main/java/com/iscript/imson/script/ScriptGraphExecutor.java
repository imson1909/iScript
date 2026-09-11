package com.iscript.imson.script;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.data.DataAccess;
import com.iscript.imson.data.Graph;
import com.iscript.imson.data.Node;
import com.iscript.imson.data.dialog.DialogData;
import com.iscript.imson.data.script.ScriptNodeType;
import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.network.packet.ClientEffectPacket;
import com.iscript.imson.network.packet.OpenGuiPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

/**
 * Полностью итеративный executor графов.
 * Устранена рекурсия — теперь можно делать глубокие цепочки и LOOP без StackOverflow.
 * Добавлены: локальные переменные, стек вызовов, inline JS в SCRIPT_JS нодах.
 */
public class ScriptGraphExecutor {
    private final Graph graph;
    private final ServerPlayer player;
    private final ServerLevel level;
    private final Map<String, Object> localVars = new HashMap<>();
    private final Deque<Frame> stack = new ArrayDeque<>();
    private int delayTicks = 0;
    private boolean running = false;
    private boolean waitingForDialog = false;
    private String dialogChoice = "";
    private static final Map<UUID, ScriptGraphExecutor> activeExecutors = new HashMap<>();

    private static class Frame {
        Node node;
        int loopCounter = 0;
        Frame(Node node) { this.node = node; }
    }

    public ScriptGraphExecutor(Graph graph, ServerPlayer player, ServerLevel level) {
        this.graph = graph;
        this.player = player;
        this.level = level;
    }

    public void start() {
        if (graph.getStartNodeId().isEmpty()) {
            IScriptMod.LOGGER.warn("Script graph {} has no start node", graph.getId());
            return;
        }
        Node start = graph.getNode(graph.getStartNodeId());
        if (start == null) {
            IScriptMod.LOGGER.warn("Start node not found in graph {}", graph.getId());
            return;
        }
        running = true;
        activeExecutors.put(player.getUUID(), this);
        stack.push(new Frame(start));
        executeCurrent();
    }

    public static void stopFor(Player player) { stopFor(player.getUUID()); }

    public static void stopFor(UUID playerId) {
        ScriptGraphExecutor exec = activeExecutors.remove(playerId);
        if (exec != null) {
            exec.running = false;
            exec.stack.clear();
            IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.UNFREEZE_PLAYER, new CompoundTag()), exec.player);
            IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.CAMERA_RESET, new CompoundTag()), exec.player);
        }
    }

    public static ScriptGraphExecutor getActive(Player player) {
        return activeExecutors.get(player.getUUID());
    }

    public void onDialogChoice(String choiceId) {
        if (waitingForDialog) {
            dialogChoice = choiceId;
            waitingForDialog = false;
        }
    }

    public void tick() {
        if (!running) return;
        if (waitingForDialog) return;
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }
        // Итеративно выполняем, пока есть ноды и нет задержек
        int steps = 0;
        while (running && !waitingForDialog && delayTicks == 0 && !stack.isEmpty() && steps < 1000) {
            steps++;
            executeCurrent();
        }
        if (steps >= 1000) {
            IScriptMod.LOGGER.warn("Script graph {} exceeded max steps per tick, stopping", graph.getId());
            finish();
        }
        if (stack.isEmpty()) {
            finish();
        }
    }

    private void executeCurrent() {
        if (stack.isEmpty()) {
            finish();
            return;
        }
        Frame frame = stack.peek();
        Node node = frame.node;
        if (node == null) {
            stack.pop();
            return;
        }
        executeNode(node);
        ScriptNodeType type = getNodeType(node);

        if (type == ScriptNodeType.DELAY) {
            try {
                delayTicks = Integer.parseInt(node.getParam("ticks"));
            } catch (NumberFormatException e) {
                delayTicks = 20;
            }
            // Не убираем из стека — следующий tick продолжит
            return;
        }

        if (type == ScriptNodeType.STOP) {
            stack.clear();
            return;
        }

        // Определяем следующую ноду
        Node next = resolveNext(node, frame);
        if (next != null) {
            frame.node = next; // Заменяем текущий фрейм
            if (type == ScriptNodeType.LOOP) {
                // LOOP создаёт новый фрейм для body, а done выходит
                // Но в нашей модели LOOP — это одна нода с 2 выходами
                // body (slot 0) и done (slot 1)
                // Мы уже выбрали направление в resolveNext
            }
        } else {
            stack.pop(); // Конец ветки
        }
    }

    private Node resolveNext(Node node, Frame frame) {
        ScriptNodeType type = getNodeType(node);
        List<Node.Connection> conns = node.getConnections();

        if (type == ScriptNodeType.IF) {
            boolean condition = evaluateCondition(node);
            for (Node.Connection c : conns) {
                if (condition && "true".equalsIgnoreCase(c.getConditionValue())) {
                    return graph.getNode(c.getTarget());
                }
                if (!condition && "false".equalsIgnoreCase(c.getConditionValue())) {
                    return graph.getNode(c.getTarget());
                }
            }
            return null;
        }

        if (type == ScriptNodeType.RANDOM) {
            if (!conns.isEmpty()) {
                return graph.getNode(conns.get(level.getRandom().nextInt(conns.size())).getTarget());
            }
            return null;
        }

        if (type == ScriptNodeType.LOOP) {
            frame.loopCounter++;
            int targetCount = 3;
            try { targetCount = Integer.parseInt(node.getParam("count")); } catch (Exception ignored) {}
            // slot 0 = body, slot 1 = done
            if (frame.loopCounter <= targetCount) {
                for (Node.Connection c : conns) {
                    if ("body".equalsIgnoreCase(c.getConditionValue()) || c.getConditionValue() == null || c.getConditionValue().isEmpty()) {
                        if (frame.loopCounter == 1 || "body".equalsIgnoreCase(c.getConditionValue())) {
                            return graph.getNode(c.getTarget());
                        }
                    }
                }
            } else {
                for (Node.Connection c : conns) {
                    if ("done".equalsIgnoreCase(c.getConditionValue())) {
                        return graph.getNode(c.getTarget());
                    }
                }
            }
            // fallback
            if (!conns.isEmpty()) return graph.getNode(conns.get(0).getTarget());
            return null;
        }

        // Обычные ноды — берём первое соединение
        if (!conns.isEmpty()) {
            return graph.getNode(conns.get(0).getTarget());
        }
        return null;
    }

    private ScriptNodeType getNodeType(Node node) {
        try { return ScriptNodeType.valueOf(node.getType()); }
        catch (Exception e) { return ScriptNodeType.STOP; }
    }

    private void executeNode(Node node) {
        ScriptNodeType type = getNodeType(node);
        switch (type) {
            case START, DELAY, IF, STOP -> {}
            case CAMERA -> {
                try {
                    double x = Double.parseDouble(node.getParam("x"));
                    double y = Double.parseDouble(node.getParam("y"));
                    double z = Double.parseDouble(node.getParam("z"));
                    float yaw = Float.parseFloat(node.getParam("yaw"));
                    float pitch = Float.parseFloat(node.getParam("pitch"));
                    int duration = Integer.parseInt(node.getParam("duration"));
                    IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.CAMERA_MOVE,
                            ClientEffectPacket.cameraMoveToTag(x, y, z, yaw, pitch, duration)), player);
                } catch (NumberFormatException e) { IScriptMod.LOGGER.error("Invalid camera params"); }
            }
            case DIALOG -> {
                String dialogId = node.getParam("dialogId");
                if (dialogId != null && !dialogId.isEmpty()) {
                    DialogData dialog = DataAccess.dialog(dialogId);
                    if (dialog != null) {
                        DialogData filtered = new DialogData();
                        filtered.setId(dialog.getId());
                        filtered.setTitle(dialog.getTitle());
                        filtered.setText(dialog.getText());
                        filtered.setPortrait(dialog.getPortrait());
                        for (DialogData.DialogOption opt : dialog.getAvailableOptions(player)) {
                            filtered.getOptions().add(opt);
                        }
                        IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG,
                                OpenGuiPacket.dialogToTag(filtered)), player);
                        waitingForDialog = true;
                    }
                }
            }
            case GIVE_ITEM -> {
                String itemId = node.getParam("itemId");
                int count = Integer.parseInt(node.getParamOrDefault("count", "1"));
                ItemStack stack = resolveItem(itemId, count);
                if (!stack.isEmpty()) player.getInventory().add(stack);
            }
            case SPAWN_ENTITY -> {
                try {
                    ResourceLocation id = new ResourceLocation(node.getParam("entityType"));
                    EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
                    if (entityType != null) {
                        double x = Double.parseDouble(node.getParam("x"));
                        double y = Double.parseDouble(node.getParam("y"));
                        double z = Double.parseDouble(node.getParam("z"));
                        Entity entity = entityType.create(level);
                        if (entity != null) {
                            entity.setPos(x, y, z);
                            level.addFreshEntity(entity);
                        }
                    }
                } catch (Exception e) { IScriptMod.LOGGER.error("Spawn entity failed: {}", e.getMessage()); }
            }
            case SET_BLOCK -> {
                try {
                    ResourceLocation id = new ResourceLocation(node.getParam("blockId"));
                    Block block = ForgeRegistries.BLOCKS.getValue(id);
                    if (block != null) {
                        double x = Double.parseDouble(node.getParam("x"));
                        double y = Double.parseDouble(node.getParam("y"));
                        double z = Double.parseDouble(node.getParam("z"));
                        level.setBlockAndUpdate(new BlockPos((int) x, (int) y, (int) z), block.defaultBlockState());
                    }
                } catch (Exception e) { IScriptMod.LOGGER.error("Set block failed: {}", e.getMessage()); }
            }
            case PLAY_SOUND -> {
                try {
                    ResourceLocation id = new ResourceLocation(node.getParam("soundId"));
                    SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
                    if (sound != null) {
                        double x = Double.parseDouble(node.getParamOrDefault("x", String.valueOf(player.getX())));
                        double y = Double.parseDouble(node.getParamOrDefault("y", String.valueOf(player.getY())));
                        double z = Double.parseDouble(node.getParamOrDefault("z", String.valueOf(player.getZ())));
                        level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
                    }
                } catch (Exception e) { IScriptMod.LOGGER.error("Play sound failed: {}", e.getMessage()); }
            }
            case RUN_COMMAND -> {
                String command = node.getParam("command");
                if (command != null && !command.isEmpty()) {
                    level.getServer().getCommands().performPrefixedCommand(
                            player.createCommandSourceStack().withLevel(level).withPosition(player.position()),
                            command.replace("@p", player.getGameProfile().getName())
                    );
                }
            }
            case PARTICLE -> {
                try {
                    ResourceLocation id = new ResourceLocation(node.getParam("particleId"));
                    var pType = ForgeRegistries.PARTICLE_TYPES.getValue(id);
                    if (pType instanceof net.minecraft.core.particles.SimpleParticleType simple) {
                        double x = Double.parseDouble(node.getParam("x"));
                        double y = Double.parseDouble(node.getParam("y"));
                        double z = Double.parseDouble(node.getParam("z"));
                        level.sendParticles(simple, x, y, z, 1, 0, 0, 0, 0);
                    }
                } catch (Exception e) { IScriptMod.LOGGER.error("Particle failed: {}", e.getMessage()); }
            }
            case TELEPORT -> {
                try {
                    double x = Double.parseDouble(node.getParam("x"));
                    double y = Double.parseDouble(node.getParam("y"));
                    double z = Double.parseDouble(node.getParam("z"));
                    player.teleportTo(x, y, z);
                } catch (NumberFormatException e) { IScriptMod.LOGGER.error("Invalid teleport params"); }
            }
            case SET_GAMEMODE -> {
                String mode = node.getParam("mode");
                if (mode != null) {
                    switch (mode.toLowerCase()) {
                        case "survival" -> player.setGameMode(GameType.SURVIVAL);
                        case "creative" -> player.setGameMode(GameType.CREATIVE);
                        case "adventure" -> player.setGameMode(GameType.ADVENTURE);
                        case "spectator" -> player.setGameMode(GameType.SPECTATOR);
                    }
                }
            }
            case SET_HEALTH -> {
                try {
                    float hp = Float.parseFloat(node.getParam("health"));
                    player.setHealth(hp);
                } catch (NumberFormatException e) { IScriptMod.LOGGER.error("Invalid health param"); }
            }
            case FREEZE -> IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.FREEZE_PLAYER, new CompoundTag()), player);
            case UNFREEZE -> IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.UNFREEZE_PLAYER, new CompoundTag()), player);
            case NPC_ANIMATE -> {
                try {
                    int entityId = Integer.parseInt(node.getParam("entityId"));
                    String anim = node.getParam("animation");
                    Entity entity = level.getEntity(entityId);
                    if (entity instanceof com.iscript.imson.entity.IScriptNPCEntity npc) npc.playAnimation(anim);
                } catch (NumberFormatException e) { IScriptMod.LOGGER.error("Invalid entityId"); }
            }
            case NPC_MOVE -> {
                try {
                    int entityId = Integer.parseInt(node.getParam("entityId"));
                    double x = Double.parseDouble(node.getParam("x"));
                    double y = Double.parseDouble(node.getParam("y"));
                    double z = Double.parseDouble(node.getParam("z"));
                    Entity entity = level.getEntity(entityId);
                    if (entity instanceof com.iscript.imson.entity.IScriptNPCEntity npc) npc.teleportTo(x, y, z);
                } catch (NumberFormatException e) { IScriptMod.LOGGER.error("Invalid NPC move params"); }
            }
            case QUEST_START -> DataAccess.startQuest(level, player.getUUID(), node.getParam("questId"));
            case QUEST_COMPLETE -> {
                var pq = DataAccess.playerQuests(level);
                if (pq != null) pq.completeQuest(player.getUUID(), node.getParam("questId"));
            }
            case SET_DATA -> {
                String key = node.getParam("key");
                String value = node.getParam("value");
                if (key != null) player.getCapability(com.iscript.imson.capability.ModCapabilities.PLAYER_DATA).ifPresent(data -> data.setString(key, value));
            }
            case SET_FACTION -> {
                String faction = node.getParam("faction");
                if (faction != null) player.getCapability(com.iscript.imson.capability.ModCapabilities.PLAYER_DATA).ifPresent(data -> data.setFaction(faction));
            }
            case SET_REPUTATION -> {
                try {
                    int rep = Integer.parseInt(node.getParam("value"));
                    player.getCapability(com.iscript.imson.capability.ModCapabilities.PLAYER_DATA).ifPresent(data -> data.setReputation(rep));
                } catch (NumberFormatException e) { IScriptMod.LOGGER.error("Invalid reputation value"); }
            }
            case SCRIPT_JS -> {
                String script = node.getParam("script");
                String functionName = node.getParam("function");
                ScriptEngine engine = ScriptEngine.getInstance();
                if (engine != null && engine.isAvailable()) {
                    try {
                        if (functionName != null && !functionName.isEmpty()) {
                            String scriptId = graph.getId() + ":" + functionName;
                            engine.execute(scriptId, script, player, level);
                            engine.callFunction(scriptId, functionName, player, level);
                        } else {
                            engine.execute(script, player, level);
                        }
                    } catch (Exception e) { IScriptMod.LOGGER.error("Script node JS error: {}", e.getMessage()); }
                }
            }
        }
    }

    private boolean evaluateCondition(Node node) {
        String conditionType = node.getParam("conditionType");
        String value = node.getParam("value");
        String compare = node.getParamOrDefault("compare", "");
        return switch (conditionType != null ? conditionType.toLowerCase() : "") {
            case "has_item" -> {
                int count = Integer.parseInt(node.getParamOrDefault("count", "1"));
                Item target = ForgeRegistries.ITEMS.getValue(new ResourceLocation(value));
                int found = 0;
                for (ItemStack stack : player.getInventory().items) {
                    if (!stack.isEmpty() && target != null && stack.getItem() == target) found += stack.getCount();
                }
                yield found >= count;
            }
            case "quest_completed" -> {
                var pq = DataAccess.playerQuests(level);
                yield pq != null && pq.hasCompleted(player.getUUID(), value);
            }
            case "quest_active" -> {
                var pq = DataAccess.playerQuests(level);
                yield pq != null && pq.isActive(player.getUUID(), value);
            }
            case "faction" -> player.getCapability(com.iscript.imson.capability.ModCapabilities.PLAYER_DATA)
                    .map(data -> data.getFaction().equalsIgnoreCase(value)).orElse(false);
            case "reputation_above" -> {
                int amount = Integer.parseInt(node.getParamOrDefault("amount", "0"));
                yield player.getCapability(com.iscript.imson.capability.ModCapabilities.PLAYER_DATA)
                        .map(data -> data.getReputation() >= amount).orElse(false);
            }
            case "health_above" -> {
                float hp = Float.parseFloat(node.getParamOrDefault("amount", "0"));
                yield player.getHealth() >= hp;
            }
            case "in_region" -> {
                boolean inside = false;
                int playerChunkX = player.chunkPosition().x;
                int playerChunkZ = player.chunkPosition().z;
                for (com.iscript.imson.blockentities.RegionBlockEntity rbe : com.iscript.imson.blockentities.RegionBlockEntity.getAllInstances()) {
                    if (rbe.getLevel() == null || !rbe.getLevel().dimension().equals(level.dimension())) continue;
                    BlockPos rbePos = rbe.getBlockPos();
                    int rbeChunkX = rbePos.getX() >> 4;
                    int rbeChunkZ = rbePos.getZ() >> 4;
                    if (Math.abs(rbeChunkX - playerChunkX) > 2 || Math.abs(rbeChunkZ - playerChunkZ) > 2) continue;
                    if (rbe.getData().getId().equals(value) && rbe.getData().isInside(rbePos, player.position())) {
                        inside = true; break;
                    }
                }
                yield inside;
            }
            case "random" -> {
                double chance = Double.parseDouble(node.getParamOrDefault("chance", "0.5"));
                yield level.getRandom().nextDouble() < chance;
            }
            case "inline_js" -> {
                // NEW: выполняет JS-код и возвращает boolean результат
                String js = node.getParam("inlineScript");
                if (js == null || js.isEmpty()) yield false;
                ScriptEngine engine = ScriptEngine.getInstance();
                if (engine == null || !engine.isAvailable()) yield false;
                try {
                    Object result = engine.execute("__inline_cond_" + System.currentTimeMillis(),
                            "(function(){" + js + "})()", player, level);
                    yield Boolean.TRUE.equals(result) || (result instanceof Number n && n.doubleValue() != 0);
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("Inline JS condition error: {}", e.getMessage());
                    yield false;
                }
            }
            default -> false;
        };
    }

    private void finish() {
        running = false;
        activeExecutors.remove(player.getUUID());
        IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.UNFREEZE_PLAYER, new CompoundTag()), player);
        IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.CAMERA_RESET, new CompoundTag()), player);
    }

    private ItemStack resolveItem(String itemId, int count) {
        if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        return item != null ? new ItemStack(item, count) : ItemStack.EMPTY;
    }
}