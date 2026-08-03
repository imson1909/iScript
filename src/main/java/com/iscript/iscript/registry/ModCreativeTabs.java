package com.iscript.iscript.registry;

import com.iscript.iscript.IScriptMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IScriptMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ISCRIPT_TAB = TABS.register("iscript_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.iscript"))
                    .icon(() -> new ItemStack(ModBlocks.SCRIPT_BLOCK.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.NPC_SPAWNER.get());
                        output.accept(ModItems.SCRIPT_ITEM.get());
                        output.accept(ModItems.SCRIPT_BLOCK_ITEM.get());
                        output.accept(ModItems.REGION_BLOCK_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}