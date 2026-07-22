package com.iscript.iscript;

import com.iscript.iscript.client.ClientSetup;
import com.iscript.iscript.client.camera.CameraRollHandler;
import com.iscript.iscript.client.camera.CutsceneCameraHandler;
import com.iscript.iscript.command.IScriptCommands;
import com.iscript.iscript.config.IScriptConfig;
import com.iscript.iscript.data.ModData;
import com.iscript.iscript.morph.MorphData;
import com.iscript.iscript.morph.MorphManager;
import com.iscript.iscript.morph.network.MorphSyncPacket;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.registry.*;
import com.iscript.iscript.script.ScriptEngine;
import com.iscript.iscript.script.ScriptGraphExecutor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(ClientSetup::onClientSetup);

        forgeBus.addListener(this::onRegisterCommands);
        forgeBus.addListener(this::onServerTick);
        forgeBus.addListener(this::onPlayerLoggedOut);
        forgeBus.addListener(this::onPlayerChangedDimension);
        forgeBus.addListener(this::onRegisterCapabilities);
        forgeBus.addGenericListener(Entity.class, this::onAttachCapabilities);
        forgeBus.addListener(this::onPlayerClone);
        forgeBus.addListener(this::onPlayerLoggedIn);

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

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ScriptEngine.getInstance().onServerTick();
            if (ModData.isDirty()) {
                ModData.get().save();
            }
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScriptGraphExecutor.stopFor(player.getUUID());
        }
    }

    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScriptGraphExecutor.stopFor(player.getUUID());
        }
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MorphData.class);
    }

    private void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation(IScriptMod.MOD_ID, "morph_data"),
                    new MorphData.Provider()
            );
        }
    }

    private void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(MorphData.CAPABILITY).ifPresent(oldData -> {
            event.getEntity().getCapability(MorphData.CAPABILITY).ifPresent(newData -> {
                newData.copyFrom(oldData);
                if (event.getEntity() instanceof ServerPlayer sp) {
                    MorphSyncPacket.syncToTracking(sp);
                }
            });
        });
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sp.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
                IScriptNetwork.sendToAll(MorphSyncPacket.create(sp));
            });
            if (sp.getServer() != null) {
                for (ServerPlayer other : sp.getServer().getPlayerList().getPlayers()) {
                    if (other == sp) continue;
                    other.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
                        IScriptNetwork.sendToPlayer(MorphSyncPacket.create(other), sp);
                    });
                }
            }
        }
    }
}