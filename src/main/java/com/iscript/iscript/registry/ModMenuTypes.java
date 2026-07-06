package com.iscript.iscript.registry;

import com.iscript.iscript.IScriptMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, IScriptMod.MOD_ID);

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
