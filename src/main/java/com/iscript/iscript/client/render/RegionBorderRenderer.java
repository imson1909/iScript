package com.iscript.iscript.client.render;

import com.iscript.iscript.blockentities.RegionBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = "iscript", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RegionBorderRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        boolean show = mc.options.renderDebug || isHoldingRegion(mc);
        if (!show) return;

        com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        Vec3 cam = event.getCamera().getPosition();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0F);
        RenderSystem.disableCull();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        for (RegionBlockEntity rbe : RegionBlockEntity.CLIENT_RENDER_TARGETS) {
            if (rbe == null || rbe.isRemoved()) continue;
            if (rbe.getLevel() != mc.level) continue;

            AABB b = rbe.getData().getBounds(rbe.getBlockPos());
            float r = 1.0f, g = 1.0f, bcol = 1.0f, a = 0.8f;
            drawBox(buf, matrix,
                    (float) b.minX + 0.5f, (float) b.minY + 0.5f, (float) b.minZ + 0.5f,
                    (float) b.maxX + 0.5f, (float) b.maxY + 0.5f, (float) b.maxZ + 0.5f,
                    r, g, bcol, a);
        }

        tess.end();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0F);
        poseStack.popPose();
    }

    private static boolean isHoldingRegion(Minecraft mc) {
        if (mc.player == null) return false;
        var regionItem = com.iscript.iscript.registry.ModBlocks.REGION_BLOCK.get().asItem();
        return mc.player.getMainHandItem().is(regionItem) || mc.player.getOffhandItem().is(regionItem);
    }

    private static void drawBox(BufferBuilder buf, Matrix4f matrix, float x1, float y1, float z1,
                                float x2, float y2, float z2, float r, float g, float b, float a) {
        line(buf, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(buf, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(buf, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(buf, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);
        line(buf, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(buf, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(buf, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(buf, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);
        line(buf, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(buf, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(buf, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(buf, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(BufferBuilder buf, Matrix4f matrix, float x1, float y1, float z1,
                             float x2, float y2, float z2, float r, float g, float b, float a) {
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}