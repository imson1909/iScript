package com.iscript.iscript.client.camera;

import com.iscript.iscript.IScriptMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = IScriptMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CutscenePathRenderer {
    private static CutscenePath previewPath = null;
    private static CutsceneCameraMode previewMode = null;
    private static Map<String, CutscenePath> allPaths = new HashMap<>();

    private static final int COLOR_LINEAR = 0xFF44AA44;
    private static final int COLOR_CATMULL_ROM = 0xFF4488FF;
    private static final int COLOR_CUBIC_BEZIER = 0xFFFF8844;
    private static final int COLOR_HERMITE = 0xFFFF44FF;
    private static final int COLOR_BSPLINE = 0xFFFFFF44;
    private static final int COLOR_CIRCULAR = 0xFF44FFFF;
    private static final int COLOR_ELLIPTICAL = 0xFFFF4444;
    private static final int COLOR_SPIRAL = 0xFFAA44FF;
    private static final int COLOR_HELIX = 0xFF44FFAA;
    private static final int COLOR_STEP = 0xFF888888;
    private static final int COLOR_NONE = 0xFF333333;
    private static final int COLOR_KEYFRAME = 0xFFFF4444;
    private static final int COLOR_DIRECTION = 0xFFFFFF00;
    private static final int COLOR_LOOKAT = 0xFFFF00FF;
    private static final int COLOR_ORBIT = 0xFF00FFFF;

    public static void setPreviewPath(CutscenePath path, CutsceneCameraMode mode) {
        previewPath = path;
        previewMode = mode;
    }

    public static void clearPreview() {
        previewPath = null;
        previewMode = null;
    }

    public static void setAllPaths(Map<String, CutscenePath> paths) {
        allPaths = new HashMap<>(paths);
    }

    public static void clearAllPaths() {
        allPaths.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.LINES);

        for (var path : allPaths.values()) {
            renderPath(builder, pose, path, null);
        }

        if (previewPath != null && !previewPath.keyframes.isEmpty()) {
            renderPath(builder, pose, previewPath, previewMode);
        }

        mc.renderBuffers().bufferSource().endBatch(RenderType.LINES);
        pose.popPose();
    }

    private static void renderPath(VertexConsumer builder, PoseStack pose, CutscenePath path, CutsceneCameraMode mode) {
        if (path.keyframes.size() < 2) return;

        int steps = Math.min(200, (int)(path.getDuration() * 2));
        if (steps < 10) steps = 10;

        Vec3 prev = path.interpolatePosition(0f);

        for (int i = 1; i <= steps; i++) {
            float t = i / (float)steps;
            Vec3 curr = path.interpolatePosition(t);

            int segIndex = findSegment(path, t);
            PathType segType = path.keyframes.get(Math.min(segIndex, path.keyframes.size() - 1)).segmentType;
            int color = getSegmentColor(segType);

            drawLine(builder, pose, prev, curr, color);
            prev = curr;
        }

        for (int i = 0; i < path.keyframes.size(); i++) {
            var kf = path.keyframes.get(i);
            drawPoint(builder, pose, kf.point, COLOR_KEYFRAME);
            drawDirectionArrow(builder, pose, kf.point, kf.yaw, kf.pitch);
        }

        if (mode != null) {
            if (mode.useLookAt && mode.lookAtTarget != null) {
                drawPoint(builder, pose, mode.lookAtTarget, COLOR_LOOKAT);
                for (int i = 0; i < path.keyframes.size(); i++) {
                    var kf = path.keyframes.get(i);
                    drawDashedLine(builder, pose, kf.point, mode.lookAtTarget, COLOR_LOOKAT, 8);
                }
            }

            if (mode.orbitMode && mode.orbitCenter != null) {
                drawPoint(builder, pose, mode.orbitCenter, COLOR_ORBIT);
                drawOrbitCircle(builder, pose, mode.orbitCenter, mode.orbitRadius);
            }
        }
    }

    private static int findSegment(CutscenePath path, float progress) {
        float totalTicks = progress * path.getDuration();
        for (int i = 0; i < path.keyframes.size() - 1; i++) {
            if (totalTicks >= path.keyframes.get(i).tick && totalTicks < path.keyframes.get(i+1).tick) {
                return i;
            }
        }
        return Math.max(0, path.keyframes.size() - 2);
    }

    private static int getSegmentColor(PathType type) {
        return switch (type) {
            case LINEAR -> COLOR_LINEAR;
            case CUBIC_BEZIER -> COLOR_CUBIC_BEZIER;
            case HERMITE -> COLOR_HERMITE;
            case BSPLINE -> COLOR_BSPLINE;
            case CIRCULAR -> COLOR_CIRCULAR;
            case ELLIPTICAL -> COLOR_ELLIPTICAL;
            case SPIRAL -> COLOR_SPIRAL;
            case HELIX -> COLOR_HELIX;
            case STEP -> COLOR_STEP;
            case NONE -> COLOR_NONE;
            default -> COLOR_CATMULL_ROM;
        };
    }

    private static void drawLine(VertexConsumer builder, PoseStack pose, Vec3 a, Vec3 b, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float bl = (color & 0xFF) / 255f;
        float alpha = ((color >> 24) & 0xFF) / 255f;

        builder.vertex(pose.last().pose(), (float)a.x, (float)a.y, (float)a.z)
                .color(r, g, bl, alpha).normal(0, 1, 0).endVertex();
        builder.vertex(pose.last().pose(), (float)b.x, (float)b.y, (float)b.z)
                .color(r, g, bl, alpha).normal(0, 1, 0).endVertex();
    }

    private static void drawDashedLine(VertexConsumer builder, PoseStack pose, Vec3 a, Vec3 b, int color, int segments) {
        Vec3 dir = b.subtract(a);
        for (int i = 0; i < segments; i += 2) {
            float t0 = i / (float)segments;
            float t1 = Math.min((i + 1) / (float)segments, 1f);
            Vec3 p0 = a.add(dir.scale(t0));
            Vec3 p1 = a.add(dir.scale(t1));
            drawLine(builder, pose, p0, p1, color);
        }
    }

    private static void drawPoint(VertexConsumer builder, PoseStack pose, Vec3 p, int color) {
        float size = 0.12f;
        drawLine(builder, pose, new Vec3(p.x - size, p.y, p.z), new Vec3(p.x + size, p.y, p.z), color);
        drawLine(builder, pose, new Vec3(p.x, p.y - size, p.z), new Vec3(p.x, p.y + size, p.z), color);
        drawLine(builder, pose, new Vec3(p.x, p.y, p.z - size), new Vec3(p.x, p.y, p.z + size), color);
    }

    private static void drawDirectionArrow(VertexConsumer builder, PoseStack pose, Vec3 pos, float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        double dx = -Math.sin(yawRad) * Math.cos(pitchRad) * 0.5;
        double dy = -Math.sin(pitchRad) * 0.5;
        double dz = Math.cos(yawRad) * Math.cos(pitchRad) * 0.5;
        Vec3 tip = pos.add(dx, dy, dz);
        drawLine(builder, pose, pos, tip, COLOR_DIRECTION);

        Vec3 side1 = new Vec3(-dz * 0.15, dy * 0.1, dx * 0.15);
        Vec3 side2 = new Vec3(dz * 0.15, dy * 0.1, -dx * 0.15);
        drawLine(builder, pose, tip, tip.add(side1), COLOR_DIRECTION);
        drawLine(builder, pose, tip, tip.add(side2), COLOR_DIRECTION);
    }

    private static void drawOrbitCircle(VertexConsumer builder, PoseStack pose, Vec3 center, float radius) {
        int segments = 32;
        Vec3 prev = null;
        for (int i = 0; i <= segments; i++) {
            float angle = (i / (float)segments) * (float)(Math.PI * 2);
            Vec3 curr = new Vec3(
                    center.x + Math.cos(angle) * radius,
                    center.y,
                    center.z + Math.sin(angle) * radius
            );
            if (prev != null) {
                drawLine(builder, pose, prev, curr, COLOR_ORBIT);
            }
            prev = curr;
        }
    }
}