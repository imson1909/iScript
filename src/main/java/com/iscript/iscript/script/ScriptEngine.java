package com.iscript.iscript.script;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.config.IScriptConfig;
import com.iscript.iscript.data.DialogManager;
import com.iscript.iscript.data.QuestManager;
import com.iscript.iscript.data.WorldDataManager;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.*;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import com.iscript.iscript.data.WorldDataManager;
import com.iscript.iscript.data.script.ScriptGraphData;
import com.iscript.iscript.script.ScriptGraphExecutor;
import com.iscript.iscript.script.ScriptGraphManager;
import com.iscript.iscript.network.packet.SyncDataPacket;
import com.iscript.iscript.network.packet.OpenGuiPacket;
import com.iscript.iscript.network.packet.ClientEffectPacket;
import com.iscript.iscript.network.packet.ServerCommandPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScriptEngine {
    private static ScriptEngine instance;
    private Context context;
    private final Map<String, Object> globals = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ScriptEngine() {
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
                    .allowHostClassLookup(className -> true)
                    .allowAllAccess(true)
                    .build();

            initGlobals();

            IScriptMod.LOGGER.info("GraalJS initialized successfully");
        } catch (Exception e) {
            IScriptMod.LOGGER.error("GraalJS initialization failed: {}", e.getMessage());
            this.context = null;
        }
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

    public void setGlobal(String name, Object value) {
        globals.put(name, value);
        if (context != null) {
            context.getBindings("js").putMember(name, value);
        }
    }

    public Object execute(String script, Player player, ServerLevel level) {
        if (!IScriptConfig.ENABLE_SCRIPTING.get()) {
            throw new IllegalStateException("Scripting disabled in config");
        }
        if (context == null) {
            throw new IllegalStateException("GraalJS context not initialized");
        }
        synchronized (context) {
            try {
                ScriptAPI api = new ScriptAPI(player, level, this);
                context.getBindings("js").putMember("api", api);
                context.getBindings("js").putMember("player", player);
                context.getBindings("js").putMember("level", level);
                context.getBindings("js").putMember("server", level.getServer());
                for (Map.Entry<String, Object> entry : globals.entrySet()) {
                    context.getBindings("js").putMember(entry.getKey(), entry.getValue());
                }
                Value result = context.eval("js", script);
                return result.isNull() ? null : result.as(Object.class);
            } catch (PolyglotException e) {
                throw new RuntimeException("JS Error: " + e.getMessage(), e);
            }
        }
    }

    public boolean isAvailable() {
        return context != null && IScriptConfig.ENABLE_SCRIPTING.get();
    }

    public void schedule(Runnable task, long delayMs) {
        scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }

    public void scheduleRepeating(Runnable task, long delayMs, long periodMs) {
        scheduler.scheduleAtFixedRate(task, delayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    public static class ScriptAPI {
        private final Player player;
        private final ServerLevel level;
        private final ScriptEngine engine;

        public ScriptAPI(Player player, ServerLevel level, ScriptEngine engine) {
            this.player = player;
            this.level = level;
            this.engine = engine;
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
            engine.schedule(() -> {
                try {
                    engine.execute(script, player, level);
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("setTimeout error: {}", e.getMessage());
                }
            }, delayMs);
        }

        @HostAccess.Export
        public void setInterval(String script, int delayMs) {
            engine.scheduleRepeating(() -> {
                try {
                    engine.execute(script, player, level);
                } catch (Exception e) {
                    IScriptMod.LOGGER.error("setInterval error: {}", e.getMessage());
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
                DialogData dialog = DialogManager.get(level, dialogId);
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
        public void worldSetString(String key, String value) { WorldDataManager.setString(level, key, value); }

        @HostAccess.Export
        public String worldGetString(String key) { return WorldDataManager.getString(level, key); }

        @HostAccess.Export
        public void worldSetInt(String key, int value) { WorldDataManager.setInt(level, key, value); }

        @HostAccess.Export
        public int worldGetInt(String key) { return WorldDataManager.getInt(level, key); }

        @HostAccess.Export
        public void worldSetDouble(String key, double value) { WorldDataManager.setDouble(level, key, value); }

        @HostAccess.Export
        public double worldGetDouble(String key) { return WorldDataManager.getDouble(level, key); }

        @HostAccess.Export
        public void worldSetBool(String key, boolean value) { WorldDataManager.setBool(level, key, value); }

        @HostAccess.Export
        public boolean worldGetBool(String key) { return WorldDataManager.getBool(level, key); }

        @HostAccess.Export
        public int worldAddInt(String key, int delta) {
            synchronized (ScriptEngine.getInstance()) {
                int val = WorldDataManager.getInt(level, key) + delta;
                WorldDataManager.setInt(level, key, val);
                return val;
            }
        }
        @HostAccess.Export
        public void scriptGraphRun(String graphId) {
            if (player instanceof ServerPlayer serverPlayer) {
                ScriptGraphData graph = ScriptGraphManager.get(level, graphId);
                if (graph != null) {
                    ScriptGraphExecutor executor = new ScriptGraphExecutor(graph, serverPlayer, level);
                    executor.start();
                }
            }
        }
    }
}