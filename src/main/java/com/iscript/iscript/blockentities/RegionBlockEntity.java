package com.iscript.iscript.blockentities;

import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.data.RegionManager;
import com.iscript.iscript.data.cutscene.CutscenePlayer;
import com.iscript.iscript.data.region.RegionData;
import com.iscript.iscript.data.region.RegionEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class RegionBlockEntity extends BlockEntity {
    public static final String TAG_DATA = "RegionData";
    private RegionData data = new RegionData();
    private Set<UUID> insidePlayers = new HashSet<>();
    private Map<UUID, Integer> tickCounters = new HashMap<>();

    public static final Set<RegionBlockEntity> CLIENT_RENDER_TARGETS = new CopyOnWriteArraySet<>();
    private static final Set<RegionBlockEntity> SERVER_INSTANCES = new CopyOnWriteArraySet<>();

    public RegionBlockEntity(BlockPos pos, BlockState state) {
        super(com.iscript.iscript.registry.ModBlockEntities.REGION_BE.get(), pos, state);
    }

    public RegionData getData() { return data; }

    public void setData(RegionData d) {
        this.data = d;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public AABB getBounds() {
        return data.getBounds(worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            if (level.isClientSide) {
                CLIENT_RENDER_TARGETS.add(this);
            } else {
                SERVER_INSTANCES.add(this);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_RENDER_TARGETS.remove(this);
        SERVER_INSTANCES.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT_RENDER_TARGETS.remove(this);
        SERVER_INSTANCES.remove(this);
    }

    public static Collection<RegionBlockEntity> getAllInstances() {
        return Collections.unmodifiableSet(SERVER_INSTANCES);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RegionBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;

        AABB bounds = be.getBounds();
        Set<UUID> current = new HashSet<>();

        for (Player player : level.getEntitiesOfClass(Player.class, bounds)) {
            if (player.isSpectator()) continue;

            UUID id = player.getUUID();
            current.add(id);

            if (!be.insidePlayers.contains(id)) {
                if (player instanceof ServerPlayer sp) {
                    if (!be.data.getOnEnterCutsceneId().isEmpty() && !CutscenePlayer.isPlaying(sp)) {
                        var cutscene = DataAccess.cutscene(be.data.getOnEnterCutsceneId());
                        if (cutscene != null) {
                            CutscenePlayer.play(sp, cutscene);
                        }
                    }
                    for (RegionEffect e : be.data.getEnterEffects()) {
                        RegionManager.applyEffect(e, sp, sl);
                    }
                }
            }

            int c = be.tickCounters.getOrDefault(id, 0) + 1;
            be.tickCounters.put(id, c);
            if (be.data.getTickInterval() > 0 && c % be.data.getTickInterval() == 0) {
                for (RegionEffect e : be.data.getTickEffects()) {
                    if (player instanceof ServerPlayer sp) {
                        RegionManager.applyEffect(e, sp, sl);
                    }
                }
            }
        }

        for (UUID old : be.insidePlayers) {
            if (!current.contains(old)) {
                Player player = level.getPlayerByUUID(old);
                if (player instanceof ServerPlayer sp) {
                    for (RegionEffect e : be.data.getExitEffects()) {
                        RegionManager.applyEffect(e, sp, sl);
                    }
                }
                be.tickCounters.remove(old);
            }
        }

        be.insidePlayers = current;
        if (current.isEmpty()) {
            be.tickCounters.clear();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag d = new CompoundTag();
        data.save(d);
        tag.put(TAG_DATA, d);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TAG_DATA, 10)) {
            data.load(tag.getCompound(TAG_DATA));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @OnlyIn(Dist.CLIENT)
    public static class ClientCleanupHandler {
        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            CLIENT_RENDER_TARGETS.clear();
        }
    }

    static {
        MinecraftForge.EVENT_BUS.register(ClientCleanupHandler.class);
    }
}