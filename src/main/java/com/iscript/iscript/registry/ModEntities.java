package com.iscript.iscript.registry;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.entity.IScriptNPCEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, IScriptMod.MOD_ID);

    public static final RegistryObject<EntityType<IScriptNPCEntity>> ISCRIPT_NPC = ENTITIES.register("iscript_npc",
            () -> EntityType.Builder.of(IScriptNPCEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .build("iscript_npc"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}