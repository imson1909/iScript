package com.iscript.iscript.morph;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.morph.render.MorphRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MorphEvents {

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
            if (data.isMorphed() && data.isVisible()) {
                event.setCanceled(true);
                MorphRenderer.render(player, data, event.getPoseStack(),
                        event.getMultiBufferSource(), event.getPackedLight(),
                        event.getPartialTick());
            }
        });
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) return;
        entity.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
            if (data.isMorphed() && data.isVisible()) {
                event.setCanceled(true);
                MorphRenderer.render(entity, data, event.getPoseStack(),
                        event.getMultiBufferSource(), event.getPackedLight(),
                        event.getPartialTick());
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null) return;
        for (Player player : Minecraft.getInstance().level.players()) {
            player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
                if (data.isMorphed() && !data.getCurrentAnimation().isEmpty()) {
                    data.incrementAnimationTick();
                }
            });
        }
    }
}