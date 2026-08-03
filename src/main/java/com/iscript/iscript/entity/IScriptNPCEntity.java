package com.iscript.iscript.entity;

import com.iscript.iscript.capability.ModCapabilities;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.npc.NPCState;
import com.iscript.iscript.data.npc.NPCTriggerData;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.quest.QuestObjective;
import com.iscript.iscript.data.quest.QuestObjectiveType;
import com.iscript.iscript.data.quest.QuestProgress;
import com.iscript.iscript.data.quest.QuestReward;
import com.iscript.iscript.data.quest.QuestStage;
import com.iscript.iscript.data.quest.QuestStatus;
import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.data.PlayerQuestData;
import com.iscript.iscript.entity.ai.*;
import com.iscript.iscript.item.NPCSpawnerItem;
import com.iscript.iscript.morph.MorphData;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.SyncDataPacket;
import com.iscript.iscript.network.packet.OpenGuiPacket;
import com.iscript.iscript.script.ScriptEngine;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IScriptNPCEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_NPC_NAME = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_DIALOG = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_FACTION = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ANIMATION = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAX_HEALTH = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_NAME_VISIBLE = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GLOW_ENABLED = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_MORPH_MODEL = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.STRING);

    private String ownerUUID = "";
    private NPCData cachedData = new NPCData();
    private UUID followTargetUUID = null;
    private int lastDamageTime = 0;
    private int stateTick = 0;
    public int attackCooldown = 0;
    private boolean wasKilled = false;
    private boolean aiDirty = false;
    private boolean wasWalking = false;
    private boolean morphDirty = false;

    public IScriptNPCEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_NPC_NAME, "NPC");
        this.entityData.define(DATA_DIALOG, "");
        this.entityData.define(DATA_SKIN, "");
        this.entityData.define(DATA_FACTION, "neutral");
        this.entityData.define(DATA_ANIMATION, "");
        this.entityData.define(DATA_HEALTH, 20.0f);
        this.entityData.define(DATA_MAX_HEALTH, 20.0f);
        this.entityData.define(DATA_SCALE, 1.0f);
        this.entityData.define(DATA_NAME_VISIBLE, true);
        this.entityData.define(DATA_GLOW_ENABLED, false);
        this.entityData.define(DATA_MORPH_MODEL, "");
    }

    @Override
    protected void registerGoals() {
    }

    public void rebuildAI() {
        this.aiDirty = true;
    }

    private void doRebuildAI() {
        if (this.cachedData == null) return;
        this.goalSelector.removeAllGoals(g -> true);
        this.targetSelector.removeAllGoals(g -> true);
        if (cachedData.isNoAI()) return;
        NPCData data = cachedData;
        double speed = data.getMovementSpeed();
        if (data.isCanSwim()) {
            this.goalSelector.addGoal(0, new NPCSwimGoal(this));
        }
        if (data.getAttackDamage() > 0) {
            this.goalSelector.addGoal(4, new NPCAttackMeleeGoal(this, speed, false));
        }
        if (!data.getFollowTarget().isEmpty()) {
            this.goalSelector.addGoal(6, new NPCFollowGoal(this, speed));
        } else if (data.isHasPost() && data.getPostPosition() != null) {
            this.goalSelector.addGoal(6, new NPCReturnToPostGoal(this, speed));
        } else if (!data.getPatrolPoints().isEmpty()) {
            this.goalSelector.addGoal(6, new NPCPatrolGoal(this, speed));
        }
        if (data.isLookAround()) {
            this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        }
        if (data.isLookAtPlayer()) {
            this.goalSelector.addGoal(9, new NPCLookAtPlayerGoal(this, data.getPathDistance()));
        }
        if (data.isWander()) {
            this.goalSelector.addGoal(9, new NPCWanderGoal(this, speed * 0.5));
        }
        if (data.isAlwaysWander()) {
            this.goalSelector.addGoal(10, new NPCAlwaysWanderGoal(this, speed * 0.5));
        }
        if (data.isAggressive() || !data.getHostileFactions().isEmpty()) {
            this.targetSelector.addGoal(2, new NPCFactionAttackGoal(this));
        }
    }

    private void applyMorphCapability(NPCData data) {
        if (data == null) return;
        String morphId = data.getMorphModelId();
        float morphScale = data.getScale();
        if (morphId != null && !morphId.isEmpty()) {
            this.getCapability(MorphData.CAPABILITY).ifPresent(morph -> {
                morph.setMorphed(true);
                morph.setModelId(morphId);
                morph.setTextureId(morphId);
                morph.setScale(morphScale);
                morph.setVisible(true);
            });
        } else {
            this.getCapability(MorphData.CAPABILITY).ifPresent(morph -> morph.setMorphed(false));
        }
    }

    public void setNPCData(NPCData data) {
        if (data == null) data = new NPCData();
        this.cachedData = data;
        this.entityData.set(DATA_NPC_NAME, data.getName());
        this.entityData.set(DATA_DIALOG, data.getDialogId());
        this.entityData.set(DATA_SKIN, data.getSkin());
        this.entityData.set(DATA_FACTION, data.getFaction());
        this.entityData.set(DATA_ANIMATION, data.getAnimation());
        this.entityData.set(DATA_HEALTH, data.getHealth());
        this.entityData.set(DATA_MAX_HEALTH, data.getMaxHealth());
        this.entityData.set(DATA_SCALE, data.getScale());
        this.entityData.set(DATA_NAME_VISIBLE, data.isNameVisible());
        this.entityData.set(DATA_GLOW_ENABLED, data.isGlowEnabled());
        this.entityData.set(DATA_MORPH_MODEL, data.getMorphModelId());
        float maxHp = Math.max(data.getMaxHealth(), 1.0f);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHp);
        this.setHealth(Math.min(data.getHealth(), maxHp));
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(data.getAttackDamage());
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(data.getMovementSpeed());
        this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(data.getPathDistance());
        this.setCustomName(Component.literal(data.getName()));
        this.setCustomNameVisible(data.isNameVisible());
        if (data.getName() == null || data.getName().isEmpty()) {
            this.setCustomName(Component.literal("NPC"));
        }
        this.setGlowingTag(data.isGlowEnabled());
        this.setNoAi(data.isNoAI());
        this.setInvulnerable(data.isInvulnerable());
        this.setSilent(data.isSilent());
        this.setNoGravity(!data.isHasGravity());
        this.setCanPickUpLoot(false);
        applyMorphCapability(data);
        this.refreshDimensions();
        rebuildAI();
        if (!this.level().isClientSide) {
            syncToTrackingPlayers();
        }
    }

    private void syncToTrackingPlayers() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        SyncDataPacket packet = new SyncDataPacket(SyncDataPacket.Type.NPC_DATA, SyncDataPacket.npcDataToTag(this.getId(), getNPCData()));
        for (ServerPlayer player : sl.getPlayers(p -> p.distanceToSqr(this) < 4096)) {
            IScriptNetwork.sendToPlayer(packet, player);
        }
    }

    public void applyNPCDataClient(NPCData data) {
        if (data == null) return;
        this.cachedData = data;
        this.entityData.set(DATA_NPC_NAME, data.getName());
        this.entityData.set(DATA_DIALOG, data.getDialogId());
        this.entityData.set(DATA_SKIN, data.getSkin());
        this.entityData.set(DATA_FACTION, data.getFaction());
        this.entityData.set(DATA_ANIMATION, data.getAnimation());
        this.entityData.set(DATA_HEALTH, data.getHealth());
        this.entityData.set(DATA_MAX_HEALTH, data.getMaxHealth());
        this.entityData.set(DATA_SCALE, data.getScale());
        this.entityData.set(DATA_NAME_VISIBLE, data.isNameVisible());
        this.entityData.set(DATA_GLOW_ENABLED, data.isGlowEnabled());
        this.entityData.set(DATA_MORPH_MODEL, data.getMorphModelId());
        this.setCustomName(Component.literal(data.getName()));
        this.setCustomNameVisible(data.isNameVisible());
        this.setGlowingTag(data.isGlowEnabled());
        applyMorphCapability(data);
        this.refreshDimensions();
    }

    public NPCData getNPCData() {
        return cachedData;
    }

    public String getSkin() {
        return this.entityData.get(DATA_SKIN);
    }

    public String getMorphModelId() {
        return this.entityData.get(DATA_MORPH_MODEL);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(DATA_MORPH_MODEL)) {
            String modelId = this.entityData.get(DATA_MORPH_MODEL);
            if (cachedData != null) {
                cachedData.setMorphModelId(modelId);
            }
            if (this.getCapability(MorphData.CAPABILITY).isPresent()) {
                applyMorphCapability(cachedData);
            } else {
                morphDirty = true;
            }
        }
    }

    @Override
    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    @Override
    public boolean isCustomNameVisible() {
        return this.entityData.get(DATA_NAME_VISIBLE);
    }

    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID().toString();
    }

    public NPCState getCurrentState() { return cachedData != null ? cachedData.getState() : NPCState.IDLE; }
    public void setCurrentState(NPCState state) {
        if (cachedData != null) cachedData.setState(state);
    }
    public String getFaction() { return this.entityData.get(DATA_FACTION); }

    public void playAnimation(String animation) {
        this.entityData.set(DATA_ANIMATION, animation);
        if (cachedData != null) cachedData.setAnimation(animation);
        if (!this.level().isClientSide) {
            SyncDataPacket packet = new SyncDataPacket(SyncDataPacket.Type.NPC_DATA, SyncDataPacket.npcDataToTag(this.getId(), getNPCData()));
            for (ServerPlayer player : ((ServerLevel) this.level()).getPlayers(p -> p.distanceToSqr(this) < 4096)) {
                IScriptNetwork.sendToPlayer(packet, player);
            }
        }
    }

    public void setFollowTarget(Player player) {
        this.followTargetUUID = player != null ? player.getUUID() : null;
        if (cachedData == null) return;
        if (player != null) {
            cachedData.setState(NPCState.FOLLOW);
            cachedData.setFollowTarget(player.getName().getString());
        } else if (cachedData.getState() == NPCState.FOLLOW) {
            cachedData.setState(NPCState.IDLE);
            cachedData.setFollowTarget("");
        }
        rebuildAI();
    }

    private void executeTrigger(NPCTriggerData trigger, Player player) {
        if (!trigger.isEnabled()) return;
        String val = trigger.getActionValue();
        switch (trigger.getActionType()) {
            case SCRIPT -> {
                ScriptEngine engine = ScriptEngine.getInstance();
                if (engine != null && engine.isAvailable()) {
                    engine.runScriptMain(val, player, (ServerLevel) level());
                }
            }
            case COMMAND -> {
                if (level().getServer() != null) {
                    CommandSourceStack source;
                    if (player != null) {
                        source = player.createCommandSourceStack().withLevel((ServerLevel) level()).withPosition(position());
                    } else {
                        source = new CommandSourceStack(CommandSource.NULL, position(), Vec2.ZERO, (ServerLevel) level(), 2, "NPC", Component.literal("NPC"), level().getServer(), null);
                    }
                    level().getServer().getCommands().performPrefixedCommand(source, val.replace("@p", player != null ? player.getGameProfile().getName() : "@p"));
                }
            }
            case SOUND -> {
                ResourceLocation rl = new ResourceLocation(val);
                SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(rl);
                if (se != null) level().playSound(null, getX(), getY(), getZ(), se, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
            case DIALOG -> {
                if (player instanceof ServerPlayer sp) {
                    DialogData dialog = DataAccess.dialog(val);
                    if (dialog != null) {
                        IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG, OpenGuiPacket.dialogToTag(dialog)), sp);
                    }
                }
            }
            case TRADE -> {
                if (player instanceof ServerPlayer sp) {
                    IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.NPC_TRADE, OpenGuiPacket.npcTradeToTag(getId(), getNPCData().getTradeData())), sp);
                }
            }
            case EVENT -> {
                if (player instanceof ServerPlayer sp) {
                    com.iscript.iscript.data.Graph graph = DataAccess.event(val);
                    if (graph != null) {
                        executeGraph(graph, sp);
                    }
                }
            }
        }
    }

    private void executeGraph(com.iscript.iscript.data.Graph graph, ServerPlayer player) {
        String startId = graph.getStartNodeId();
        if (startId == null || startId.isEmpty()) return;
        com.iscript.iscript.data.Node node = graph.getNode(startId);
        if (node == null) return;
        com.iscript.iscript.script.ScriptEngine engine = com.iscript.iscript.script.ScriptEngine.getInstance();
        if (node.getType().equals("SCRIPT_JS")) {
            String script = node.getParam("script");
            if (script != null && !script.isEmpty() && engine != null && engine.isAvailable()) {
                engine.execute(graph.getId(), script, player, player.serverLevel());
            }
        } else if (node.getType().equals("START") || node.getType().equals("TRIGGER")) {
            String eventTypeStr = node.getParam("eventType");
            if (eventTypeStr != null && !eventTypeStr.isEmpty()) {
                try {
                    com.iscript.iscript.event.EventType type = com.iscript.iscript.event.EventType.valueOf(eventTypeStr);
                    com.iscript.iscript.event.EventManager.trigger(type, player, player.serverLevel());
                } catch (IllegalArgumentException ignored) {}
            }
        }
        for (com.iscript.iscript.data.Node.Connection conn : node.getConnections()) {
            com.iscript.iscript.data.Node next = graph.getNode(conn.getTarget());
            if (next != null) executeGraphNode(next, graph, player);
        }
    }

    private void executeGraphNode(com.iscript.iscript.data.Node node, com.iscript.iscript.data.Graph graph, ServerPlayer player) {
        com.iscript.iscript.script.ScriptEngine engine = com.iscript.iscript.script.ScriptEngine.getInstance();
        if (node.getType().equals("SCRIPT_JS")) {
            String script = node.getParam("script");
            if (script != null && !script.isEmpty() && engine != null && engine.isAvailable()) {
                engine.execute(graph.getId(), script, player, player.serverLevel());
            }
        } else if (node.getType().equals("STOP")) {
            return;
        }
        for (com.iscript.iscript.data.Node.Connection conn : node.getConnections()) {
            com.iscript.iscript.data.Node next = graph.getNode(conn.getTarget());
            if (next != null) executeGraphNode(next, graph, player);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (cachedData == null) return super.hurt(source, amount);
        if (!cachedData.isCanFallDamage() && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return false;
        if (!cachedData.isCanGetBurned() && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return false;
        if (cachedData.isInvulnerable()) {
            boolean bypass = source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
            boolean creativeAttacker = source.getEntity() instanceof ServerPlayer sp && sp.isCreative();
            if (!bypass && !creativeAttacker) return false;
        }
        if (!cachedData.isKillable() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            float newHealth = this.getHealth() - amount;
            if (newHealth <= 0) {
                boolean result = super.hurt(source, amount);
                if (result) {
                    this.setHealth(0.001f);
                    this.entityData.set(DATA_HEALTH, 0.001f);
                    cachedData.setHealth(0.001f);
                }
                return result;
            }
        }
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            this.lastDamageTime = 0;
            float currentHealth = this.getHealth();
            this.entityData.set(DATA_HEALTH, currentHealth);
            cachedData.setHealth(currentHealth);
            if (source.getEntity() instanceof Player attacker) {
                String attackerFaction = attacker.getCapability(ModCapabilities.PLAYER_DATA)
                        .map(com.iscript.iscript.capability.PlayerData::getFaction).orElse("neutral");
                if (cachedData.isHostileTo(attackerFaction)) {
                    cachedData.setState(NPCState.ATTACK);
                    rebuildAI();
                }
            }
            for (NPCTriggerData trigger : cachedData.getTriggers()) {
                if (trigger.getTriggerType() == NPCTriggerData.TriggerType.HURT) {
                    executeTrigger(trigger, source.getEntity() instanceof Player p ? p : null);
                }
            }
        }
        return result;
    }

    @Override
    public void heal(float amount) {
        super.heal(amount);
        if (!this.level().isClientSide && cachedData != null) {
            float currentHealth = this.getHealth();
            this.entityData.set(DATA_HEALTH, currentHealth);
            cachedData.setHealth(currentHealth);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (cachedData != null && !cachedData.isKillable() && this.getHealth() <= 0) {
            this.setHealth(0.001f);
            return;
        }
        super.die(source);
        wasKilled = true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof NPCSpawnerItem) {
            NPCData data = getNPCData();
            if (data.getId() == null || data.getId().isEmpty()) {
                data.setId("npc_" + this.getUUID().toString().replace("-", "").substring(0, 8));
                this.cachedData = data;
            } else {
                NPCData fresh = DataAccess.npc(data.getId());
                if (fresh != null) {
                    data = fresh;
                    this.cachedData = fresh;
                }
            }
            IScriptNetwork.sendToPlayer(
                    new OpenGuiPacket(OpenGuiPacket.Type.NPC_EDIT, OpenGuiPacket.npcEditToTag(this.getId(), data)),
                    (ServerPlayer) player
            );
            return InteractionResult.SUCCESS;
        }
        if (cachedData != null) {
            for (NPCTriggerData trigger : cachedData.getTriggers()) {
                if (trigger.getTriggerType() == NPCTriggerData.TriggerType.INTERACT) {
                    executeTrigger(trigger, player);
                }
            }
        }
        if (cachedData != null && cachedData.isEnableTrade() && cachedData.getState() == NPCState.TRADE) {
            IScriptNetwork.sendToPlayer(
                    new OpenGuiPacket(OpenGuiPacket.Type.NPC_TRADE, OpenGuiPacket.npcTradeToTag(this.getId(), cachedData.getTradeData())),
                    (ServerPlayer) player
            );
            return InteractionResult.SUCCESS;
        }
        String dialogId = this.entityData.get(DATA_DIALOG);
        if (!dialogId.isEmpty()) {
            handleDialogAndQuests(player, dialogId);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void handleDialogAndQuests(Player player, String dialogId) {
        UUID playerId = player.getUUID();
        ServerLevel level = (ServerLevel) this.level();
        Map<String, QuestProgress> playerQuests = DataAccess.playerQuests(level).getActive(playerId);
        for (QuestData quest : DataAccess.quests().values()) {
            QuestProgress progress = playerQuests.get(quest.getId());
            if (progress == null || progress.getStatus() != QuestStatus.ACTIVE) continue;
            QuestStage stage = progress.getCurrentStage();
            if (stage == null) continue;
            for (int i = 0; i < stage.getObjectives().size(); i++) {
                QuestObjective obj = stage.getObjectives().get(i);
                if (obj.getType() != QuestObjectiveType.TALK_TO) continue;
                if (obj.getCurrentCount() >= obj.getRequiredCount()) continue;
                if (obj.getTarget().equals(dialogId) || obj.getTarget().equals(this.entityData.get(DATA_NPC_NAME))) {
                    obj.setCurrentCount(obj.getCurrentCount() + 1);
                    checkStageCompletion(progress, stage, quest, player, level);
                    break;
                }
            }
        }
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
            IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.DIALOG, OpenGuiPacket.dialogToTag(filtered)), (ServerPlayer) player);
        }
    }

    private void checkStageCompletion(QuestProgress progress, QuestStage stage, QuestData quest, Player player, ServerLevel level) {
        boolean allComplete = true;
        for (QuestObjective o : stage.getObjectives()) {
            if (o.getCurrentCount() < o.getRequiredCount()) {
                allComplete = false;
                break;
            }
        }
        if (allComplete) {
            boolean wasLast = progress.advanceStage();
            if (wasLast) {
                QuestReward reward = quest.getReward();
                if (reward != null && reward.getCommand() != null && !reward.getCommand().isEmpty()) {
                    level.getServer().getCommands().performPrefixedCommand(
                            player.createCommandSourceStack().withLevel(level).withPosition(player.position()),
                            reward.getCommand().replace("@p", player.getGameProfile().getName())
                    );
                }
            }
            PlayerQuestData.get(level).setDirty();
        }
    }

    @Override
    public void tick() {
        if (cachedData != null && cachedData.isNoAI()) {
            super.tick();
            return;
        }
        if (aiDirty) {
            aiDirty = false;
            doRebuildAI();
        }
        super.tick();
        if (!this.level().isClientSide) {
            stateTick++;
            if (attackCooldown > 0) attackCooldown--;
            updateRegen();
            updateStateMachine();
            if (cachedData != null) {
                for (NPCTriggerData trigger : cachedData.getTriggers()) {
                    if (trigger.getTriggerType() == NPCTriggerData.TriggerType.TICK) {
                        executeTrigger(trigger, null);
                    }
                }
                boolean walking = getDeltaMovement().lengthSqr() > 0.001;
                if (walking && !wasWalking) {
                    for (NPCTriggerData trigger : cachedData.getTriggers()) {
                        if (trigger.getTriggerType() == NPCTriggerData.TriggerType.WALK) {
                            executeTrigger(trigger, null);
                        }
                    }
                }
                wasWalking = walking;
            }
        } else if (morphDirty && this.getCapability(MorphData.CAPABILITY).isPresent()) {
            morphDirty = false;
            applyMorphCapability(cachedData);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if (cachedData != null) {
            for (NPCTriggerData trigger : cachedData.getTriggers()) {
                if (trigger.getTriggerType() == NPCTriggerData.TriggerType.SPAWN) {
                    executeTrigger(trigger, null);
                }
            }
        }
        return result;
    }

    private void updateRegen() {
        if (cachedData == null) return;
        NPCData data = cachedData;
        if (data.getRegenDelay() <= 0) return;
        if (this.lastDamageTime < data.getRegenDelay()) {
            this.lastDamageTime++;
            return;
        }
        int freq = data.getRegenFrequency() <= 0 ? 1 : data.getRegenFrequency();
        if (this.tickCount % freq == 0) {
            if (this.getHealth() > 0 && this.getHealth() < this.getMaxHealth()) {
                this.heal(1.0f);
            }
        }
        this.lastDamageTime++;
    }

    private void updateStateMachine() {
        if (cachedData == null) return;
        NPCState state = cachedData.getState();
        switch (state) {
            case IDLE -> {
                if (cachedData.isAggressive()) {
                }
            }
            case SIT -> {
                this.setPos(this.getX(), Math.floor(this.getY()), this.getZ());
                this.getNavigation().stop();
            }
            case DANCE -> {
                this.getNavigation().stop();
                if (stateTick % 20 == 0) {
                    this.setYRot(this.getYRot() + 45);
                }
            }
            case TRADE -> {
                this.getNavigation().stop();
                List<Player> nearby = this.level().getEntitiesOfClass(Player.class, new AABB(this.blockPosition()).inflate(8.0));
                if (!nearby.isEmpty()) {
                    this.getLookControl().setLookAt(nearby.get(0));
                }
            }
            default -> {}
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        NPCData data = cachedData != null ? cachedData : new NPCData();
        tag.putString("NPCId", data.getId());
        tag.putString("NPCName", this.entityData.get(DATA_NPC_NAME));
        tag.putString("NPCDialog", this.entityData.get(DATA_DIALOG));
        tag.putString("NPCSkin", this.entityData.get(DATA_SKIN));
        tag.putString("NPCFaction", this.entityData.get(DATA_FACTION));
        tag.putString("NPCAnimation", this.entityData.get(DATA_ANIMATION));
        tag.putFloat("NPCHealth", this.entityData.get(DATA_HEALTH));
        tag.putFloat("NPCMaxHealth", this.entityData.get(DATA_MAX_HEALTH));
        tag.putFloat("NPCScale", this.entityData.get(DATA_SCALE));
        tag.putBoolean("NPCNameVisible", this.entityData.get(DATA_NAME_VISIBLE));
        tag.putBoolean("NPCGlowEnabled", this.entityData.get(DATA_GLOW_ENABLED));
        tag.putString("OwnerUUID", this.ownerUUID);
        tag.putBoolean("NPCKilled", this.wasKilled);
        tag.putString("NPCState", data.getState().name());
        ListTag statesTag = new ListTag();
        for (NPCState s : data.getStates()) {
            CompoundTag t = new CompoundTag();
            t.putString("State", s.name());
            statesTag.add(t);
        }
        tag.put("NPCStates", statesTag);
        tag.putBoolean("NPCAggressive", data.isAggressive());
        tag.putString("NPCHostileFactions", data.getHostileFactions());
        tag.putFloat("NPCAttackDamage", data.getAttackDamage());
        tag.putFloat("NPCMoveSpeed", data.getMovementSpeed());
        tag.putBoolean("NPCEnableTrade", data.isEnableTrade());
        tag.putBoolean("NPCNoAI", data.isNoAI());
        tag.putBoolean("NPCInvulnerable", data.isInvulnerable());
        tag.putBoolean("NPCSilent", data.isSilent());
        tag.putBoolean("NPCHasGravity", data.isHasGravity());
        tag.putBoolean("CanSwim", data.isCanSwim());
        tag.putBoolean("CanFly", data.isCanFly());
        tag.putBoolean("Immovable", data.isImmovable());
        tag.putBoolean("HasPost", data.isHasPost());
        if (data.getPostPosition() != null) {
            tag.put("PostPosition", net.minecraft.nbt.NbtUtils.writeBlockPos(data.getPostPosition()));
        }
        tag.putFloat("PostRadius", data.getPostRadius());
        tag.putFloat("Fallback", data.getFallback());
        tag.putBoolean("PatrolLoop", data.isPatrolLoop());
        ListTag patrolList = new ListTag();
        for (BlockPos pos : data.getPatrolPoints()) {
            patrolList.add(net.minecraft.nbt.NbtUtils.writeBlockPos(pos));
        }
        tag.put("PatrolPoints", patrolList);
        tag.putBoolean("LookAtPlayer", data.isLookAtPlayer());
        tag.putBoolean("LookAround", data.isLookAround());
        tag.putBoolean("Wander", data.isWander());
        tag.putBoolean("AlwaysWander", data.isAlwaysWander());
        tag.putInt("RegenDelay", data.getRegenDelay());
        tag.putInt("RegenFrequency", data.getRegenFrequency());
        tag.putInt("DamageDelay", data.getDamageDelay());
        tag.putFloat("PathDistance", data.getPathDistance());
        tag.putBoolean("CanFallDamage", data.isCanFallDamage());
        tag.putBoolean("CanGetBurned", data.isCanGetBurned());
        tag.putBoolean("Killable", data.isKillable());
        tag.putString("FollowTarget", data.getFollowTarget());
        tag.putString("MorphModelId", data.getMorphModelId());
        CompoundTag tradeTag = new CompoundTag();
        data.getTradeData().save(tradeTag);
        tag.put("TradeData", tradeTag);
        tag.put("Triggers", NPCTriggerData.saveList(data.getTriggers()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NPCData data = new NPCData();
        data.setId(tag.contains("NPCId") ? tag.getString("NPCId") : "");

        String morphModel = tag.contains("MorphModelId") ? tag.getString("MorphModelId") : "";
        float scale = tag.contains("NPCScale") ? tag.getFloat("NPCScale") : 1.0f;
        String name = tag.getString("NPCName");
        boolean nameVisible = tag.contains("NPCNameVisible") ? tag.getBoolean("NPCNameVisible") : true;
        boolean glow = tag.contains("NPCGlowEnabled") ? tag.getBoolean("NPCGlowEnabled") : false;

        this.entityData.set(DATA_NPC_NAME, name);
        this.entityData.set(DATA_DIALOG, tag.getString("NPCDialog"));
        this.entityData.set(DATA_SKIN, tag.getString("NPCSkin"));
        this.entityData.set(DATA_FACTION, tag.getString("NPCFaction"));
        this.entityData.set(DATA_ANIMATION, tag.getString("NPCAnimation"));
        this.entityData.set(DATA_HEALTH, tag.getFloat("NPCHealth"));
        this.entityData.set(DATA_MAX_HEALTH, tag.getFloat("NPCMaxHealth"));
        this.entityData.set(DATA_SCALE, scale);
        this.entityData.set(DATA_NAME_VISIBLE, nameVisible);
        this.entityData.set(DATA_GLOW_ENABLED, glow);
        this.entityData.set(DATA_MORPH_MODEL, morphModel);

        this.ownerUUID = tag.getString("OwnerUUID");
        this.wasKilled = tag.contains("NPCKilled") && tag.getBoolean("NPCKilled");
        try {
            data.setState(NPCState.valueOf(tag.getString("NPCState")));
        } catch (IllegalArgumentException e) {
            data.setState(NPCState.IDLE);
        }
        data.getStates().clear();
        if (tag.contains("NPCStates")) {
            ListTag list = tag.getList("NPCStates", 10);
            for (int i = 0; i < list.size(); i++) {
                try { data.getStates().add(NPCState.valueOf(list.getCompound(i).getString("State"))); } catch (Exception ignored) {}
            }
        }
        if (data.getStates().isEmpty()) data.getStates().add(data.getState());
        data.setName(name);
        data.setDialogId(tag.getString("NPCDialog"));
        data.setSkin(tag.getString("NPCSkin"));
        data.setFaction(tag.getString("NPCFaction"));
        data.setAnimation(tag.getString("NPCAnimation"));
        data.setScale(scale);
        data.setNameVisible(nameVisible);
        data.setGlowEnabled(glow);
        data.setAggressive(tag.getBoolean("NPCAggressive"));
        data.setHostileFactions(tag.getString("NPCHostileFactions"));
        data.setAttackDamage(tag.getFloat("NPCAttackDamage"));
        data.setMovementSpeed(tag.getFloat("NPCMoveSpeed"));
        data.setEnableTrade(tag.getBoolean("NPCEnableTrade"));
        data.setNoAI(tag.contains("NPCNoAI") ? tag.getBoolean("NPCNoAI") : false);
        data.setInvulnerable(tag.contains("NPCInvulnerable") ? tag.getBoolean("NPCInvulnerable") : false);
        data.setSilent(tag.contains("NPCSilent") ? tag.getBoolean("NPCSilent") : false);
        data.setHasGravity(tag.contains("NPCHasGravity") ? tag.getBoolean("NPCHasGravity") : true);
        data.setCanSwim(tag.contains("CanSwim") ? tag.getBoolean("CanSwim") : false);
        data.setCanFly(tag.contains("CanFly") ? tag.getBoolean("CanFly") : false);
        data.setImmovable(tag.contains("Immovable") ? tag.getBoolean("Immovable") : false);
        data.setHasPost(tag.contains("HasPost") ? tag.getBoolean("HasPost") : false);
        data.setPostPosition(tag.contains("PostPosition") ? net.minecraft.nbt.NbtUtils.readBlockPos(tag.getCompound("PostPosition")) : null);
        data.setPostRadius(tag.contains("PostRadius") ? tag.getFloat("PostRadius") : 1.0f);
        data.setFallback(tag.contains("Fallback") ? tag.getFloat("Fallback") : 15.0f);
        data.setPatrolLoop(tag.contains("PatrolLoop") ? tag.getBoolean("PatrolLoop") : false);
        data.getPatrolPoints().clear();
        if (tag.contains("PatrolPoints")) {
            ListTag list = tag.getList("PatrolPoints", 10);
            for (int i = 0; i < list.size(); i++) {
                data.getPatrolPoints().add(net.minecraft.nbt.NbtUtils.readBlockPos(list.getCompound(i)));
            }
        }
        data.setLookAtPlayer(tag.contains("LookAtPlayer") ? tag.getBoolean("LookAtPlayer") : false);
        data.setLookAround(tag.contains("LookAround") ? tag.getBoolean("LookAround") : false);
        data.setWander(tag.contains("Wander") ? tag.getBoolean("Wander") : false);
        data.setAlwaysWander(tag.contains("AlwaysWander") ? tag.getBoolean("AlwaysWander") : false);
        data.setRegenDelay(tag.contains("RegenDelay") ? tag.getInt("RegenDelay") : 0);
        data.setRegenFrequency(tag.contains("RegenFrequency") ? tag.getInt("RegenFrequency") : 20);
        data.setDamageDelay(tag.contains("DamageDelay") ? tag.getInt("DamageDelay") : 20);
        data.setPathDistance(tag.contains("PathDistance") ? tag.getFloat("PathDistance") : 32.0f);
        data.setCanFallDamage(tag.contains("CanFallDamage") ? tag.getBoolean("CanFallDamage") : true);
        data.setCanGetBurned(tag.contains("CanGetBurned") ? tag.getBoolean("CanGetBurned") : true);
        data.setKillable(tag.contains("Killable") ? tag.getBoolean("Killable") : true);
        data.setFollowTarget(tag.contains("FollowTarget") ? tag.getString("FollowTarget") : "");
        data.setMorphModelId(morphModel);
        if (tag.contains("TradeData")) {
            data.getTradeData().load(tag.getCompound("TradeData"));
        }
        data.getTriggers().clear();
        if (tag.contains("Triggers")) {
            data.getTriggers().addAll(NPCTriggerData.loadList(tag.getList("Triggers", net.minecraft.nbt.Tag.TAG_COMPOUND)));
        }
        float loadedHealth = this.entityData.get(DATA_HEALTH);
        float loadedMaxHealth = this.entityData.get(DATA_MAX_HEALTH);
        data.setMaxHealth(loadedMaxHealth);
        data.setHealth(Math.min(loadedHealth, loadedMaxHealth));
        this.setCustomName(Component.literal(this.entityData.get(DATA_NPC_NAME)));
        this.setCustomNameVisible(this.entityData.get(DATA_NAME_VISIBLE));
        this.cachedData = data;
        applyMorphCapability(data);
        this.refreshDimensions();
        rebuildAI();
    }

    @Override
    public boolean isPushable() {
        return cachedData == null || !cachedData.isImmovable();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        if (cachedData != null && !cachedData.isCanFallDamage()) return false;
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    protected int decreaseAirSupply(int currentAir) {
        if (cachedData != null && !cachedData.isCanSwim()) return currentAir;
        return super.decreaseAirSupply(currentAir);
    }

    @Override
    public boolean fireImmune() {
        return cachedData != null && !cachedData.isCanGetBurned();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }
}