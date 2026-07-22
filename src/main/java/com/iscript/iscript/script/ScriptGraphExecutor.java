package com.iscript.iscript.script;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.data.Graph;
import com.iscript.iscript.data.Node;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.script.ScriptNodeType;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.ClientEffectPacket;
import com.iscript.iscript.network.packet.OpenGuiPacket;
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

public class ScriptGraphExecutor {
    private final Graph graph;
    private final ServerPlayer player;
    private final ServerLevel level;
    private final Map<String, Object> localVars = new HashMap<>();
    private Node currentNode;
    private int delayTicks = 0;
    private boolean running = false;
    private boolean waitingForDialog = false;
    private String dialogChoice = "";
    private static final Map<UUID, ScriptGraphExecutor> activeExecutors = new HashMap<>();

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
        currentNode = graph.getNode(graph.getStartNodeId());
        if (currentNode == null) {
            IScriptMod.LOGGER.warn("Start node not found in graph {}", graph.getId());
            return;
        }
        running = true;
        activeExecutors.put(player.getUUID(), this);
        executeCurrent();
    }

    public static void stopFor(Player player) {
        stopFor(player.getUUID());
    }

    public static void stopFor(UUID playerId) {
        ScriptGraphExecutor exec = activeExecutors.remove(playerId);
        if (exec != null) {
            exec.running = false;
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
        if (!running || currentNode == null) return;
        if (waitingForDialog) return;
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }
        advance();
    }

    private void executeCurrent() {
        if (currentNode == null) {
            finish();
            return;
        }
        executeNode(currentNode);
        if (getNodeType(currentNode) == ScriptNodeType.DELAY) {
            try {
                delayTicks = Integer.parseInt(currentNode.getParam("ticks"));
            } catch (NumberFormatException e) {
                delayTicks = 20;
            }
        }
    }

    private ScriptNodeType getNodeType(Node node) {
        try {
            return ScriptNodeType.valueOf(node.getType());
        } catch (Exception e) {
            return ScriptNodeType.STOP;
        }
    }

    private void advance() {
        if (currentNode == null) {
            finish();
            return;
        }
        ScriptNodeType type = getNodeType(currentNode);
        Node.Connection next = null;
        if (type == ScriptNodeType.IF) {
            boolean condition = evaluateCondition(currentNode);
            for (Node.Connection c : currentNode.getConnections()) {
                if (condition && "true".equalsIgnoreCase(c.getConditionValue())) {
                    next = c;
                    break;
                }
                if (!condition && "false".equalsIgnoreCase(c.getConditionValue())) {
                    next = c;
                    break;
                }
            }
        } else if (type == ScriptNodeType.RANDOM) {
            List<Node.Connection> conns = currentNode.getConnections();
            if (!conns.isEmpty()) {
                next = conns.get(level.getRandom().nextInt(conns.size()));
            }
        } else if (type == ScriptNodeType.LOOP) {
            String targetId = currentNode.getParamOrDefault("loopTarget", graph.getStartNodeId());
            currentNode = graph.getNode(targetId);
            if (currentNode != null) {
                executeCurrent();
            } else {
                finish();
            }
            return;
        } else {
            if (!currentNode.getConnections().isEmpty()) {
                next = currentNode.getConnections().get(0);
            }
        }
        if (next != null) {
            currentNode = graph.getNode(next.getTarget());
            if (currentNode != null) {
                executeCurrent();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    private void executeNode(Node node) {
        ScriptNodeType type = getNodeType(node);
        switch (type) {
            case START, DELAY, IF, THEN, STOP -> {}
            case CAMERA -> {
                try {
                    double x = Double.parseDouble(node.getParam("x"));
                    double y = Double.parseDouble(node.getParam("y"));
                    double z = Double.parseDouble(node.getParam("z"));
                    float yaw = Float.parseFloat(node.getParam("yaw"));
                    float pitch = Float.parseFloat(node.getParam("pitch"));
                    int duration = Integer.parseInt(node.getParam("duration"));
                    IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.CAMERA_MOVE, ClientEffectPacket.cameraMoveToTag(x, y, z, yaw, pitch, duration)), player);
                } catch (NumberFormatException e) {
                    IScriptMod.LOGGER.error("Invalid camera params");
                }
            }
            case DIALOG -> {
                String dialogId = node.getParam("dialogId");
                if (!dialogId.isEmpty()) {
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
                        IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG, OpenGuiPacket.dialogToTag(filtered)), player);
                        waitingForDialog = true;
                    }
                }
            }
            case GIVE_ITEM -> {
                String itemId = node.getParam("itemId");
                int count = Integer.parseInt(node.getParamOrDefault("count", "1"));
                ItemStack stack = resolveItem(itemId, count);
                if (!stack.isEmpty()) {
                    player.getInventory().add(stack);
                }
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
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("Spawn entity failed: {}", e.getMessage());
                }
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
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("Set block failed: {}", e.getMessage());
                }
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
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("Play sound failed: {}", e.getMessage());
                }
            }
            case RUN_COMMAND -> {
                String command = node.getParam("command");
                if (!command.isEmpty()) {
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
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("Particle failed: {}", e.getMessage());
                }
            }
            case TELEPORT -> {
                try {
                    double x = Double.parseDouble(node.getParam("x"));
                    double y = Double.parseDouble(node.getParam("y"));
                    double z = Double.parseDouble(node.getParam("z"));
                    player.teleportTo(x, y, z);
                } catch (NumberFormatException e) {
                    IScriptMod.LOGGER.error("Invalid teleport params");
                }
            }
            case SET_GAMEMODE -> {
                String mode = node.getParam("mode");
                switch (mode.toLowerCase()) {
                    case "survival" -> player.setGameMode(GameType.SURVIVAL);
                    case "creative" -> player.setGameMode(GameType.CREATIVE);
                    case "adventure" -> player.setGameMode(GameType.ADVENTURE);
                    case "spectator" -> player.setGameMode(GameType.SPECTATOR);
                }
            }
            case SET_HEALTH -> {
                try {
                    float hp = Float.parseFloat(node.getParam("health"));
                    player.setHealth(hp);
                } catch (NumberFormatException e) {
                    IScriptMod.LOGGER.error("Invalid health param");
                }
            }
            case FREEZE -> IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.FREEZE_PLAYER, new CompoundTag()), player);
            case UNFREEZE -> IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.UNFREEZE_PLAYER, new CompoundTag()), player);
            case NPC_ANIMATE -> {
                try {
                    int entityId = Integer.parseInt(node.getParam("entityId"));
                    String anim = node.getParam("animation");
                    Entity entity = level.getEntity(entityId);
                    if (entity instanceof com.iscript.iscript.entity.IScriptNPCEntity npc) {
                        npc.playAnimation(anim);
                    }
                } catch (NumberFormatException e) {
                    IScriptMod.LOGGER.error("Invalid entityId");
                }
            }
            case NPC_MOVE -> {
                try {
                    int entityId = Integer.parseInt(node.getParam("entityId"));
                    double x = Double.parseDouble(node.getParam("x"));
                    double y = Double.parseDouble(node.getParam("y"));
                    double z = Double.parseDouble(node.getParam("z"));
                    Entity entity = level.getEntity(entityId);
                    if (entity instanceof com.iscript.iscript.entity.IScriptNPCEntity npc) {
                        npc.teleportTo(x, y, z);
                    }
                } catch (NumberFormatException e) {
                    IScriptMod.LOGGER.error("Invalid NPC move params");
                }
            }
            case QUEST_START -> DataAccess.startQuest(level, player.getUUID(), node.getParam("questId"));
            case QUEST_COMPLETE -> {
                var pq = DataAccess.playerQuests(level);
                if (pq != null) pq.completeQuest(player.getUUID(), node.getParam("questId"));
            }
            case SET_DATA -> {
                String key = node.getParam("key");
                String value = node.getParam("value");
                player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA).ifPresent(data -> data.setString(key, value));
            }
            case SET_FACTION -> {
                String faction = node.getParam("faction");
                player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA).ifPresent(data -> data.setFaction(faction));
            }
            case SET_REPUTATION -> {
                try {
                    int rep = Integer.parseInt(node.getParam("value"));
                    player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA).ifPresent(data -> data.setReputation(rep));
                } catch (NumberFormatException e) {
                    IScriptMod.LOGGER.error("Invalid reputation value");
                }
            }
            case SCRIPT_JS -> {
                String script = node.getParam("script");
                if (!script.isEmpty()) {
                    ScriptEngine engine = ScriptEngine.getInstance();
                    if (engine.isAvailable()) {
                        try {
                            engine.execute(script, player, level);
                        } catch (Exception e) {
                            IScriptMod.LOGGER.error("Script node JS error: {}", e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private boolean evaluateCondition(Node node) {
        String conditionType = node.getParam("conditionType");
        String value = node.getParam("value");
        String compare = node.getParamOrDefault("compare", "");
        return switch (conditionType.toLowerCase()) {
            case "has_item" -> {
                int count = Integer.parseInt(node.getParamOrDefault("count", "1"));
                Item target = ForgeRegistries.ITEMS.getValue(new ResourceLocation(value));
                int found = 0;
                for (ItemStack stack : player.getInventory().items) {
                    if (!stack.isEmpty() && target != null && stack.getItem() == target) {
                        found += stack.getCount();
                    }
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
            case "faction" -> player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA)
                    .map(data -> data.getFaction().equalsIgnoreCase(value)).orElse(false);
            case "reputation_above" -> {
                int amount = Integer.parseInt(node.getParamOrDefault("amount", "0"));
                yield player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA)
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
                for (com.iscript.iscript.blockentities.RegionBlockEntity rbe : com.iscript.iscript.blockentities.RegionBlockEntity.getAllInstances()) {
                    if (rbe.getLevel() == null || !rbe.getLevel().dimension().equals(level.dimension())) continue;
                    BlockPos rbePos = rbe.getBlockPos();
                    int rbeChunkX = rbePos.getX() >> 4;
                    int rbeChunkZ = rbePos.getZ() >> 4;
                    if (Math.abs(rbeChunkX - playerChunkX) > 2 || Math.abs(rbeChunkZ - playerChunkZ) > 2) continue;
                    if (rbe.getData().getId().equals(value) && rbe.getData().isInside(rbePos, player.position())) {
                        inside = true;
                        break;
                    }
                }
                yield inside;
            }
            case "random" -> {
                double chance = Double.parseDouble(node.getParamOrDefault("chance", "0.5"));
                yield level.getRandom().nextDouble() < chance;
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