package com.iscript.imson.client;

import com.iscript.imson.client.render.RegionBlockEntityRenderer;
import com.iscript.imson.client.render.ScriptBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "iscript", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        BlockEntityRenderers.register(com.iscript.imson.registry.ModBlockEntities.REGION_BE.get(), RegionBlockEntityRenderer::new);
        BlockEntityRenderers.register(com.iscript.imson.registry.ModBlockEntities.SCRIPT_BLOCK_ENTITY.get(), ScriptBlockEntityRenderer::new);
    }
}