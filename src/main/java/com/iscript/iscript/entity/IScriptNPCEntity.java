package com.iscript.iscript.entity;

import com.iscript.iscript.capability.ModCapabilities;
import com.iscript.iscript.data.DialogManager;
import com.iscript.iscript.data.QuestManager;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.npc.NPCState;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.quest.QuestObjective;
import com.iscript.iscript.data.quest.QuestObjectiveType;
import com.iscript.iscript.data.quest.QuestProgress;
import com.iscript.iscript.data.quest.QuestReward;
import com.iscript.iscript.data.quest.QuestStage;
import com.iscript.iscript.data.quest.QuestStatus;
import com.iscript.iscript.data.PlayerQuestData;
import com.iscript.iscript.entity.ai.*;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.SyncDataPacket;
import com.iscript.iscript.network.packet.OpenGuiPacket;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

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

    private String ownerUUID = "";
    private NPCData cachedData = new NPCData();
    private UUID followTargetUUID = null;
    private int lastDamageTime = 0;
    private int stateTick = 0;
    public int attackCooldown = 0;
    private boolean wasKilled = false;

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
    }

    @Override
    protected void registerGoals() {
    }

    public void rebuildAI() {
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
        this.setCustomName(Component.literal(data.getName()));
        this.setCustomNameVisible(data.isNameVisible());
        this.setGlowingTag(data.isGlowEnabled());
        this.refreshDimensions();
    }

    public NPCData getNPCData() {
        return cachedData;
    }

    public String getSkin() {
        return this.entityData.get(DATA_SKIN);
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
            for (ServerPlayer player : ((ServerLevel) this.level()).getPlayers(p -> true)) {
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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (cachedData == null) return super.hurt(source, amount);

        if (!cachedData.isCanFallDamage() && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (!cachedData.isCanGetBurned() && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }

        if (cachedData.isInvulnerable()) {
            boolean bypass = source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
            boolean creativeAttacker = source.getEntity() instanceof ServerPlayer sp && sp.isCreative();
            if (!bypass && !creativeAttacker) {
                return false;
            }
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

        if (player.isShiftKeyDown()) {
            NPCData data = getNPCData();
            IScriptNetwork.sendToPlayer(
                    new OpenGuiPacket(OpenGuiPacket.Type.NPC_EDIT, OpenGuiPacket.npcEditToTag(this.getId(), data)),
                    (ServerPlayer) player
            );
            return InteractionResult.SUCCESS;
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

        Map<String, QuestProgress> playerQuests = QuestManager.getPlayerQuests(level, playerId);
        for (QuestData quest : QuestManager.getAll(level).values()) {
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

        super.tick();

        if (!this.level().isClientSide) {
            stateTick++;
            if (attackCooldown > 0) attackCooldown--;
            updateRegen();
            updateStateMachine();
        }
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
                List<Player> nearby = this.level().getEntitiesOfClass(Player.class,
                        new AABB(this.blockPosition()).inflate(8.0));
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

        CompoundTag tradeTag = new CompoundTag();
        data.getTradeData().save(tradeTag);
        tag.put("TradeData", tradeTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        NPCData data = new NPCData();

        data.setId(tag.contains("NPCId") ? tag.getString("NPCId") : "");
        this.entityData.set(DATA_NPC_NAME, tag.getString("NPCName"));
        this.entityData.set(DATA_DIALOG, tag.getString("NPCDialog"));
        this.entityData.set(DATA_SKIN, tag.getString("NPCSkin"));
        this.entityData.set(DATA_FACTION, tag.getString("NPCFaction"));
        this.entityData.set(DATA_ANIMATION, tag.getString("NPCAnimation"));
        this.entityData.set(DATA_HEALTH, tag.getFloat("NPCHealth"));
        this.entityData.set(DATA_MAX_HEALTH, tag.getFloat("NPCMaxHealth"));
        this.entityData.set(DATA_SCALE, tag.contains("NPCScale") ? tag.getFloat("NPCScale") : 1.0f);
        this.entityData.set(DATA_NAME_VISIBLE, tag.contains("NPCNameVisible") ? tag.getBoolean("NPCNameVisible") : true);
        this.entityData.set(DATA_GLOW_ENABLED, tag.contains("NPCGlowEnabled") ? tag.getBoolean("NPCGlowEnabled") : false);
        this.ownerUUID = tag.getString("OwnerUUID");
        this.wasKilled = tag.contains("NPCKilled") && tag.getBoolean("NPCKilled");

        try {
            data.setState(NPCState.valueOf(tag.getString("NPCState")));
        } catch (IllegalArgumentException e) {
            data.setState(NPCState.IDLE);
        }

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

        if (tag.contains("TradeData")) {
            data.getTradeData().load(tag.getCompound("TradeData"));
        }

        float loadedHealth = this.entityData.get(DATA_HEALTH);
        float loadedMaxHealth = this.entityData.get(DATA_MAX_HEALTH);
        data.setMaxHealth(loadedMaxHealth);
        data.setHealth(Math.min(loadedHealth, loadedMaxHealth));

        this.setCustomName(Component.literal(this.entityData.get(DATA_NPC_NAME)));
        this.setCustomNameVisible(this.entityData.get(DATA_NAME_VISIBLE));

        this.cachedData = data;
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