package com.iscript.imson.morph.gui;

import com.iscript.imson.gui.screen.I18n;
import com.iscript.imson.gui.theme.Theme;
import com.iscript.imson.gui.theme.UI;
import com.iscript.imson.morph.MorphManager;
import com.iscript.imson.morph.model.Bone;
import com.iscript.imson.morph.model.GeoModel;
import com.iscript.imson.morph.render.MorphRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.lwjgl.opengl.GL11;

public class ModelPreviewScreen {
    private final Minecraft mc;
    private final Font font;
    private String selectedId = "";
    private float scaleValue = 1.0f;
    private float modelYaw = 0f;
    private float modelPitch = 0f;
    private float panX = 0f;
    private float panY = 0f;
    private boolean isDragging = false;
    private boolean isPanning = false;
    private double lastMouseX;
    private double lastMouseY;
    private Entity previewEntity = null;
    private String previewEntityId = "";

    public ModelPreviewScreen() {
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
    }

    public void setSelectedId(String id) {
        if (!id.equals(this.selectedId)) {
            this.selectedId = id;
            this.panX = 0f;
            this.panY = 0f;
            this.previewEntity = null;
        }
    }

    public String getSelectedId() {
        return selectedId;
    }

    public void resetView() {
        this.scaleValue = 1.0f;
        this.modelYaw = 0f;
        this.modelPitch = 0f;
        this.panX = 0f;
        this.panY = 0f;
    }

    public void render(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, float pt) {
        PoseStack pose = g.pose();
        pose.pushPose();

        enableScissor(x, y, w, h);

        String scaleText = String.format("Zoom: %.0f%%", scaleValue * 100);
        int textW = font.width(scaleText);
        int textX = x + w - 8 - textW;
        int textY = y + 8;
        g.fill(textX - 4, textY - 2, textX + textW + 4, textY + font.lineHeight + 2, 0x80000000);
        g.drawString(font, scaleText, textX, textY, Theme.TEXT);

        if (!selectedId.isEmpty()) {
            try {
                pose.pushPose();
                float centerX = x + w / 2f + panX;
                float centerY = y + h / 2f + panY;
                float scale = 30f * scaleValue;
                float zOffset = 500f + (scale * 2f);
                pose.translate(centerX, centerY, zOffset);
                pose.scale(scale, -scale, scale);
                float rx = (float) Math.toRadians(modelPitch);
                float ry = (float) Math.toRadians(modelYaw);
                pose.mulPose(com.mojang.math.Axis.XP.rotation(rx));
                pose.mulPose(com.mojang.math.Axis.YP.rotation(ry));

                if (selectedId.startsWith("entity:")) {
                    renderEntityPreview(pose, g, x, y, w, h);
                } else {
                    renderCustomModelPreview(pose, g, x, y, w, h, pt);
                }
                pose.popPose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            UI.centerLabel(g, font, I18n.s("iscript.morph.no_model"), x, y + h / 2 - 10, w);
        }

        disableScissor();
        pose.popPose();

        g.drawCenteredString(font, I18n.s("iscript.morph.rotate_hint"), x + w / 2, y + h - 15, Theme.TEXT_DIM);
    }

    private void renderEntityPreview(PoseStack pose, GuiGraphics g, int x, int y, int w, int h) {
        if (!selectedId.equals(previewEntityId) || previewEntity == null || previewEntity.isRemoved()) {
            previewEntityId = selectedId;
            previewEntity = null;
            String rl = selectedId.substring(7);
            try {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(rl));
                if (type != null && mc.level != null) {
                    previewEntity = type.create(mc.level);
                    if (previewEntity != null) {
                        if (previewEntity instanceof Mob mob) {
                            mob.setNoAi(true);
                        }
                        previewEntity.setYRot(0f);
                        previewEntity.setXRot(0f);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (previewEntity != null) {
            previewEntity.setYRot(0f);
            previewEntity.setXRot(0f);
            if (previewEntity instanceof LivingEntity living) {
                living.yHeadRot = 0f;
                living.yBodyRot = 0f;
                living.yHeadRotO = 0f;
                living.yBodyRotO = 0f;
                living.yRotO = 0f;
                living.xRotO = 0f;
            }

            float baseScale = 30f * scaleValue;
            float height = previewEntity.getBbHeight();
            if (height > 2) {
                baseScale *= 2 / height;
            } else if (height < 0.5) {
                baseScale *= 0.6 / height;
            }

            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

            RenderSystem.enableDepthTest();
            try {
                PoseStack renderPose = new PoseStack();
                renderPose.pushPose();
                float centerX = x + w / 2f + panX;
                float centerY = y + h / 2f + panY;
                float zOffset = 500f + (baseScale * 2f);
                renderPose.translate(centerX, centerY, zOffset);
                renderPose.scale(baseScale, -baseScale, baseScale);
                float rx = (float) Math.toRadians(modelPitch);
                float ry = (float) Math.toRadians(modelYaw);
                renderPose.mulPose(com.mojang.math.Axis.XP.rotation(rx));
                renderPose.mulPose(com.mojang.math.Axis.YP.rotation(ry));
                dispatcher.render(previewEntity, 0.0, 0.0, 0.0, 0.0f, 0.0f, renderPose, buffer, 15728880);
                buffer.endBatch();
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
                renderPose.popPose();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                RenderSystem.disableDepthTest();
            }
            dispatcher.setRenderShadow(true);
        } else {
            UI.centerLabel(g, font, I18n.s("iscript.morph.model_not_found"), x, y + h / 2 - 10, w);
        }
    }

    private void renderCustomModelPreview(PoseStack pose, GuiGraphics g, int x, int y, int w, int h, float pt) {
        GeoModel model = MorphManager.getModel(selectedId);
        if (model == null) {
            UI.centerLabel(g, font, I18n.s("iscript.morph.model_not_found"), x, y + h / 2 - 10, w);
            return;
        }

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        ResourceLocation texture = MorphRenderer.getTextureLocation(selectedId);
        if (texture == null) texture = new ResourceLocation("minecraft", "textures/block/stone.png");
        com.mojang.blaze3d.vertex.VertexConsumer builder = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

        RenderSystem.enableDepthTest();
        try {
            for (Bone bone : model.getBones()) {
                if (bone.getParent().isEmpty()) {
                    MorphRenderer.renderBone(bone, model, null, 0.0f, pose, builder, 15728880, OverlayTexture.NO_OVERLAY, 1.0f, null, pt);
                }
            }
            buffer.endBatch();
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RenderSystem.disableDepthTest();
        }
    }

    public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w, int h) {
        if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
            if (btn == 0) {
                isDragging = true;
                lastMouseX = mx;
                lastMouseY = my;
                return true;
            }
            if (btn == 1) {
                isPanning = true;
                lastMouseX = mx;
                lastMouseY = my;
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mx, double my, int btn) {
        isDragging = false;
        isPanning = false;
        return false;
    }

    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (isDragging && btn == 0) {
            modelYaw += (float) (mx - lastMouseX) * 0.8f;
            modelPitch += (float) (my - lastMouseY) * 0.8f;
            modelPitch = Math.max(-90f, Math.min(90f, modelPitch));
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        if (isPanning && btn == 1) {
            panX += (float) (mx - lastMouseX);
            panY += (float) (my - lastMouseY);
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double delta, int x, int y, int w, int h) {
        if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
            if (delta > 0) {
                scaleValue = Math.min(10.0f, scaleValue + 0.1f);
            } else {
                scaleValue = Math.max(0.1f, scaleValue - 0.1f);
            }
            return true;
        }
        return false;
    }

    private void enableScissor(int x, int y, int width, int height) {
        double scale = mc.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int) (x * scale),
                (int) (mc.getWindow().getHeight() - (y + height) * scale),
                (int) (width * scale),
                (int) (height * scale)
        );
    }

    private void disableScissor() {
        RenderSystem.disableScissor();
    }

    public void onClose() {
        previewEntity = null;
        previewEntityId = "";
    }
}