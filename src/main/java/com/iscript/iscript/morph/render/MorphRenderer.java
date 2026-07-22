package com.iscript.iscript.morph.render;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.morph.MorphData;
import com.iscript.iscript.morph.MorphManager;
import com.iscript.iscript.morph.animation.AnimationController;
import com.iscript.iscript.morph.model.Bone;
import com.iscript.iscript.morph.model.Cube;
import com.iscript.iscript.morph.model.GeoModel;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class MorphRenderer {
    private static final Map<String, ResourceLocation> TEXTURE_LOCATIONS = new HashMap<>();
    private static final Map<String, DynamicTexture> DYNAMIC_TEXTURES = new HashMap<>();

    public static void render(Entity entity, MorphData morphData, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight, float partialTick) {
        if (!morphData.isMorphed() || !morphData.isVisible()) return;

        GeoModel model = MorphManager.getModel(morphData.getModelId());
        if (model == null) {
            IScriptMod.LOGGER.warn("Morph model not found: {}", morphData.getModelId());
            return;
        }

        ResourceLocation texture = getTextureLocation(morphData.getModelId());
        if (texture == null) {
            IScriptMod.LOGGER.warn("Morph texture not found: {}", morphData.getModelId());
            texture = new ResourceLocation("minecraft", "textures/block/stone.png");
        }

        float scale = morphData.getScale();
        float time = morphData.getAnimationTick() / 20f;

        AnimationController controller = new AnimationController(morphData);

        poseStack.pushPose();

        float yaw;
        if (entity instanceof Player player) {
            yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        } else {
            yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.scale(scale, scale, scale);

        VertexConsumer builder = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

        for (Bone bone : model.getBones()) {
            if (bone.getParent().isEmpty()) {
                renderBone(bone, model, controller, time, poseStack, builder, packedLight, OverlayTexture.NO_OVERLAY, 1.0f);
            }
        }

        poseStack.popPose();
    }

    private static void renderBone(Bone bone, GeoModel model, AnimationController controller, float time,
                                   PoseStack poseStack, VertexConsumer builder, int packedLight, int packedOverlay, float alpha) {
        poseStack.pushPose();

        float[] animRot = controller.getBoneRotation(bone.getName(), time);
        float[] animPos = controller.getBonePosition(bone.getName(), time);

        float[] pivot = bone.getPivot();
        float[] baseRot = bone.getRotation();

        poseStack.translate(animPos[0] / 16f, animPos[1] / 16f, animPos[2] / 16f);

        poseStack.translate(pivot[0] / 16f, pivot[1] / 16f, pivot[2] / 16f);

        float rx = (float) Math.toRadians(baseRot[0] - animRot[0]);
        float ry = (float) Math.toRadians(baseRot[1] - animRot[1]);
        float rz = (float) Math.toRadians(baseRot[2] + animRot[2]);

        poseStack.mulPose(new Quaternionf().rotationZYX(rz, ry, rx));

        poseStack.translate(-pivot[0] / 16f, -pivot[1] / 16f, -pivot[2] / 16f);

        for (Cube cube : bone.getCubes()) {
            renderCube(cube, model, poseStack, builder, packedLight, packedOverlay, alpha);
        }

        for (Bone child : bone.getChildren()) {
            renderBone(child, model, controller, time, poseStack, builder, packedLight, packedOverlay, alpha);
        }

        poseStack.popPose();
    }

    private static void renderCube(Cube cube, GeoModel model, PoseStack poseStack,
                                   VertexConsumer builder, int packedLight, int packedOverlay, float alpha) {
        float[] origin = cube.getOrigin();
        float[] size = cube.getSize();
        float inflate = cube.getInflate();
        boolean mirror = cube.isMirror();

        float x = origin[0] / 16f;
        float y = origin[1] / 16f;
        float z = origin[2] / 16f;
        float w = size[0] / 16f;
        float h = size[1] / 16f;
        float d = size[2] / 16f;
        float inf = inflate / 16f;

        float x1 = x - inf, y1 = y - inf, z1 = z - inf;
        float x2 = x + w + inf, y2 = y + h + inf, z2 = z + d + inf;

        float texW = model.getTextureWidth();
        float texH = model.getTextureHeight();

        Matrix4f matrix = poseStack.last().pose();
        Vector3f normal = new Vector3f();

        float r = 1.0f, g = 1.0f, b = 1.0f;

        if (cube.hasPerFaceUv()) {
            Map<String, Cube.FaceUv> fuv = cube.getFaceUvs();
            if (fuv.containsKey("south"))  drawFace(builder, matrix, poseStack, normal, fuv.get("south"),  texW, texH, mirror, r, g, b, alpha, packedLight, packedOverlay,
                    x1, y2, z2,  x2, y2, z2,  x2, y1, z2,  x1, y1, z2,  0, 0, 1);
            if (fuv.containsKey("north"))  drawFace(builder, matrix, poseStack, normal, fuv.get("north"),  texW, texH, mirror, r, g, b, alpha, packedLight, packedOverlay,
                    x2, y2, z1,  x1, y2, z1,  x1, y1, z1,  x2, y1, z1,  0,0,-1);
            if (fuv.containsKey("east"))   drawFace(builder, matrix, poseStack, normal, fuv.get("east"),   texW, texH, mirror, r, g, b, alpha, packedLight, packedOverlay,
                    x2, y2, z2,  x2, y2, z1,  x2, y1, z1,  x2, y1, z2,  1,0,0);
            if (fuv.containsKey("west"))   drawFace(builder, matrix, poseStack, normal, fuv.get("west"),   texW, texH, mirror, r, g, b, alpha, packedLight, packedOverlay,
                    x1, y2, z1,  x1, y2, z2,  x1, y1, z2,  x1, y1, z1, -1,0,0);
            if (fuv.containsKey("up"))     drawFace(builder, matrix, poseStack, normal, fuv.get("up"),     texW, texH, mirror, r, g, b, alpha, packedLight, packedOverlay,
                    x1, y2, z2,  x2, y2, z2,  x2, y2, z1,  x1, y2, z1,  0,1,0);
            if (fuv.containsKey("down"))   drawFace(builder, matrix, poseStack, normal, fuv.get("down"),   texW, texH, mirror, r, g, b, alpha, packedLight, packedOverlay,
                    x1, y1, z1,  x2, y1, z1,  x2, y1, z2,  x1, y1, z2,  0,-1,0);
            return;
        }

        float u = cube.getUv()[0];
        float v = cube.getUv()[1];

        float su = size[0];
        float sh = size[1];
        float sd = size[2];

        float f_u1 = (u + sd) / texW;
        float f_v1 = (v + sd) / texH;
        float f_u2 = (u + sd + su) / texW;
        float f_v2 = (v + sd + sh) / texH;

        float b_u1 = (u + 2 * sd + su) / texW;
        float b_v1 = (v + sd) / texH;
        float b_u2 = (u + 2 * sd + 2 * su) / texW;
        float b_v2 = (v + sd + sh) / texH;

        float l_u1 = u / texW;
        float l_v1 = (v + sd) / texH;
        float l_u2 = (u + sd) / texW;
        float l_v2 = (v + sd + sh) / texH;

        float r_u1 = (u + sd + su) / texW;
        float r_v1 = (v + sd) / texH;
        float r_u2 = (u + sd + su + sd) / texW;
        float r_v2 = (v + sd + sh) / texH;

        float t_u1 = (u + sd) / texW;
        float t_v1 = v / texH;
        float t_u2 = (u + sd + su) / texW;
        float t_v2 = (v + sd) / texH;

        float bo_u1 = (u + sd + su) / texW;
        float bo_v1 = v / texH;
        float bo_u2 = (u + sd + su + su) / texW;
        float bo_v2 = (v + sd) / texH;

        if (mirror) {
            float t;
            t = f_u1; f_u1 = f_u2; f_u2 = t;
            t = b_u1; b_u1 = b_u2; b_u2 = t;
            t = l_u1; l_u1 = l_u2; l_u2 = t;
            t = r_u1; r_u1 = r_u2; r_u2 = t;
            t = t_u1; t_u1 = t_u2; t_u2 = t;
            t = bo_u1; bo_u1 = bo_u2; bo_u2 = t;
        }

        normal.set(0, 0, 1);
        poseStack.last().normal().transform(normal);
        normal.normalize();
        addQuad(matrix, builder, packedLight, packedOverlay, r, g, b, alpha,
                x1, y2, z2,  x2, y2, z2,  x2, y1, z2,  x1, y1, z2,
                f_u1, f_v1, f_u2, f_v2, normal);

        normal.set(0, 0, -1);
        poseStack.last().normal().transform(normal);
        normal.normalize();
        addQuad(matrix, builder, packedLight, packedOverlay, r, g, b, alpha,
                x2, y2, z1,  x1, y2, z1,  x1, y1, z1,  x2, y1, z1,
                b_u1, b_v1, b_u2, b_v2, normal);

        normal.set(1, 0, 0);
        poseStack.last().normal().transform(normal);
        normal.normalize();
        addQuad(matrix, builder, packedLight, packedOverlay, r, g, b, alpha,
                x2, y2, z2,  x2, y2, z1,  x2, y1, z1,  x2, y1, z2,
                r_u1, r_v1, r_u2, r_v2, normal);

        normal.set(-1, 0, 0);
        poseStack.last().normal().transform(normal);
        normal.normalize();
        addQuad(matrix, builder, packedLight, packedOverlay, r, g, b, alpha,
                x1, y2, z1,  x1, y2, z2,  x1, y1, z2,  x1, y1, z1,
                l_u1, l_v1, l_u2, l_v2, normal);

        normal.set(0, 1, 0);
        poseStack.last().normal().transform(normal);
        normal.normalize();
        addQuad(matrix, builder, packedLight, packedOverlay, r, g, b, alpha,
                x1, y2, z2,  x2, y2, z2,  x2, y2, z1,  x1, y2, z1,
                t_u1, t_v1, t_u2, t_v2, normal);

        normal.set(0, -1, 0);
        poseStack.last().normal().transform(normal);
        normal.normalize();
        addQuad(matrix, builder, packedLight, packedOverlay, r, g, b, alpha,
                x1, y1, z1,  x2, y1, z1,  x2, y1, z2,  x1, y1, z2,
                bo_u1, bo_v1, bo_u2, bo_v2, normal);
    }

    private static void drawFace(VertexConsumer builder, Matrix4f matrix, PoseStack poseStack, Vector3f normal, Cube.FaceUv fuv,
                                 float texW, float texH, boolean mirror,
                                 float r, float g, float b, float alpha, int packedLight, int packedOverlay,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float nx, float ny, float nz) {
        float u1 = fuv.uv[0] / texW;
        float v1 = fuv.uv[1] / texH;
        float u2 = (fuv.uv[0] + fuv.uvSize[0]) / texW;
        float v2 = (fuv.uv[1] + fuv.uvSize[1]) / texH;

        if (mirror) {
            float t = u1; u1 = u2; u2 = t;
        }

        normal.set(nx, ny, nz);
        poseStack.last().normal().transform(normal);
        normal.normalize();
        addQuad(matrix, builder, packedLight, packedOverlay, r, g, b, alpha,
                x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4,
                u1, v1, u2, v2, normal);
    }

    private static void addQuad(Matrix4f matrix, VertexConsumer builder, int packedLight, int packedOverlay,
                                float r, float g, float b, float alpha,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float x3, float y3, float z3, float x4, float y4, float z4,
                                float u1, float v1, float u2, float v2, Vector3f normal) {
        addVertex(matrix, builder, packedLight, packedOverlay, r, g, b, alpha, x1, y1, z1, u1, v1, normal);
        addVertex(matrix, builder, packedLight, packedOverlay, r, g, b, alpha, x2, y2, z2, u2, v1, normal);
        addVertex(matrix, builder, packedLight, packedOverlay, r, g, b, alpha, x3, y3, z3, u2, v2, normal);
        addVertex(matrix, builder, packedLight, packedOverlay, r, g, b, alpha, x4, y4, z4, u1, v2, normal);
    }

    private static void addVertex(Matrix4f matrix, VertexConsumer builder, int packedLight, int packedOverlay,
                                  float r, float g, float b, float alpha,
                                  float x, float y, float z, float u, float v, Vector3f normal) {
        Vector4f pos = new Vector4f(x, y, z, 1.0f);
        pos.mul(matrix);
        builder.vertex(pos.x(), pos.y(), pos.z())
                .color(r, g, b, alpha)
                .uv(u, v)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    public static ResourceLocation getTextureLocation(String modelId) {
        if (TEXTURE_LOCATIONS.containsKey(modelId)) return TEXTURE_LOCATIONS.get(modelId);

        BufferedImage img = MorphManager.getTexture(modelId);
        if (img == null) return null;

        NativeImage nativeImage = bufferedImageToNativeImage(img);
        DynamicTexture texture = new DynamicTexture(nativeImage);
        ResourceLocation loc = new ResourceLocation("iscript", "morph/" + modelId);
        Minecraft.getInstance().getTextureManager().register(loc, texture);
        DYNAMIC_TEXTURES.put(modelId, texture);
        TEXTURE_LOCATIONS.put(modelId, loc);
        return loc;
    }

    private static NativeImage bufferedImageToNativeImage(BufferedImage img) {
        NativeImage nativeImage = new NativeImage(img.getWidth(), img.getHeight(), false);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }
        return nativeImage;
    }

    public static void clearTextures() {
        for (DynamicTexture tex : DYNAMIC_TEXTURES.values()) {
            tex.close();
        }
        DYNAMIC_TEXTURES.clear();
        TEXTURE_LOCATIONS.clear();
    }
}