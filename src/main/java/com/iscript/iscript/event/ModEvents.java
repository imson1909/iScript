package com.iscript.iscript.event;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.capability.PlayerQuestData;
import com.iscript.iscript.entity.IScriptNPCEntity;
import com.iscript.iscript.entity.render.IScriptNPCRenderer;
import com.iscript.iscript.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ISCRIPT_NPC.get(), IScriptNPCEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ISCRIPT_NPC.get(), IScriptNPCRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerQuestData.class);
    }
}
