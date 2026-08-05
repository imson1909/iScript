package com.iscript.imson.block;

import com.iscript.imson.network.IScriptNetwork;
import com.iscript.imson.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.iscript.imson.network.packet.OpenGuiPacket;

public class ScriptBlockEntity extends BlockEntity {
    private String scriptId = "";

    public ScriptBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCRIPT_BLOCK_ENTITY.get(), pos, state);
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String id) {
        this.scriptId = id;
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void openEditGui(ServerPlayer player) {
        String script = com.iscript.imson.script.ScriptFileManager.load((net.minecraft.server.level.ServerLevel) this.getLevel(), this.scriptId);
        IScriptNetwork.sendToPlayer(new OpenGuiPacket(OpenGuiPacket.Type.SCRIPT_BLOCK, OpenGuiPacket.scriptBlockToTag(this.getBlockPos(), this.scriptId, script)), player);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.scriptId = tag.getString("ScriptId");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("ScriptId", this.scriptId);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}