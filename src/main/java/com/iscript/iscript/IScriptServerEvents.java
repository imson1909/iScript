package com.iscript.iscript;

import com.iscript.iscript.data.ModData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class IScriptServerEvents {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ModData.init(event.getServer().overworld());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ModData.isDirty()) {
            ModData.get().save();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ModData.shutdown();
    }
}