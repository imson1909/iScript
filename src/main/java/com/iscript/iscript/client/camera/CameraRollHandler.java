package com.iscript.iscript.client.camera;

import com.iscript.iscript.IScriptMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CameraRollHandler {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (!CutsceneCameraHandler.isActive()) {
            return;
        }

        float roll = CutsceneCameraHandler.getCurrentRoll();
        System.out.println("[RollHandler] roll=" + roll + " active=" + CutsceneCameraHandler.isActive());
        if (Math.abs(roll) < 0.01f) return;

        System.out.println("[RollHandler] APPLYING roll=" + roll);
        PoseStack pose = event.getPoseStack();
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
    }
}