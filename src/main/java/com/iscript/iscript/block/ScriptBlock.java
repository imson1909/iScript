package com.iscript.iscript.block;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.script.ScriptEngine;
import com.iscript.iscript.script.ScriptFileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ScriptBlock extends BaseEntityBlock {
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public ScriptBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TRIGGERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ScriptBlockEntity scriptBE) {
                scriptBE.openEditGui(serverPlayer);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        boolean wasTriggered = state.getValue(TRIGGERED);
        if (powered && !wasTriggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, true), 3);
            executeScript(level, pos);
        } else if (!powered && wasTriggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), 3);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(TRIGGERED)) {
            executeScript(level, pos);
        }
    }

    private void executeScript(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ScriptBlockEntity scriptBE && level instanceof ServerLevel serverLevel) {
            String script = ScriptFileManager.load(serverLevel, scriptBE.getScriptId());
            if (script.isEmpty()) return;
            Player nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Player p : level.players()) {
                double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (d < bestDist) {
                    bestDist = d;
                    nearest = p;
                }
            }
            if (nearest != null) {
                ScriptEngine engine = ScriptEngine.getInstance();
                if (engine.isAvailable()) {
                    try {
                        engine.execute(script, nearest, serverLevel);
                    } catch (Exception e) {
                        IScriptMod.LOGGER.error("ScriptBlock error: {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScriptBlockEntity(pos, state);
    }
}