package com.iscript.iscript.registry;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.item.NPCSpawnerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IScriptMod.MOD_ID);

    public static final RegistryObject<Item> NPC_SPAWNER = ITEMS.register("npc_spawner",
            () -> new NPCSpawnerItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
