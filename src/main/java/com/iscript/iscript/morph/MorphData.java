package com.iscript.iscript.morph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MorphData {
    public static final Capability<MorphData> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private String modelId = "";
    private String textureId = "";
    private String currentAnimation = "";
    private float scale = 1.0f;
    private boolean morphed = false;
    private boolean visible = true;
    private int animationTick = 0;

    public String getModelId() { return modelId; }
    public void setModelId(String id) { this.modelId = id != null ? id : ""; }

    public String getTextureId() { return textureId; }
    public void setTextureId(String id) { this.textureId = id != null ? id : ""; }

    public String getCurrentAnimation() { return currentAnimation; }
    public void setCurrentAnimation(String anim) { this.currentAnimation = anim != null ? anim : ""; }

    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }

    public boolean isMorphed() { return morphed; }
    public void setMorphed(boolean v) { this.morphed = v; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }

    public int getAnimationTick() { return animationTick; }
    public void incrementAnimationTick() { this.animationTick++; }
    public void resetAnimationTick() { this.animationTick = 0; }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ModelId", modelId);
        tag.putString("TextureId", textureId);
        tag.putString("CurrentAnimation", currentAnimation);
        tag.putFloat("Scale", scale);
        tag.putBoolean("Morphed", morphed);
        tag.putBoolean("Visible", visible);
        tag.putInt("AnimationTick", animationTick);
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        modelId = tag.getString("ModelId");
        textureId = tag.getString("TextureId");
        currentAnimation = tag.getString("CurrentAnimation");
        scale = tag.getFloat("Scale");
        morphed = tag.getBoolean("Morphed");
        visible = tag.getBoolean("Visible");
        animationTick = tag.getInt("AnimationTick");
    }

    public void copyFrom(MorphData other) {
        this.modelId = other.modelId;
        this.textureId = other.textureId;
        this.currentAnimation = other.currentAnimation;
        this.scale = other.scale;
        this.morphed = other.morphed;
        this.visible = other.visible;
        this.animationTick = other.animationTick;
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final MorphData data = new MorphData();
        private final LazyOptional<MorphData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == CAPABILITY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serialize();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.deserialize(tag);
        }
    }
}