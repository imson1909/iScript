package com.iscript.imson.morph.gui;

import com.iscript.imson.morph.MorphManager;
import com.iscript.imson.morph.model.Bone;
import com.iscript.imson.morph.model.GeoModel;
import com.iscript.imson.morph.render.MorphRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class GuiModelPreviewRenderer {
    private final Minecraft mc;
    private float yaw = 0f;
    private float pitch = 0f;
    private float scale = 1.0f;
    private String selectedId = "";

    public GuiModelPreviewRenderer(Minecraft mc) {
        this.mc = mc;
    }

    public void setSelectedModel(String id) {
        this.selectedId = id;
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void render(PoseStack poseStack, int x, int y, int width, int height) {
        if (selectedId.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        poseStack.translate(centerX, centerY, 50);

        float renderScale = 30f * scale;
        poseStack.scale(renderScale, -renderScale, renderScale);

        float rx = (float) Math.toRadians(pitch);
        float ry = (float) Math.toRadians(yaw);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotation(rx));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(ry));

        if (selectedId.startsWith("entity:")) {
            renderEntity(poseStack);
        } else {
            renderCustomModel(poseStack);
        }

        poseStack.popPose();
    }

    private void renderEntity(PoseStack poseStack) {
        String rl = selectedId.substring(7);
        try {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(rl));
            if (type != null && mc.level != null) {
                Entity entity = type.create(mc.level);
                if (entity != null) {
                    entity.setYRot(yaw);
                    entity.setXRot(pitch);

                    EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
                    dispatcher.setRenderShadow(false);

                    MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
                    dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, mc.getFrameTime(), poseStack, buffer, 15728880);
                    buffer.endBatch();

                    dispatcher.setRenderShadow(true);
                }
            }
        } catch (Exception e) {
        }
    }

    private void renderCustomModel(PoseStack poseStack) {
        GeoModel model = MorphManager.getModel(selectedId);
        if (model == null) {
            return;
        }

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        ResourceLocation texture = MorphRenderer.getTextureLocation(selectedId);
        if (texture == null) {
            texture = new ResourceLocation("minecraft", "textures/block/stone.png");
        }

        VertexConsumer builder = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

        for (Bone bone : model.getBones()) {
            if (bone.getParent().isEmpty()) {
                MorphRenderer.renderBoneGui(bone, model, poseStack, builder, 15728880, OverlayTexture.NO_OVERLAY, 1.0f);
            }
        }

        buffer.endBatch();
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getScale() {
        return scale;
    }
}