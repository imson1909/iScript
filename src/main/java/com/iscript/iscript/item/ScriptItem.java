package com.iscript.iscript.item;

import com.iscript.iscript.gui.screen.I18n;
import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.script.ScriptEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class ScriptItem extends Item {
    public ScriptItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        executeScript(stack, player, level);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        executeScript(stack, player, level);
        return InteractionResult.SUCCESS;
    }

    private void executeScript(ItemStack stack, Player player, Level level) {
        if (stack.hasTag() && stack.getTag().contains("Script")) {
            String script = stack.getTag().getString("Script");
            if (!script.isEmpty() && level instanceof ServerLevel serverLevel) {
                ScriptEngine engine = ScriptEngine.getInstance();
                if (engine.isAvailable()) {
                    try {
                        engine.execute(script, player, serverLevel);
                    } catch (Exception e) {
                        IScriptMod.LOGGER.error("ScriptItem error: {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains("Script")) {
            String script = stack.getTag().getString("Script");
            tooltip.add(I18n.t("iscript.item.script.tooltip", script.length() > 30 ? script.substring(0, 30) + "..." : script));
        } else {
            tooltip.add(I18n.t("iscript.item.script.tooltip.empty"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}