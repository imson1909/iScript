package com.iscript.iscript.entity;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.capability.ModCapabilities;
import com.iscript.iscript.data.DialogManager;
import com.iscript.iscript.data.QuestManager;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.data.npc.NPCState;
import com.iscript.iscript.data.npc.NPCWaypointData;
import com.iscript.iscript.data.quest.QuestData;
import com.iscript.iscript.data.quest.QuestObjectiveType;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.NPCAnimationPacket;
import com.iscript.iscript.network.packet.OpenDialogScreenPacket;
import com.iscript.iscript.network.packet.OpenNPCEditGuiPacket;
import com.iscript.iscript.network.packet.SyncNPCDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
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
    private static final EntityDataAccessor<Integer> DATA_GLOW_COLOR = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.INT);

    private String ownerUUID = "";
    private NPCWaypointData waypointData = new NPCWaypointData();
    private NPCData cachedData = new NPCData();
    private UUID followTargetUUID = null;
    private int stateTick = 0;
    private int attackCooldown = 0;
    private int sitTick = 0;
    private boolean isDancing = false;

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
        this.entityData.define(DATA_GLOW_COLOR, 0xFFFFFFFF);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new NPCFollowGoal(this, 1.2));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.4));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.targetSelector.addGoal(1, new NPCFactionAttackGoal(this));
    }

    public void setNPCData(NPCData data) {
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
        this.entityData.set(DATA_GLOW_COLOR, data.getGlowColor());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(data.getMaxHealth());
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(data.getAttackDamage());
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(data.getMovementSpeed());
        this.setHealth(data.getHealth());
        this.setCustomName(Component.literal(data.getName()));
        this.setCustomNameVisible(data.isNameVisible());
        this.setGlowingTag(data.isGlowEnabled());
        this.refreshDimensions();
        if (data.isNoAI()) {
            this.setNoAi(true);
        } else {
            this.setNoAi(false);
        }
        this.setInvulnerable(data.isInvulnerable());
        this.setSilent(data.isSilent());
        this.setNoGravity(!data.isHasGravity());
        if (!this.level().isClientSide) {
            IScriptNetwork.sendToAll(new SyncNPCDataPacket(this.getId(), data));
        }
    }

    public void applyNPCDataClient(NPCData data) {
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
        this.entityData.set(DATA_GLOW_COLOR, data.getGlowColor());
        this.setCustomName(Component.literal(data.getName()));
        this.setCustomNameVisible(data.isNameVisible());
        this.setGlowingTag(data.isGlowEnabled());
        this.refreshDimensions();
    }

    public NPCData getNPCData() {
        NPCData data = new NPCData();
        data.setName(this.entityData.get(DATA_NPC_NAME));
        data.setDialogId(this.entityData.get(DATA_DIALOG));
        data.setSkin(this.entityData.get(DATA_SKIN));
        data.setFaction(this.entityData.get(DATA_FACTION));
        data.setAnimation(this.entityData.get(DATA_ANIMATION));
        data.setHealth(this.entityData.get(DATA_HEALTH));
        data.setMaxHealth(this.entityData.get(DATA_MAX_HEALTH));
        data.setScale(this.entityData.get(DATA_SCALE));
        data.setNameVisible(this.entityData.get(DATA_NAME_VISIBLE));
        data.setGlowEnabled(this.entityData.get(DATA_GLOW_ENABLED));
        data.setGlowColor(this.entityData.get(DATA_GLOW_COLOR));
        data.setState(cachedData.getState());
        data.setAggressive(cachedData.isAggressive());
        data.setHostileFactions(cachedData.getHostileFactions());
        data.setAttackDamage(cachedData.getAttackDamage());
        data.setMovementSpeed(cachedData.getMovementSpeed());
        data.setEnableTrade(cachedData.isEnableTrade());
        data.setTradeData(cachedData.getTradeData());
        data.setNoAI(cachedData.isNoAI());
        data.setInvulnerable(cachedData.isInvulnerable());
        data.setSilent(cachedData.isSilent());
        data.setHasGravity(cachedData.isHasGravity());
        return data;
    }

    @Override
    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID().toString();
    }

    public NPCWaypointData getWaypointData() { return waypointData; }

    public NPCState getCurrentState() { return cachedData.getState(); }
    public void setCurrentState(NPCState state) { cachedData.setState(state); }
    public String getFaction() { return this.entityData.get(DATA_FACTION); }
    public float getNPCHealth() { return this.entityData.get(DATA_HEALTH); }
    public float getNPCMaxHealth() { return this.entityData.get(DATA_MAX_HEALTH); }
    public String getCurrentAnimation() { return this.entityData.get(DATA_ANIMATION); }

    public void playAnimation(String animation) {
        this.entityData.set(DATA_ANIMATION, animation);
        this.cachedData.setAnimation(animation);
        if (!this.level().isClientSide) {
            IScriptNetwork.sendToAll(new NPCAnimationPacket(this.getId(), animation));
        }
    }

    public void setFollowTarget(Player player) {
        this.followTargetUUID = player != null ? player.getUUID() : null;
        if (player != null) {
            cachedData.setState(NPCState.FOLLOW);
        } else if (cachedData.getState() == NPCState.FOLLOW) {
            cachedData.setState(NPCState.IDLE);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (cachedData.isInvulnerable() && source.getEntity() instanceof Player) return false;
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            float newHealth = this.getHealth();
            this.entityData.set(DATA_HEALTH, newHealth);
            this.cachedData.setHealth(newHealth);
            if (source.getEntity() instanceof Player attacker) {
                String attackerFaction = attacker.getCapability(ModCapabilities.PLAYER_DATA)
                        .map(com.iscript.iscript.capability.PlayerData::getFaction).orElse("neutral");
                if (cachedData.isHostileTo(attackerFaction)) {
                    cachedData.setState(NPCState.ATTACK);
                }
            }
        }
        return result;
    }

    @Override
    public void heal(float amount) {
        super.heal(amount);
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_HEALTH, this.getHealth());
            this.cachedData.setHealth(this.getHealth());
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown() && this.ownerUUID.equals(player.getUUID().toString())) {
            NPCData data = getNPCData();
            IScriptNetwork.sendToPlayer(
                    new OpenNPCEditGuiPacket(this.getId(), data.getName(), data.getDialogId(), data.getSkin(), data.getFaction()),
                    (ServerPlayer) player
            );
            return InteractionResult.SUCCESS;
        }

        if (cachedData.isEnableTrade() && cachedData.getState() == NPCState.TRADE) {
            IScriptNetwork.sendToPlayer(new com.iscript.iscript.network.packet.OpenNPCTradePacket(this.getId(), cachedData.getTradeData()), (ServerPlayer) player);
            return InteractionResult.SUCCESS;
        }

        String dialogId = this.entityData.get(DATA_DIALOG);
        if (!dialogId.isEmpty()) {
            player.getCapability(com.iscript.iscript.capability.ModCapabilities.PLAYER_QUESTS).ifPresent(data -> {
                for (QuestData quest : QuestManager.getAll((ServerLevel) this.level()).values()) {
                    if (data.isCompleted(quest.getId())) continue;
                    if (quest.getObjectiveType() == QuestObjectiveType.TALK) {
                        if (quest.getTarget().equals(dialogId) || quest.getTarget().equals(this.entityData.get(DATA_NPC_NAME))) {
                            int progress = data.getProgress(quest.getId()) + 1;
                            data.setProgress(quest.getId(), progress);
                            if (progress >= quest.getRequiredCount()) {
                                data.complete(quest.getId());
                                if (!quest.getRewardCommand().isEmpty()) {
                                    this.level().getServer().getCommands().performPrefixedCommand(
                                            player.createCommandSourceStack().withLevel((ServerLevel) this.level()).withPosition(player.position()),
                                            quest.getRewardCommand().replace("@p", player.getGameProfile().getName())
                                    );
                                }
                            }
                        }
                    }
                }
            });

            DialogData dialog = DialogManager.get((ServerLevel) this.level(), dialogId);
            if (dialog != null) {
                DialogData filtered = new DialogData();
                filtered.setId(dialog.getId());
                filtered.setTitle(dialog.getTitle());
                filtered.setText(dialog.getText());
                filtered.setPortrait(dialog.getPortrait());
                for (DialogData.DialogOption opt : dialog.getAvailableOptions(player)) {
                    filtered.getOptions().add(opt);
                }
                IScriptNetwork.sendToPlayer(new OpenDialogScreenPacket(filtered), (ServerPlayer) player);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        if (cachedData.isNoAI()) {
            super.tick();
            return;
        }
        super.tick();
        if (!this.level().isClientSide) {
            stateTick++;
            if (attackCooldown > 0) attackCooldown--;
            updateStateMachine();
            updateWaypoints();
        }
    }

    private void updateStateMachine() {
        NPCState state = cachedData.getState();
        switch (state) {
            case IDLE -> {
                if (stateTick % 100 == 0 && this.getNavigation().isDone()) {
                    double rx = this.getX() + (this.random.nextDouble() - 0.5) * 10;
                    double rz = this.getZ() + (this.random.nextDouble() - 0.5) * 10;
                    this.getNavigation().moveTo(rx, this.getY(), rz, 0.4);
                }
                if (cachedData.isAggressive()) checkFactionTargets();
            }
            case PATROL -> updateWaypoints();
            case FOLLOW -> {
                if (followTargetUUID != null) {
                    Player target = this.level().getPlayerByUUID(followTargetUUID);
                    if (target != null && target.isAlive()) {
                        double dist = this.distanceToSqr(target);
                        if (dist > 4.0) {
                            this.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.2);
                        } else {
                            this.getNavigation().stop();
                            this.getLookControl().setLookAt(target);
                        }
                    } else {
                        cachedData.setState(NPCState.IDLE);
                        followTargetUUID = null;
                    }
                }
            }
            case ATTACK -> {
                if (this.getTarget() != null && this.getTarget().isAlive()) {
                    if (this.distanceToSqr(this.getTarget()) < 9.0) {
                        if (attackCooldown <= 0) {
                            this.doHurtTarget(this.getTarget());
                            attackCooldown = 20;
                        }
                    } else {
                        this.getNavigation().moveTo(this.getTarget(), 1.0);
                    }
                } else {
                    cachedData.setState(NPCState.IDLE);
                    this.setTarget(null);
                }
            }
            case SIT -> {
                this.setPos(this.getX(), Math.floor(this.getY()), this.getZ());
                this.getNavigation().stop();
                sitTick++;
            }
            case DANCE -> {
                isDancing = true;
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
        }
    }

    private void updateWaypoints() {
        if (cachedData.getState() != NPCState.PATROL && cachedData.getState() != NPCState.IDLE) return;
        if (waypointData == null || waypointData.getWaypoints().isEmpty()) return;
        if (this.getNavigation().isDone()) {
            BlockPos next = waypointData.getNextWaypoint();
            if (next != null) {
                this.getNavigation().moveTo(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, cachedData.getMovementSpeed());
            }
        }
    }

    private void checkFactionTargets() {
        if (cachedData.getHostileFactions().isEmpty()) return;
        AABB box = new AABB(this.blockPosition()).inflate(16.0);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, box);
        for (Player p : players) {
            String pf = p.getCapability(ModCapabilities.PLAYER_DATA)
                    .map(com.iscript.iscript.capability.PlayerData::getFaction).orElse("neutral");
            if (cachedData.isHostileTo(pf)) {
                this.setTarget(p);
                cachedData.setState(NPCState.ATTACK);
                return;
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
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
        tag.putInt("NPCGlowColor", this.entityData.get(DATA_GLOW_COLOR));
        tag.putString("OwnerUUID", this.ownerUUID);
        tag.putString("NPCState", cachedData.getState().name());
        tag.putBoolean("NPCAggressive", cachedData.isAggressive());
        tag.putString("NPCHostileFactions", cachedData.getHostileFactions());
        tag.putFloat("NPCAttackDamage", cachedData.getAttackDamage());
        tag.putFloat("NPCMoveSpeed", cachedData.getMovementSpeed());
        tag.putBoolean("NPCEnableTrade", cachedData.isEnableTrade());
        tag.putBoolean("NPCNoAI", cachedData.isNoAI());
        tag.putBoolean("NPCInvulnerable", cachedData.isInvulnerable());
        tag.putBoolean("NPCSilent", cachedData.isSilent());
        tag.putBoolean("NPCHasGravity", cachedData.isHasGravity());
        CompoundTag waypoints = new CompoundTag();
        this.waypointData.save(waypoints);
        tag.put("Waypoints", waypoints);
        CompoundTag tradeTag = new CompoundTag();
        cachedData.getTradeData().save(tradeTag);
        tag.put("TradeData", tradeTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
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
        this.entityData.set(DATA_GLOW_COLOR, tag.contains("NPCGlowColor") ? tag.getInt("NPCGlowColor") : 0xFFFFFFFF);
        this.ownerUUID = tag.getString("OwnerUUID");
        this.setCustomName(Component.literal(this.entityData.get(DATA_NPC_NAME)));
        try {
            cachedData.setState(NPCState.valueOf(tag.getString("NPCState")));
        } catch (IllegalArgumentException e) {
            cachedData.setState(NPCState.IDLE);
        }
        cachedData.setAggressive(tag.getBoolean("NPCAggressive"));
        cachedData.setHostileFactions(tag.getString("NPCHostileFactions"));
        cachedData.setAttackDamage(tag.getFloat("NPCAttackDamage"));
        cachedData.setMovementSpeed(tag.getFloat("NPCMoveSpeed"));
        cachedData.setEnableTrade(tag.getBoolean("NPCEnableTrade"));
        cachedData.setNoAI(tag.contains("NPCNoAI") ? tag.getBoolean("NPCNoAI") : false);
        cachedData.setInvulnerable(tag.contains("NPCInvulnerable") ? tag.getBoolean("NPCInvulnerable") : false);
        cachedData.setSilent(tag.contains("NPCSilent") ? tag.getBoolean("NPCSilent") : false);
        cachedData.setHasGravity(tag.contains("NPCHasGravity") ? tag.getBoolean("NPCHasGravity") : true);
        if (tag.contains("Waypoints")) {
            this.waypointData.load(tag.getCompound("Waypoints"));
        }
        if (tag.contains("TradeData")) {
            cachedData.getTradeData().load(tag.getCompound("TradeData"));
        }
    }

    @Override
    public boolean isPushable() { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public boolean canBeLeashed(Player player) { return false; }

    private static class NPCFollowGoal extends Goal {
        private final IScriptNPCEntity npc;
        private final double speed;

        NPCFollowGoal(IScriptNPCEntity npc, double speed) {
            this.npc = npc;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return npc.cachedData.getState() == NPCState.FOLLOW && npc.followTargetUUID != null;
        }

        @Override
        public void tick() {
            Player target = npc.level().getPlayerByUUID(npc.followTargetUUID);
            if (target != null) {
                npc.getNavigation().moveTo(target, speed);
                npc.getLookControl().setLookAt(target);
            }
        }
    }

    private static class NPCFactionAttackGoal extends NearestAttackableTargetGoal<LivingEntity> {
        private final IScriptNPCEntity npc;

        NPCFactionAttackGoal(IScriptNPCEntity npc) {
            super(npc, LivingEntity.class, true);
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            if (!npc.cachedData.isAggressive() || npc.cachedData.getHostileFactions().isEmpty()) return false;
            if (npc.cachedData.getState() != NPCState.ATTACK && npc.cachedData.getState() != NPCState.IDLE) return false;
            return super.canUse();
        }

        @Override
        protected void findTarget() {
            AABB box = this.mob.getBoundingBox().inflate(16.0);
            List<LivingEntity> candidates = this.mob.level().getEntitiesOfClass(LivingEntity.class, box, e -> {
                if (e == npc) return false;
                if (e instanceof Player p) {
                    String faction = p.getCapability(ModCapabilities.PLAYER_DATA)
                            .map(com.iscript.iscript.capability.PlayerData::getFaction).orElse("neutral");
                    return npc.cachedData.isHostileTo(faction);
                }
                return false;
            });
            if (!candidates.isEmpty()) {
                this.target = candidates.get(0);
                npc.cachedData.setState(NPCState.ATTACK);
            }
        }
    }
}
