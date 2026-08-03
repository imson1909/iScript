package com.iscript.iscript.client.render;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.data.DataAccess;
import com.iscript.iscript.data.region.RegionData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RegionRenderHandler {
    private static String selectedRegionId = null;
    private static RegionData selectedRegionData = null;
    private static BlockPos selectedRegionAnchor = BlockPos.ZERO;
    private static int selectedRegionColor = 0xFF44FF44;

    public static void setSelectedRegion(String regionId, BlockPos anchor) {
        selectedRegionId = regionId;
        selectedRegionAnchor = anchor;
        if (regionId != null && !regionId.isEmpty()) {
            selectedRegionData = DataAccess.region(regionId);
        } else {
            selectedRegionData = null;
        }
    }

    public static void setSelectedRegionColor(int color) {
        selectedRegionColor = color;
    }

    public static void clearSelectedRegion() {
        selectedRegionId = null;
        selectedRegionData = null;
        selectedRegionAnchor = BlockPos.ZERO;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (selectedRegionData == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 cam = event.getCamera().getPosition();
        AABB bounds = selectedRegionData.getBounds(selectedRegionAnchor);

        float minX = (float) (bounds.minX - cam.x);
        float minY = (float) (bounds.minY - cam.y);
        float minZ = (float) (bounds.minZ - cam.z);
        float maxX = (float) (bounds.maxX - cam.x);
        float maxY = (float) (bounds.maxY - cam.y);
        float maxZ = (float) (bounds.maxZ - cam.z);

        float a = ((selectedRegionColor >> 24) & 0xFF) / 255.0f * 0.3f;
        float r = ((selectedRegionColor >> 16) & 0xFF) / 255.0f;
        float g = ((selectedRegionColor >> 8) & 0xFF) / 255.0f;
        float b = (selectedRegionColor & 0xFF) / 255.0f;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        Matrix4f matrix = event.getPoseStack().last().pose();

        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        drawLine(buf, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a + 0.4f);
        drawLine(buf, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a + 0.4f);
        drawLine(buf, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a + 0.4f);
        drawLine(buf, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a + 0.4f);

        drawLine(buf, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a + 0.4f);
        drawLine(buf, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a + 0.4f);
        drawLine(buf, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a + 0.4f);
        drawLine(buf, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a + 0.4f);

        drawLine(buf, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a + 0.4f);
        drawLine(buf, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a + 0.4f);
        drawLine(buf, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a + 0.4f);
        drawLine(buf, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a + 0.4f);

        tess.end();

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        drawQuad(buf, matrix, minX, minY, minZ, maxX, maxZ, r, g, b, a);
        drawQuad(buf, matrix, minX, maxY, minZ, maxX, maxZ, r, g, b, a);

        tess.end();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private static void drawLine(BufferBuilder buf, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }

    private static void drawQuad(BufferBuilder buf, Matrix4f matrix, float minX, float y, float minZ, float maxX, float maxZ, float r, float g, float b, float a) {
        buf.vertex(matrix, minX, y, minZ).color(r, g, b, a).endVertex();
        buf.vertex(matrix, maxX, y, minZ).color(r, g, b, a).endVertex();
        buf.vertex(matrix, maxX, y, maxZ).color(r, g, b, a).endVertex();
        buf.vertex(matrix, minX, y, maxZ).color(r, g, b, a).endVertex();
    }
}