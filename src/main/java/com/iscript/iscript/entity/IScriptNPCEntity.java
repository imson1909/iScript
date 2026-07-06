package com.iscript.iscript.entity;

import com.iscript.iscript.data.DialogManager;
import com.iscript.iscript.data.dialog.DialogData;
import com.iscript.iscript.data.npc.NPCData;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.OpenDialogScreenPacket;
import com.iscript.iscript.network.packet.OpenNPCEditGuiPacket;
import com.iscript.iscript.network.packet.SyncNPCDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class IScriptNPCEntity extends Mob {
    private static final EntityDataAccessor<String> DATA_NPC_NAME = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_DIALOG = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_FACTION = SynchedEntityData.defineId(IScriptNPCEntity.class, EntityDataSerializers.STRING);

    private String ownerUUID = "";

    public IScriptNPCEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(false);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_NPC_NAME, "NPC");
        this.entityData.define(DATA_DIALOG, "");
        this.entityData.define(DATA_SKIN, "");
        this.entityData.define(DATA_FACTION, "neutral");
    }

    public void setNPCData(NPCData data) {
        this.entityData.set(DATA_NPC_NAME, data.getName());
        this.entityData.set(DATA_DIALOG, data.getDialogId());
        this.entityData.set(DATA_SKIN, data.getSkin());
        this.entityData.set(DATA_FACTION, data.getFaction());
        this.setCustomName(Component.literal(data.getName()));
        if (!this.level().isClientSide) {
            IScriptNetwork.sendToAll(new SyncNPCDataPacket(this.getId(), data));
        }
    }

    public NPCData getNPCData() {
        NPCData data = new NPCData();
        data.setName(this.entityData.get(DATA_NPC_NAME));
        data.setDialogId(this.entityData.get(DATA_DIALOG));
        data.setSkin(this.entityData.get(DATA_SKIN));
        data.setFaction(this.entityData.get(DATA_FACTION));
        return data;
    }

    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID().toString();
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

        String dialogId = this.entityData.get(DATA_DIALOG);
        if (!dialogId.isEmpty()) {
            DialogData dialog = DialogManager.get((ServerLevel) this.level(), dialogId);
            if (dialog != null) {
                DialogData filtered = new DialogData();
                filtered.setId(dialog.getId());
                filtered.setTitle(dialog.getTitle());
                filtered.setText(dialog.getText());
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NPCName", this.entityData.get(DATA_NPC_NAME));
        tag.putString("NPCDialog", this.entityData.get(DATA_DIALOG));
        tag.putString("NPCSkin", this.entityData.get(DATA_SKIN));
        tag.putString("NPCFaction", this.entityData.get(DATA_FACTION));
        tag.putString("OwnerUUID", this.ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_NPC_NAME, tag.getString("NPCName"));
        this.entityData.set(DATA_DIALOG, tag.getString("NPCDialog"));
        this.entityData.set(DATA_SKIN, tag.getString("NPCSkin"));
        this.entityData.set(DATA_FACTION, tag.getString("NPCFaction"));
        this.ownerUUID = tag.getString("OwnerUUID");
        this.setCustomName(Component.literal(this.entityData.get(DATA_NPC_NAME)));
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public Vec3 getDeltaMovement() { return Vec3.ZERO; }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D);
    }

    @Override
    public boolean canBeLeashed(Player player) { return false; }
}
