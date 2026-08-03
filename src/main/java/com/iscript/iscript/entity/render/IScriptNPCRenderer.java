package com.iscript.iscript.entity.render;

import com.iscript.iscript.IScriptMod;
import com.iscript.iscript.entity.IScriptNPCEntity;
import com.iscript.iscript.skin.SkinManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class IScriptNPCRenderer extends LivingEntityRenderer<IScriptNPCEntity, PlayerModel<IScriptNPCEntity>> {
    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(IScriptMod.MOD_ID, "textures/entity/npc.png");

    public IScriptNPCRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(IScriptNPCEntity entity) {
        String skin = entity.getSkin();
        if (skin == null || skin.isEmpty()) {
            return DEFAULT_TEXTURE;
        }

        if (skin.startsWith("http://") || skin.startsWith("https://")) {
            ResourceLocation loaded = SkinManager.getOrLoad(skin);
            return loaded != null ? loaded : DEFAULT_TEXTURE;
        }

        try {
            return new ResourceLocation(skin);
        } catch (Exception e) {
            return DEFAULT_TEXTURE;
        }
    }

    @Override
    protected void scale(IScriptNPCEntity entity, PoseStack poseStack, float partialTickTime) {
        float scale = entity.getScale();
        poseStack.scale(scale, scale, scale);
        super.scale(entity, poseStack, partialTickTime);
    }

    @Override
    protected boolean shouldShowName(IScriptNPCEntity entity) {
        return entity.isCustomNameVisible() && entity.getCustomName() != null;
    }
}