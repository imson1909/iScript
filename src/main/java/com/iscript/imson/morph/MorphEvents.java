package com.iscript.imson.morph;

import com.iscript.imson.IScriptMod;
import com.iscript.imson.morph.render.MorphRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MorphEvents {

    private boolean animationPlaying = false;

    // Тот самый хак Budschie для предотвращения двойного рендера и дублирования теней!
    public static boolean isRenderingMorphBlocker = false;

    public boolean isAnimationPlaying() {
        return animationPlaying;
    }

    public void setAnimationPlaying(boolean v) {
        this.animationPlaying = v;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (isRenderingMorphBlocker) return;

        Player player = event.getEntity();
        player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
            if (data.isMorphed() && data.isVisible()) {
                // 1. Активируем защиту от зацикливания
                isRenderingMorphBlocker = true;

                // 2. Сразу отменяем ванильный рендер игрока, чтобы сохранить чистую матрицу
                event.setCanceled(true);

                // 3. Рисуем нашего моба (внутри MorphRenderer.render углы yaw/pitch считаются правильно)
                MorphRenderer.render(player, data, event.getPoseStack(),
                        event.getMultiBufferSource(), event.getPackedLight(),
                        event.getPartialTick());

                // 4. Отключаем защиту
                isRenderingMorphBlocker = false;
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
                    data.getAnimationController().update(1.0f / 20.0f);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onRenderHand(net.minecraftforge.client.event.RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getCapability(MorphData.CAPABILITY).ifPresent(data -> {
            if (data.isMorphed() && data.isVisible()) {
                event.setCanceled(true);
            }
        });
    }
}
