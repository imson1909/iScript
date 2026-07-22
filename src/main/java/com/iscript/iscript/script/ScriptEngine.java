package com.iscript.iscript.script;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.config.IScriptConfig;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.data.Graph;
import com.iscript.iscript.data.GlobalStates;
import com.iscript.iscript.data.ModData;
import com.iscript.iscript.morph.MorphData;
import com.iscript.iscript.morph.network.MorphSyncPacket;
import com.iscript.iscript.network.IScriptNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import com.iscript.iscript.network.packet.OpenGuiPacket;
import com.iscript.iscript.network.packet.ClientEffectPacket;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScriptEngine {
    private static ScriptEngine instance;
    private static final ThreadLocal<String> ACTIVE_SCRIPT_ID = new ThreadLocal<>();
    private final Map<java.util.UUID, String> lastScriptIdByPlayer = new ConcurrentHashMap<>();
    private Context context;
    private final Map<String, Object> globals = new HashMap<>();
    private final Queue<DelayedTask> taskQueue = new ConcurrentLinkedQueue<>();
    private long serverTickCounter = 0;

    private ScriptEngine() {
        createContext();
    }

    private void createContext() {
        try {
            HostAccess hostAccess = HostAccess.newBuilder(HostAccess.ALL)
                    .allowAccessAnnotatedBy(HostAccess.Export.class)
                    .allowArrayAccess(true)
                    .allowListAccess(true)
                    .allowMapAccess(true)
                    .allowBufferAccess(true)
                    .build();

            this.context = Context.newBuilder("js")
                    .allowHostAccess(hostAccess)
                    .allowHostClassLookup(ScriptEngine::isHostClassAllowed)
                    .allowCreateThread(false)
                    .allowIO(false)
                    .allowNativeAccess(false)
                    .allowExperimentalOptions(false)
                    .build();

            initGlobals();

            IScriptMod.LOGGER.info("GraalJS initialized successfully");
        } catch (Exception e) {
            IScriptMod.LOGGER.error("GraalJS initialization failed: {}", e.getMessage());
            this.context = null;
        }
    }

    private static boolean isHostClassAllowed(String className) {
        if (className.startsWith("net.minecraft.")) return true;
        if (className.startsWith("com.iscript.iscript.")) return true;
        if (className.startsWith("java.lang.")) {
            return !(className.contains("Runtime")
                    || className.contains("Process")
                    || className.contains("reflect.")
                    || className.contains("ClassLoader")
                    || className.contains("Thread")
                    || className.contains("System")
                    || className.contains("Security")
                    || className.contains("invoke."));
        }
        if (className.startsWith("java.util.")) {
            return !(className.contains("jar.")
                    || className.contains("zip.")
                    || className.contains("ServiceLoader"));
        }
        return false;
    }

    private void initGlobals() {
        setGlobal("Component", Component.class);
        setGlobal("ItemStack", ItemStack.class);
        setGlobal("Items", Items.class);
        setGlobal("Block", Block.class);
        setGlobal("Blocks", net.minecraft.world.level.block.Blocks.class);
        setGlobal("Vec3", Vec3.class);
        setGlobal("ResourceLocation", ResourceLocation.class);
        setGlobal("ChatFormatting", ChatFormatting.class);
        setGlobal("EntityType", EntityType.class);
        setGlobal("GameType", GameType.class);
        setGlobal("BlockPos", BlockPos.class);
        setGlobal("SoundSource", SoundSource.class);
        setGlobal("ForgeRegistries", ForgeRegistries.class);
    }

    public static ScriptEngine getInstance() {
        if (instance == null) {
            instance = new ScriptEngine();
        }
        return instance;
    }

    public static void setActiveScriptId(String scriptId) {
        ACTIVE_SCRIPT_ID.set(scriptId);
    }

    public String getLastScriptIdFor(Player player) {
        return player != null ? lastScriptIdByPlayer.get(player.getUUID()) : null;
    }

    public void setGlobal(String name, Object value) {
        globals.put(name, value);
        if (context != null) {
            context.getBindings("js").putMember(name, value);
        }
    }

    public Object execute(String script, Player player, ServerLevel level) {
        String id = ACTIVE_SCRIPT_ID.get();
        ACTIVE_SCRIPT_ID.remove();
        if (id == null && player != null) {
            id = lastScriptIdByPlayer.get(player.getUUID());
        }
        if (id == null || id.isEmpty()) {
            id = "<unknown>";
        }
        return execute(id, script, player, level);
    }

    public Object execute(String scriptId, String script, Player player, ServerLevel level) {
        if (scriptId != null && !scriptId.isEmpty() && player != null) {
            lastScriptIdByPlayer.put(player.getUUID(), scriptId);
        }
        if (!IScriptConfig.ENABLE_SCRIPTING.get()) {
            throw new IllegalStateException("Scripting disabled in config");
        }
        if (context == null) {
            throw new IllegalStateException("GraalJS context not initialized");
        }
        synchronized (this) {
            try {
                ScriptAPI api = new ScriptAPI(player, level, this, scriptId);
                context.getBindings("js").putMember("api", api);
                context.getBindings("js").putMember("player", player);
                context.getBindings("js").putMember("level", level);
                context.getBindings("js").putMember("server", level.getServer());
                for (Map.Entry<String, Object> entry : globals.entrySet()) {
                    context.getBindings("js").putMember(entry.getKey(), entry.getValue());
                }
                Source source = Source.newBuilder("js", script, scriptId != null ? scriptId : "<unknown>").build();
                Value result = context.eval(source);
                return result.isNull() ? null : result.as(Object.class);
            } catch (PolyglotException e) {
                int line = -1;
                if (e.getSourceLocation() != null) {
                    line = e.getSourceLocation().getStartLine();
                }
                String msg = "JS Error: " + e.getMessage();
                if (player instanceof ServerPlayer serverPlayer) {
                    IScriptNetwork.sendToPlayer(
                            new ClientEffectPacket(
                                    ClientEffectPacket.Type.LOG_MESSAGE,
                                    ClientEffectPacket.logMessageToTag(msg, "ERROR", scriptId, line)
                            ),
                            serverPlayer
                    );
                }
                throw new RuntimeException(msg, e);
            } catch (IOException e) {
                throw new RuntimeException("Failed to build script source", e);
            }
        }
    }

    public boolean isAvailable() {
        return context != null && IScriptConfig.ENABLE_SCRIPTING.get();
    }

    public void reload() {
        IScriptMod.LOGGER.info("Reloading GraalJS context...");
        synchronized (this) {
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    IScriptMod.LOGGER.warn("Error closing old GraalJS context: {}", e.getMessage());
                }
            }
            globals.clear();
            createContext();
        }
    }

    public void shutdown() {
        synchronized (this) {
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    IScriptMod.LOGGER.warn("Error closing GraalJS context: {}", e.getMessage());
                }
                context = null;
            }
        }
        taskQueue.clear();
    }

    public void onServerTick() {
        serverTickCounter++;
        DelayedTask task;
        while ((task = taskQueue.peek()) != null) {
            if (task.executeTick > serverTickCounter) break;
            taskQueue.poll();
            if (task.cancelled) continue;
            try {
                task.runnable.run();
            } catch (Exception e) {
                IScriptMod.LOGGER.error("Delayed task error: {}", e.getMessage());
                if (task.player instanceof ServerPlayer serverPlayer) {
                    IScriptNetwork.sendToPlayer(
                            new ClientEffectPacket(
                                    ClientEffectPacket.Type.LOG_MESSAGE,
                                    ClientEffectPacket.logMessageToTag("Delayed task error: " + e.getMessage(), "ERROR", task.scriptId, -1)
                            ),
                            serverPlayer
                    );
                }
            }
            if (task.repeating) {
                task.executeTick = serverTickCounter + task.delayTicks;
                taskQueue.offer(task);
            }
        }
    }

    public void schedule(Runnable task, long delayMs) {
        schedule(null, null, task, delayMs);
    }

    public void schedule(String scriptId, Player player, Runnable task, long delayMs) {
        long delayTicks = Math.max(1, delayMs / 50);
        taskQueue.offer(new DelayedTask(scriptId, player, task, serverTickCounter + delayTicks, false, 0));
    }

    public void scheduleRepeating(Runnable task, long delayMs, long periodMs) {
        scheduleRepeating(null, null, task, delayMs, periodMs);
    }

    public void scheduleRepeating(String scriptId, Player player, Runnable task, long delayMs, long periodMs) {
        long delayTicks = Math.max(1, delayMs / 50);
        long periodTicks = Math.max(1, periodMs / 50);
        taskQueue.offer(new DelayedTask(scriptId, player, task, serverTickCounter + delayTicks, true, periodTicks));
    }

    public void cancelAllTasks() {
        for (DelayedTask task : taskQueue) {
            task.cancelled = true;
        }
        taskQueue.clear();
    }

    private static class DelayedTask {
        final String scriptId;
        final Player player;
        final Runnable runnable;
        long executeTick;
        final boolean repeating;
        final long delayTicks;
        volatile boolean cancelled = false;

        DelayedTask(String scriptId, Player player, Runnable runnable, long executeTick, boolean repeating, long delayTicks) {
            this.scriptId = scriptId;
            this.player = player;
            this.runnable = runnable;
            this.executeTick = executeTick;
            this.repeating = repeating;
            this.delayTicks = delayTicks;
        }
    }

    public static class ScriptAPI {
        private final Player player;
        private final ServerLevel level;
        private final ScriptEngine engine;
        private final String scriptId;

        public ScriptAPI(Player player, ServerLevel level, ScriptEngine engine) {
            this(player, level, engine, null);
        }

        public ScriptAPI(Player player, ServerLevel level, ScriptEngine engine, String scriptId) {
            this.player = player;
            this.level = level;
            this.engine = engine;
            this.scriptId = scriptId;
        }

        @HostAccess.Export
        public Player getPlayer() {
            return player;
        }

        @HostAccess.Export
        public ServerLevel getLevel() {
            return level;
        }

        @HostAccess.Export
        public void sendMessage(String text) {
            player.sendSystemMessage(Component.literal(text));
        }

        @HostAccess.Export
        public void sendMessage(String text, String color) {
            try {
                ChatFormatting formatting = ChatFormatting.valueOf(color.toUpperCase());
                player.sendSystemMessage(Component.literal(text).withStyle(formatting));
            } catch (IllegalArgumentException e) {
                player.sendSystemMessage(Component.literal(text));
            }
        }

        @HostAccess.Export
        public void giveItem(String itemId, int count) {
            ItemStack stack = switch (itemId.toLowerCase()) {
                case "diamond" -> new ItemStack(Items.DIAMOND, count);
                case "iron_ingot" -> new ItemStack(Items.IRON_INGOT, count);
                case "gold_ingot" -> new ItemStack(Items.GOLD_INGOT, count);
                case "emerald" -> new ItemStack(Items.EMERALD, count);
                case "stick" -> new ItemStack(Items.STICK, count);
                case "apple" -> new ItemStack(Items.APPLE, count);
                default -> ItemStack.EMPTY;
            };
            if (!stack.isEmpty()) {
                player.getInventory().add(stack);
            }
        }

        @HostAccess.Export
        public void teleport(double x, double y, double z) {
            player.teleportTo(x, y, z);
        }

        @HostAccess.Export
        public void runCommand(String command) {
            level.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withLevel(level).withPosition(player.position()),
                    command.replace("@p", player.getGameProfile().getName())
            );
        }

        @HostAccess.Export
        public double getX() { return player.getX(); }

        @HostAccess.Export
        public double getY() { return player.getY(); }

        @HostAccess.Export
        public double getZ() { return player.getZ(); }

        @HostAccess.Export
        public String getName() {
            return player.getGameProfile().getName();
        }

        @HostAccess.Export
        public void setTimeout(String script, int delayMs) {
            engine.schedule(scriptId, player, () -> {
                try {
                    engine.execute(scriptId, script, player, level);
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("setTimeout error: {}", e.getMessage());
                    if (player instanceof ServerPlayer serverPlayer) {
                        IScriptNetwork.sendToPlayer(
                                new ClientEffectPacket(
                                        ClientEffectPacket.Type.LOG_MESSAGE,
                                        ClientEffectPacket.logMessageToTag("setTimeout error: " + e.getMessage(), "ERROR", scriptId, -1)
                                ),
                                serverPlayer
                        );
                    }
                }
            }, delayMs);
        }

        @HostAccess.Export
        public void setInterval(String script, int delayMs) {
            engine.scheduleRepeating(scriptId, player, () -> {
                try {
                    engine.execute(scriptId, script, player, level);
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("setInterval error: {}", e.getMessage());
                    if (player instanceof ServerPlayer serverPlayer) {
                        IScriptNetwork.sendToPlayer(
                                new ClientEffectPacket(
                                        ClientEffectPacket.Type.LOG_MESSAGE,
                                        ClientEffectPacket.logMessageToTag("setInterval error: " + e.getMessage(), "ERROR", scriptId, -1)
                                ),
                                serverPlayer
                        );
                    }
                }
            }, delayMs, delayMs);
        }

        @HostAccess.Export
        public void setData(String key, String value) {
            player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA).ifPresent(data -> {
                data.setString(key, value);
            });
        }

        @HostAccess.Export
        public String getData(String key) {
            return player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA)
                    .map(data -> data.getString(key)).orElse("");
        }

        @HostAccess.Export
        public void setIntData(String key, int value) {
            player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA).ifPresent(data -> {
                data.setInt(key, value);
            });
        }

        @HostAccess.Export
        public int getIntData(String key) {
            return player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA)
                    .map(data -> data.getInt(key)).orElse(0);
        }

        @HostAccess.Export
        public void setFaction(String faction) {
            player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA).ifPresent(data -> {
                data.setFaction(faction);
            });
        }

        @HostAccess.Export
        public String getFaction() {
            return player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA)
                    .map(com.iscript.iscript.capability.PlayerData::getFaction).orElse("neutral");
        }

        @HostAccess.Export
        public void setReputation(int value) {
            player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA).ifPresent(data -> {
                data.setReputation(value);
            });
        }

        @HostAccess.Export
        public int getReputation() {
            return player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_DATA)
                    .map(com.iscript.iscript.capability.PlayerData::getReputation).orElse(0);
        }

        @HostAccess.Export
        public void spawnEntity(String type, double x, double y, double z) {
            try {
                ResourceLocation id = new ResourceLocation(type);
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
                if (entityType != null) {
                    Entity entity = entityType.create(level);
                    if (entity != null) {
                        entity.setPos(x, y, z);
                        level.addFreshEntity(entity);
                    }
                }
            } catch (Exception e) {
                IScriptMod.LOGGER.error("Failed to spawn entity: {}", e.getMessage());
            }
        }

        @HostAccess.Export
        public void setBlock(String blockId, double x, double y, double z) {
            try {
                ResourceLocation id = new ResourceLocation(blockId);
                Block block = ForgeRegistries.BLOCKS.getValue(id);
                if (block != null) {
                    level.setBlockAndUpdate(new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)), block.defaultBlockState());
                }
            } catch (Exception e) {
                IScriptMod.LOGGER.error("Failed to set block: {}", e.getMessage());
            }
        }

        @HostAccess.Export
        public String getBlock(double x, double y, double z) {
            return level.getBlockState(new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))).getBlock().builtInRegistryHolder().key().location().toString();
        }

        @HostAccess.Export
        public void playSound(String soundId, double x, double y, double z) {
            try {
                ResourceLocation id = new ResourceLocation(soundId);
                SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
                if (sound != null) {
                    level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            } catch (Exception e) {
                IScriptMod.LOGGER.error("Failed to play sound: {}", e.getMessage());
            }
        }

        @HostAccess.Export
        public void particle(String particleId, double x, double y, double z) {
            try {
                ResourceLocation id = new ResourceLocation(particleId);
                var pType = ForgeRegistries.PARTICLE_TYPES.getValue(id);
                if (pType instanceof SimpleParticleType simple) {
                    level.sendParticles(simple, x, y, z, 1, 0, 0, 0, 0);
                } else {
                    player.sendSystemMessage(Component.literal("§cParticle '" + particleId + "' is not a simple particle type"));
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("§cParticle error: " + e.getMessage()));
            }
        }

        @HostAccess.Export
        public void setHealth(double hp) {
            player.setHealth((float) hp);
        }

        @HostAccess.Export
        public double getHealth() {
            return player.getHealth();
        }

        @HostAccess.Export
        public void setGamemode(String mode) {
            if (player instanceof ServerPlayer serverPlayer) {
                switch (mode.toLowerCase()) {
                    case "survival" -> serverPlayer.setGameMode(GameType.SURVIVAL);
                    case "creative" -> serverPlayer.setGameMode(GameType.CREATIVE);
                    case "adventure" -> serverPlayer.setGameMode(GameType.ADVENTURE);
                    case "spectator" -> serverPlayer.setGameMode(GameType.SPECTATOR);
                }
            }
        }

        @HostAccess.Export
        public boolean hasItem(String itemId, int count) {
            int found = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && stack.getItem().builtInRegistryHolder().key().location().toString().equals(itemId)) {
                    found += stack.getCount();
                }
            }
            return found >= count;
        }

        @HostAccess.Export
        public void clearInventory() {
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                player.getInventory().items.set(i, ItemStack.EMPTY);
            }
        }

        @HostAccess.Export
        public void cameraMove(double x, double y, double z, float yaw, float pitch, int durationTicks) {
            if (player instanceof ServerPlayer serverPlayer) {
                IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.CAMERA_MOVE, ClientEffectPacket.cameraMoveToTag(x, y, z, yaw, pitch, durationTicks)), serverPlayer);
            }
        }

        @HostAccess.Export
        public void cameraReset() {
            if (player instanceof ServerPlayer serverPlayer) {
                IScriptNetwork.sendToPlayer(new ClientEffectPacket(ClientEffectPacket.Type.CAMERA_RESET, new CompoundTag()), serverPlayer);
            }
        }

        @HostAccess.Export
        public void dialogOpen(String dialogId) {
            if (player instanceof ServerPlayer serverPlayer) {
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
                    IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG, OpenGuiPacket.dialogToTag(filtered)), serverPlayer);
                }
            }
        }

        @HostAccess.Export
        public void questStart(String questId) {
            player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_QUESTS).ifPresent(data -> {
                if (data.getProgress(questId) == 0 && !data.isCompleted(questId)) {
                    data.setProgress(questId, 1);
                }
            });
        }

        @HostAccess.Export
        public void questComplete(String questId) {
            player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_QUESTS).ifPresent(data -> {
                data.complete(questId);
            });
        }

        @HostAccess.Export
        public void npcMove(int entityId, double x, double y, double z) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof com.iscript.iscript.entity.IScriptNPCEntity npc) {
                npc.teleportTo(x, y, z);
            }
        }

        @HostAccess.Export
        public void scriptGraphRun(String graphId) {
            if (player instanceof ServerPlayer serverPlayer) {
                Graph graph = DataAccess.scriptGraph(graphId);
                if (graph != null) {
                    ScriptGraphExecutor executor = new ScriptGraphExecutor(graph, serverPlayer, level);
                    executor.start();
                }
            }
        }

        @HostAccess.Export
        public void setState(String key, Object value) {
            if (value instanceof Number n) {
                GlobalStates.get().setNumber(key, n.doubleValue());
            } else {
                GlobalStates.get().setString(key, value.toString());
            }
            GlobalStates.save();
            ModData.setDirty();
        }

        @HostAccess.Export
        public String getStateString(String key) {
            return GlobalStates.get().getString(key);
        }

        @HostAccess.Export
        public double getStateNumber(String key) {
            return GlobalStates.get().getNumber(key);
        }

        @HostAccess.Export
        public boolean hasState(String key) {
            return GlobalStates.get().has(key);
        }

        @HostAccess.Export
        public void addState(String key, double delta) {
            GlobalStates.get().add(key, delta);
            GlobalStates.save();
            ModData.setDirty();
        }

        @HostAccess.Export
        public void removeState(String key) {
            GlobalStates.get().remove(key);
            GlobalStates.save();
            ModData.setDirty();
        }

        @HostAccess.Export
        public void log(String message) {
            sendLog(message, "INFO", scriptId, -1);
        }

        @HostAccess.Export
        public void logInfo(String message) {
            sendLog(message, "INFO", scriptId, -1);
        }

        @HostAccess.Export
        public void logWarn(String message) {
            sendLog(message, "WARN", scriptId, -1);
        }

        @HostAccess.Export
        public void logError(String message) {
            sendLog(message, "ERROR", scriptId, -1);
        }

        @HostAccess.Export
        public void logDebug(String message) {
            sendLog(message, "DEBUG", scriptId, -1);
        }

        @HostAccess.Export
        public void morph(String modelId) {
            if (player == null) return;
            player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
                data.setModelId(modelId);
                data.setTextureId(modelId);
                data.setMorphed(true);
                data.setCurrentAnimation("animation." + modelId + ".idle");
                data.resetAnimationTick();
                if (player instanceof ServerPlayer sp) {
                    IScriptNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), MorphSyncPacket.create(player));
                }
            });
        }

        @HostAccess.Export
        public void morphReset() {
            if (player == null) return;
            player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
                data.setMorphed(false);
                data.setModelId("");
                data.setCurrentAnimation("idle");
                if (player instanceof ServerPlayer sp) {
                    IScriptNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), MorphSyncPacket.create(player));
                }
            });
        }

        @HostAccess.Export
        public void morphScale(float scale) {
            if (player == null) return;
            player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
                data.setScale(scale);
                if (player instanceof ServerPlayer sp) {
                    IScriptNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), MorphSyncPacket.create(player));
                }
            });
        }

        @HostAccess.Export
        public void playAnim(String animationName) {
            if (player == null) return;
            player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
                data.setCurrentAnimation(animationName);
                data.resetAnimationTick();
                if (player instanceof ServerPlayer sp) {
                    IScriptNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), MorphSyncPacket.create(player));
                }
            });
        }

        @HostAccess.Export
        public boolean isMorphed() {
            return player != null && player.getCapability(MorphData.CAPABILITY)
                    .map(MorphData::isMorphed).orElse(false);
        }

        @HostAccess.Export
        public String getMorphId() {
            return player != null ? player.getCapability(MorphData.CAPABILITY)
                    .map(MorphData::getModelId).orElse("") : "";
        }

        private void sendLog(String message, String level, String sourceFile, int sourceLine) {
            if (sourceFile == null || sourceFile.isEmpty()) {
                sourceFile = engine.getLastScriptIdFor(player);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                IScriptNetwork.sendToPlayer(
                        new ClientEffectPacket(
                                ClientEffectPacket.Type.LOG_MESSAGE,
                                ClientEffectPacket.logMessageToTag(message, level, sourceFile, sourceLine)
                        ),
                        serverPlayer
                );
            }
        }
    }
}