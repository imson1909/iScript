package com.iscript.iscript.registry;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.block.ScriptBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, IScriptMod.MOD_ID);

    public static final RegistryObject<Block> SCRIPT_BLOCK = BLOCKS.register("script_block",
            () -> new ScriptBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}