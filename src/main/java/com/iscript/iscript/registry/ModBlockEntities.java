package com.iscript.iscript.registry;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.block.ScriptBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, IScriptMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ScriptBlockEntity>> SCRIPT_BLOCK_ENTITY = BLOCK_ENTITIES.register("script_block_entity",
            () -> BlockEntityType.Builder.of(ScriptBlockEntity::new, ModBlocks.SCRIPT_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}