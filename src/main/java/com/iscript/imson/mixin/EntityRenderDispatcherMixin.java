package com.iscript.imson.mixin;

import com.iscript.imson.morph.MorphData;
import com.iscript.imson.morph.render.MorphRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    // Ссылаемся на оригинальный приватный метод shadowRadius
    @Shadow
    private static void renderShadow(PoseStack poseStack, MultiBufferSource buffer, Entity entity, float weight, float partialTicks, LevelReader level, float size) {}

    // Блокируем автоматические двойные тени
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void onRenderShadow(PoseStack poseStack, MultiBufferSource buffer, Entity entity, float weight, float partialTicks, LevelReader level, float size, CallbackInfo ci) {
        if (MorphRenderer.isRenderingDummyShadowBlock) {
            ci.cancel();
            return;
        }

        if (entity instanceof Player player) {
            MorphData data = player.getCapability(MorphData.CAPABILITY).orElse(null);
            if (data != null && data.isMorphed() && data.isVisible()) {
                ci.cancel();
            }
        }
    }

    /**
     * НЕ-статический метод (публичный), который будет добавлен прямо в EntityRenderDispatcher.
     * Мы сможем вызвать его у любого объекта диспетчера рендеринга!
     */
    public void renderMorphedPlayerShadow(PoseStack poseStack, MultiBufferSource buffer, Player player, float partialTicks) {
        MorphData data = player.getCapability(MorphData.CAPABILITY).orElse(null);
        if (data == null || !data.isMorphed() || !data.isVisible()) return;

        float targetRadius = MorphRenderer.getTargetShadowRadius(data.getModelId());
        float finalSize = targetRadius * data.getScale();
        float weight = 1.0f;

        // Вызываем приватный метод. Поскольку метод НЕ статический, Java разрешает это делать без ошибок!
        renderShadow(poseStack, buffer, player, weight, partialTicks, player.level(), finalSize);
    }
}
