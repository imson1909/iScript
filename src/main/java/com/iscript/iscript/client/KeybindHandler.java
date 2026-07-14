package com.iscript.iscript.client;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.gui.screen.DashboardScreen;
import com.iscript.iscript.network.IScriptNetwork;
import com.iscript.iscript.network.packet.RequestCutscenesPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeybindHandler {
    public static final KeyMapping OPEN_DASHBOARD = new KeyMapping(
            "key.iscript.dashboard",
            GLFW.GLFW_KEY_F6,
            "key.categories.iscript"
    );

    public static final KeyMapping OPEN_CUTSCENE_EDITOR = new KeyMapping(
            "key.iscript.cutscene_editor",
            GLFW.GLFW_KEY_EQUAL,
            "key.categories.iscript"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_DASHBOARD);
        event.register(OPEN_CUTSCENE_EDITOR);
    }

    @Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientTickHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            if (OPEN_DASHBOARD.consumeClick()) {
                mc.setScreen(new DashboardScreen());
            }

            if (OPEN_CUTSCENE_EDITOR.consumeClick()) {
                IScriptNetwork.sendToServer(new RequestCutscenesPacket());
            }
        }
    }
}