package com.iscript.imson.item;

import com.iscript.imson.data.DataAccess;
import com.iscript.imson.data.npc.NPCData;
import com.iscript.imson.entity.IScriptNPCEntity;
import com.iscript.imson.gui.screen.NPCSpawnTemplateScreen;
import com.iscript.imson.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
        spawnNPC(level, player, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getItemInHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            spawnNPC(level, player, player.getX(), player.getY(), player.getZ(), stack);
            return InteractionResultHolder.success(stack);
        }
        if (level.isClientSide) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new NPCSpawnTemplateScreen(hand));
        }
        return InteractionResultHolder.success(stack);
    }

    private void spawnNPC(Level level, Player player, double x, double y, double z, ItemStack stack) {
        IScriptNPCEntity npc = ModEntities.ISCRIPT_NPC.get().create(level);
        if (npc == null) return;
        npc.setPos(x, y, z);
        String templateId = stack.getOrCreateTag().getString("TemplateId");
        NPCData data;
        if (!templateId.isEmpty()) {
            data = DataAccess.npc(templateId);
            if (data == null) data = new NPCData();
        } else {
            data = new NPCData();
            data.setName("NPC");
            data.setHealth(20);
            data.setMaxHealth(20);
        }
        npc.setNPCData(data);
        if (player != null) {
            npc.setOwner(player);
        }
        level.addFreshEntity(npc);
    }
}