package com.iscript.iscript.item;

import com.iscript.iscript.entity.IScriptNPCEntity;
import com.iscript.iscript.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class NPCSpawnerItem extends Item {
    public NPCSpawnerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos().above();
        Player player = context.getPlayer();

        IScriptNPCEntity npc = ModEntities.ISCRIPT_NPC.get().create(level);
        if (npc == null) return InteractionResult.FAIL;
        npc.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (player != null) {
            npc.setOwner(player);
        }
        level.addFreshEntity(npc);

        return InteractionResult.CONSUME;
    }
}
