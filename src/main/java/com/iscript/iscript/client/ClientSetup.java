package com.iscript.iscript.client;

import com.iscript.iscript.blockentities.RegionBlockEntity;
import com.iscript.iscript.client.render.RegionBlockEntityRenderer;
import com.iscript.iscript.client.render.RegionBorderRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "iscript", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        BlockEntityRenderers.register(com.iscript.iscript.registry.ModBlockEntities.REGION_BE.get(), RegionBlockEntityRenderer::new);
    }
}