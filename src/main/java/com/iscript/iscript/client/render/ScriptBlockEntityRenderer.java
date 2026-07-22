package com.iscript.iscript.client.render;

import com.iscript.iscript.block.ScriptBlockEntity;
import com.iscript.iscript.registry.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ScriptBlockEntityRenderer implements BlockEntityRenderer<ScriptBlockEntity> {
    public static final ResourceLocation SCRIPT_TEXTURE = new ResourceLocation("iscript", "textures/blocks/script_block.png");

    public ScriptBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ScriptBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        boolean holdingScript = isHoldingScriptBlock(player);
        boolean debugMode = mc.options.renderDebug;

        if (!player.isCreative() || (!holdingScript && !debugMode)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        Matrix4f matrix = poseStack.last().pose();
        float s = 0.4f;

        RenderSystem.setShaderTexture(0, SCRIPT_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        buf.vertex(matrix, -s, -s, 0).color(1, 1, 1, 0.8f).uv(0, 1).endVertex();
        buf.vertex(matrix, s, -s, 0).color(1, 1, 1, 0.8f).uv(1, 1).endVertex();
        buf.vertex(matrix, s, s, 0).color(1, 1, 1, 0.8f).uv(1, 0).endVertex();
        buf.vertex(matrix, -s, s, 0).color(1, 1, 1, 0.8f).uv(0, 0).endVertex();

        tess.end();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private boolean isHoldingScriptBlock(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(ModBlocks.SCRIPT_BLOCK.get().asItem()) || off.is(ModBlocks.SCRIPT_BLOCK.get().asItem());
    }

    @Override
    public boolean shouldRenderOffScreen(ScriptBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}