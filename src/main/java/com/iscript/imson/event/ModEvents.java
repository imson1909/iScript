package com.iscript.imson.event;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.capability.PlayerData;
import com.iscript.imson.capability.PlayerQuestData;
import com.iscript.imson.entity.IScriptNPCEntity;
import com.iscript.imson.entity.render.IScriptNPCRenderer;
import com.iscript.imson.registry.ModEntities;
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
        event.register(PlayerData.class);
    }
}
