package com.iscript.iscript;

import com.iscript.iscript.client.ClientSetup;
import com.iscript.iscript.client.camera.CameraRollHandler;
import com.iscript.iscript.client.camera.CutsceneCameraHandler;
import com.iscript.iscript.client.camera.CutscenePathRenderer;
import com.iscript.iscript.command.IScriptCommands;
import com.iscript.iscript.config.IScriptConfig;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.registry.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(IScriptMod.MOD_ID)
public class IScriptMod {
    public static final String MOD_ID = "iscript";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public IScriptMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(ClientSetup::onClientSetup);

        forgeBus.addListener(this::onRegisterCommands);

        forgeBus.register(CutsceneCameraHandler.class);
        forgeBus.register(CameraRollHandler.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, IScriptConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            IScriptNetwork.register();
            LOGGER.info("iScript common setup complete");
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("iScript client setup complete");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        IScriptCommands.register(event.getDispatcher());
    }
}