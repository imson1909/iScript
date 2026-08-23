package com.iscript.imson.script.api;

import com.iscript.imson.data.GlobalStates;
import com.iscript.imson.data.ModData;
import com.iscript.imson.script.ScriptExecutionService;
import com.iscript.imson.script.ScriptTaskScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.graalvm.polyglot.HostAccess;

public class ScriptAPI {
    protected final Player player;
    protected final ServerLevel level;
    protected final ScriptExecutionService exec;
    protected final ScriptTaskScheduler scheduler;
    protected final String scriptId;

    @HostAccess.Export
    public final PlayerAPI playerApi;
    @HostAccess.Export
    public final WorldAPI world;
    @HostAccess.Export
    public final StateAPI state;
    @HostAccess.Export
    public final QuestAPI quest;
    @HostAccess.Export
    public final DialogAPI dialog;
    @HostAccess.Export
    public final MorphAPI morphs;
    @HostAccess.Export
    public final EffectAPI effect;
    @HostAccess.Export
    public final ScriptControlAPI scriptControl;
    @HostAccess.Export
    public final States states;
    @HostAccess.Export
    public final ServerAPI server;
    @HostAccess.Export
    public final FileAPI file;
    @HostAccess.Export
    public final SchematicAPI schematic;

    public ScriptAPI(Player player, ServerLevel level, ScriptExecutionService exec, ScriptTaskScheduler scheduler, String scriptId) {
        this.player = player;
        this.level = level;
        this.exec = exec;
        this.scheduler = scheduler;
        this.scriptId = scriptId;
        this.playerApi = new PlayerAPI(this);
        this.world = new WorldAPI(this);
        this.state = new StateAPI(this);
        this.quest = new QuestAPI(this);
        this.dialog = new DialogAPI(this);
        this.morphs = new MorphAPI(this);
        this.effect = new EffectAPI(this);
        this.scriptControl = new ScriptControlAPI(this);
        this.states = new States();
        this.server = new ServerAPI(this);
        this.file = new FileAPI(this);
        this.schematic = new SchematicAPI(this);
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
        playerApi.sendMessage(text);
    }

    @HostAccess.Export
    public void sendMessage(String text, String color) {
        playerApi.sendMessage(text, color);
    }

    @HostAccess.Export
    public void giveItem(String itemId, int count) {
        playerApi.giveItem(itemId, count);
    }

    @HostAccess.Export
    public void teleport(double x, double y, double z) {
        playerApi.teleport(x, y, z);
    }

    @HostAccess.Export
    public void runCommand(String command) {
        world.runCommand(command);
    }

    @HostAccess.Export
    public double getX() {
        return playerApi.getX();
    }

    @HostAccess.Export
    public double getY() {
        return playerApi.getY();
    }

    @HostAccess.Export
    public double getZ() {
        return playerApi.getZ();
    }

    @HostAccess.Export
    public String getName() {
        return playerApi.getName();
    }

    @HostAccess.Export
    public void setTimeout(String script, int delayMs) {
        scriptControl.setTimeout(script, delayMs);
    }

    @HostAccess.Export
    public void setInterval(String script, int delayMs) {
        scriptControl.setInterval(script, delayMs);
    }

    @HostAccess.Export
    public void setData(String key, String value) {
        playerApi.setData(key, value);
    }

    @HostAccess.Export
    public String getData(String key) {
        return playerApi.getData(key);
    }

    @HostAccess.Export
    public void setIntData(String key, int value) {
        playerApi.setIntData(key, value);
    }

    @HostAccess.Export
    public int getIntData(String key) {
        return playerApi.getIntData(key);
    }

    @HostAccess.Export
    public void setFaction(String faction) {
        playerApi.setFaction(faction);
    }

    @HostAccess.Export
    public String getFaction() {
        return playerApi.getFaction();
    }

    @HostAccess.Export
    public void setReputation(int value) {
        playerApi.setReputation(value);
    }

    @HostAccess.Export
    public int getReputation() {
        return playerApi.getReputation();
    }

    @HostAccess.Export
    public void spawnEntity(String type, double x, double y, double z) {
        world.spawnEntity(type, x, y, z);
    }

    @HostAccess.Export
    public void setBlock(String blockId, double x, double y, double z) {
        world.setBlock(blockId, x, y, z);
    }

    @HostAccess.Export
    public String getBlock(double x, double y, double z) {
        return world.getBlock(x, y, z);
    }

    @HostAccess.Export
    public void playSound(String soundId, double x, double y, double z) {
        world.playSound(soundId, x, y, z);
    }

    @HostAccess.Export
    public void particle(String particleId, double x, double y, double z) {
        world.particle(particleId, x, y, z);
    }

    @HostAccess.Export
    public void setHealth(double hp) {
        playerApi.setHealth(hp);
    }

    @HostAccess.Export
    public double getHealth() {
        return playerApi.getHealth();
    }

    @HostAccess.Export
    public void setGamemode(String mode) {
        playerApi.setGamemode(mode);
    }

    @HostAccess.Export
    public boolean hasItem(String itemId, int count) {
        return playerApi.hasItem(itemId, count);
    }

    @HostAccess.Export
    public void clearInventory() {
        playerApi.clearInventory();
    }

    @HostAccess.Export
    public void cameraMove(double x, double y, double z, float yaw, float pitch, int durationTicks) {
        playerApi.cameraMove(x, y, z, yaw, pitch, durationTicks);
    }

    @HostAccess.Export
    public void cameraReset() {
        playerApi.cameraReset();
    }

    @HostAccess.Export
    public void cameraReset(Player target) {
        playerApi.cameraReset(target);
    }

    @HostAccess.Export
    public void dialogOpen(String dialogId) {
        dialog.open(dialogId);
    }

    @HostAccess.Export
    public void questStart(String questId) {
        quest.start(questId);
    }

    @HostAccess.Export
    public void questComplete(String questId) {
        quest.complete(questId);
    }

    @HostAccess.Export
    public void npcMove(int entityId, double x, double y, double z) {
        world.npcMove(entityId, x, y, z);
    }

    @HostAccess.Export
    public void scriptGraphRun(String graphId) {
        scriptControl.scriptGraphRun(graphId);
    }

    @HostAccess.Export
    public void setState(String key, Object value) {
        state.set(key, value);
    }

    @HostAccess.Export
    public String getStateString(String key) {
        return state.getString(key);
    }

    @HostAccess.Export
    public double getStateNumber(String key) {
        return state.getNumber(key);
    }

    @HostAccess.Export
    public boolean hasState(String key) {
        return state.has(key);
    }

    @HostAccess.Export
    public void addState(String key, double delta) {
        state.add(key, delta);
    }

    @HostAccess.Export
    public void incrementState(String key, double delta) {
        state.increment(key, delta);
    }

    @HostAccess.Export
    public void removeState(String key) {
        state.remove(key);
    }

    @HostAccess.Export
    public void log(String message) {
        effect.log(message);
    }

    @HostAccess.Export
    public void logInfo(String message) {
        effect.logInfo(message);
    }

    @HostAccess.Export
    public void logWarn(String message) {
        effect.logWarn(message);
    }

    @HostAccess.Export
    public void logError(String message) {
        effect.logError(message);
    }

    @HostAccess.Export
    public void logDebug(String message) {
        effect.logDebug(message);
    }

    @HostAccess.Export
    public void morph(String modelId) {
        morphs.morph(modelId);
    }

    @HostAccess.Export
    public void morphReset() {
        morphs.reset();
    }

    @HostAccess.Export
    public void morphScale(float scale) {
        morphs.scale(scale);
    }

    @HostAccess.Export
    public void playAnim(String animationName) {
        morphs.playAnim(animationName);
    }

    @HostAccess.Export
    public boolean isMorphed() {
        return morphs.isMorphed();
    }

    @HostAccess.Export
    public String getMorphId() {
        return morphs.getId();
    }

    @HostAccess.Export
    public void execute(String targetScriptId) {
        scriptControl.execute(targetScriptId);
    }

    @HostAccess.Export
    public void execute(String targetScriptId, String functionName) {
        scriptControl.execute(targetScriptId, functionName);
    }

    @HostAccess.Export
    public void execute(String targetScriptId, int delayTicks) {
        scriptControl.execute(targetScriptId, delayTicks);
    }

    @HostAccess.Export
    public void execute(String targetScriptId, String functionName, int delayTicks) {
        scriptControl.execute(targetScriptId, functionName, delayTicks);
    }

    @HostAccess.Export
    public boolean scriptExists(String targetScriptId) {
        return scriptControl.scriptExists(targetScriptId);
    }

    @HostAccess.Export
    public java.util.List<String> getScriptIds() {
        return scriptControl.getScriptIds();
    }

    @HostAccess.Export
    public EntityAPI entity(Entity entity) {
        return new EntityAPI(entity);
    }

    @HostAccess.Export
    public NpcAPI npc(Entity entity) {
        if (entity instanceof com.iscript.imson.entity.IScriptNPCEntity npc) {
            return new NpcAPI(npc);
        }
        return null;
    }

    @HostAccess.Export
    public ItemAPI item(ItemStack stack) {
        return new ItemAPI(stack);
    }

    @HostAccess.Export
    public InventoryAPI inventory(Player player) {
        return new InventoryAPI(player);
    }

    @HostAccess.Export
    public int getGameMode() {
        return playerApi.getGameMode();
    }

    @HostAccess.Export
    public void setGameMode(int mode) {
        playerApi.setGameMode(mode);
    }

    @HostAccess.Export
    public int getHotbarIndex() {
        return playerApi.getHotbarIndex();
    }

    @HostAccess.Export
    public void setHotbarIndex(int index) {
        playerApi.setHotbarIndex(index);
    }

    @HostAccess.Export
    public int getXpLevel() {
        return playerApi.getXpLevel();
    }

    @HostAccess.Export
    public void setXpLevel(int lvl) {
        playerApi.setXpLevel(lvl);
    }

    @HostAccess.Export
    public void addXp(int amount) {
        playerApi.addXp(amount);
    }

    @HostAccess.Export
    public void removeXp(int amount) {
        playerApi.removeXp(amount);
    }

    @HostAccess.Export
    public void setXp(int amount) {
        playerApi.setXp(amount);
    }

    @HostAccess.Export
    public void setSpawnPoint(double x, double y, double z) {
        playerApi.setSpawnPoint(x, y, z);
    }

    @HostAccess.Export
    public void sendTitle(String text) {
        playerApi.sendTitle(text);
    }

    @HostAccess.Export
    public void sendSubtitle(String text) {
        playerApi.sendSubtitle(text);
    }

    @HostAccess.Export
    public void sendActionBar(String text) {
        playerApi.sendActionBar(text);
    }

    @HostAccess.Export
    public void stopSound(String soundId) {
        playerApi.stopSound(soundId);
    }

    @HostAccess.Export
    public void stopAllSound() {
        playerApi.stopAllSound();
    }

    @HostAccess.Export
    public boolean isPlayer() {
        return playerApi.isPlayer();
    }

    @HostAccess.Export
    public ItemStack getMainItem() {
        return playerApi.getMainItem();
    }

    @HostAccess.Export
    public void setMainItem(String itemId) {
        playerApi.setMainItem(itemId);
    }

    @HostAccess.Export
    public InventoryAPI getInventory() {
        return new InventoryAPI(player);
    }

    public class States {
        @HostAccess.Export
        public String getString(String key) {
            return GlobalStates.get().getString(key);
        }

        @HostAccess.Export
        public double getNumber(String key) {
            return GlobalStates.get().getNumber(key);
        }

        @HostAccess.Export
        public void setString(String key, String value) {
            GlobalStates.get().setString(key, value);
            GlobalStates.save();
            ModData.setDirty();
        }

        @HostAccess.Export
        public void setNumber(String key, double value) {
            GlobalStates.get().setNumber(key, value);
            GlobalStates.save();
            ModData.setDirty();
        }

        @HostAccess.Export
        public boolean has(String key) {
            return GlobalStates.get().has(key);
        }

        @HostAccess.Export
        public void add(String key, double delta) {
            GlobalStates.get().add(key, delta);
            GlobalStates.save();
            ModData.setDirty();
        }

        @HostAccess.Export
        public void remove(String key) {
            GlobalStates.get().remove(key);
            GlobalStates.save();
            ModData.setDirty();
        }
    }
}