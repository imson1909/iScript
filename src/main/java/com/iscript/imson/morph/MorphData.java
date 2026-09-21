package com.iscript.imson.morph;

import com.iscript.imson.morph.animation.AnimationController;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

public class MorphData {
    public static final Capability<MorphData> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private String modelId = "";
    private String textureId = "";
    private String currentAnimation = "";
    private float scale = 1.0f;
    private boolean morphed = false;
    private boolean visible = true;
    private int animationTick = 0;
    private AnimationController animationController;

    private String displayName = "";
    private float hitboxWidth = 0.6f;
    private float hitboxHeight = 1.8f;
    private float hitboxEye = 1.62f;
    private float hitboxSneakingHeight = 1.5f;

    private String customTexture = "";
    private float animationSpeed = 1.0f;
    private boolean animates = true;
    private int animationDuration = 20;
    private String interpolation = "LINEAR";

    private Map<String, BodyPartTransform> bodyParts = new HashMap<>();
    private Map<String, ActionConfig> actions = new HashMap<>();

    private boolean canFly = false;
    private boolean canSwim = false;
    private boolean canClimb = false;
    private boolean fireImmune = false;
    private boolean waterBreathing = false;
    private boolean nightVision = false;

    private String poseName = "";
    private boolean actionPlayerMode = false;
    private float globalSize = 1.0f;
    private float guiSize = 30.0f;
    private String skinPath = "";

    public static class BodyPartTransform {
        public String attachedId = "";
        public String targetBone = "";
        public float rx, ry, rz;
        public float sx = 1, sy = 1, sz = 1;
        public float tx, ty, tz;
    }

    public static class ActionConfig {
        public String id = "";
        public float speed = 1.0f;
        public int fade = 5;
    }

    public String getModelId() { return modelId; }
    public void setModelId(String id) { this.modelId = id != null ? id : ""; }
    public String getTextureId() { return textureId; }
    public void setTextureId(String id) { this.textureId = id != null ? id : ""; }
    public String getCurrentAnimation() { return currentAnimation; }
    public void setCurrentAnimation(String anim) {
        this.currentAnimation = anim != null ? anim : "";
        if (this.animationController != null) this.animationController.reset();
    }
    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }
    public boolean isMorphed() { return morphed; }
    public void setMorphed(boolean v) { this.morphed = v; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }
    public int getAnimationTick() { return animationTick; }
    public void incrementAnimationTick() { this.animationTick++; }
    public void resetAnimationTick() { this.animationTick = 0; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String name) { this.displayName = name != null ? name : ""; }
    public float getHitboxWidth() { return hitboxWidth; }
    public void setHitboxWidth(float w) { this.hitboxWidth = w; }
    public float getHitboxHeight() { return hitboxHeight; }
    public void setHitboxHeight(float h) { this.hitboxHeight = h; }
    public float getHitboxEye() { return hitboxEye; }
    public void setHitboxEye(float e) { this.hitboxEye = e; }
    public float getHitboxSneakingHeight() { return hitboxSneakingHeight; }
    public void setHitboxSneakingHeight(float sh) { this.hitboxSneakingHeight = sh; }
    public String getCustomTexture() { return customTexture; }
    public void setCustomTexture(String tex) { this.customTexture = tex != null ? tex : ""; }
    public float getAnimationSpeed() { return animationSpeed; }
    public void setAnimationSpeed(float speed) { this.animationSpeed = speed; }
    public boolean isAnimates() { return animates; }
    public void setAnimates(boolean v) { this.animates = v; }
    public int getAnimationDuration() { return animationDuration; }
    public void setAnimationDuration(int d) { this.animationDuration = d; }
    public String getInterpolation() { return interpolation; }
    public void setInterpolation(String i) { this.interpolation = i != null ? i : "LINEAR"; }
    public Map<String, BodyPartTransform> getBodyParts() { return bodyParts; }
    public Map<String, ActionConfig> getActions() { return actions; }
    public boolean canFly() { return canFly; }
    public void setCanFly(boolean v) { this.canFly = v; }
    public boolean canSwim() { return canSwim; }
    public void setCanSwim(boolean v) { this.canSwim = v; }
    public boolean canClimb() { return canClimb; }
    public void setCanClimb(boolean v) { this.canClimb = v; }
    public boolean isFireImmune() { return fireImmune; }
    public void setFireImmune(boolean v) { this.fireImmune = v; }
    public boolean hasWaterBreathing() { return waterBreathing; }
    public void setWaterBreathing(boolean v) { this.waterBreathing = v; }
    public boolean hasNightVision() { return nightVision; }
    public void setNightVision(boolean v) { this.nightVision = v; }
    public String getPoseName() { return poseName; }
    public void setPoseName(String p) { this.poseName = p != null ? p : ""; }
    public boolean isActionPlayerMode() { return actionPlayerMode; }
    public void setActionPlayerMode(boolean v) { this.actionPlayerMode = v; }
    public float getGlobalSize() { return globalSize; }
    public void setGlobalSize(float s) { this.globalSize = s; }
    public float getGuiSize() { return guiSize; }
    public void setGuiSize(float s) { this.guiSize = s; }
    public String getSkinPath() { return skinPath; }
    public void setSkinPath(String p) { this.skinPath = p != null ? p : ""; }
    public AnimationController getAnimationController() {
        if (this.animationController == null) this.animationController = new AnimationController(this);
        return this.animationController;
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ModelId", modelId);
        tag.putString("TextureId", textureId);
        tag.putString("CurrentAnimation", currentAnimation);
        tag.putFloat("Scale", scale);
        tag.putBoolean("Morphed", morphed);
        tag.putBoolean("Visible", visible);
        tag.putInt("AnimationTick", animationTick);
        tag.putString("DisplayName", displayName);
        tag.putFloat("HitboxWidth", hitboxWidth);
        tag.putFloat("HitboxHeight", hitboxHeight);
        tag.putFloat("HitboxEye", hitboxEye);
        tag.putFloat("HitboxSneakingHeight", hitboxSneakingHeight);
        tag.putString("CustomTexture", customTexture);
        tag.putFloat("AnimationSpeed", animationSpeed);
        tag.putBoolean("Animates", animates);
        tag.putInt("AnimationDuration", animationDuration);
        tag.putString("Interpolation", interpolation);
        tag.putString("PoseName", poseName);
        tag.putBoolean("ActionPlayerMode", actionPlayerMode);
        tag.putFloat("GlobalSize", globalSize);
        tag.putFloat("GuiSize", guiSize);
        tag.putString("SkinPath", skinPath);

        CompoundTag bpTag = new CompoundTag();
        for (Map.Entry<String, BodyPartTransform> e : bodyParts.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putFloat("rx", e.getValue().rx); t.putFloat("ry", e.getValue().ry); t.putFloat("rz", e.getValue().rz);
            t.putFloat("sx", e.getValue().sx); t.putFloat("sy", e.getValue().sy); t.putFloat("sz", e.getValue().sz);
            t.putFloat("tx", e.getValue().tx); t.putFloat("ty", e.getValue().ty); t.putFloat("tz", e.getValue().tz);
            bpTag.put(e.getKey(), t);
        }
        tag.put("BodyParts", bpTag);

        CompoundTag actTag = new CompoundTag();
        for (Map.Entry<String, ActionConfig> e : actions.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("id", e.getValue().id);
            t.putFloat("speed", e.getValue().speed);
            t.putInt("fade", e.getValue().fade);
            actTag.put(e.getKey(), t);
        }
        tag.put("Actions", actTag);
        tag.putBoolean("CanFly", canFly);
        tag.putBoolean("CanSwim", canSwim);
        tag.putBoolean("CanClimb", canClimb);
        tag.putBoolean("FireImmune", fireImmune);
        tag.putBoolean("WaterBreathing", waterBreathing);
        tag.putBoolean("NightVision", nightVision);
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
        displayName = tag.getString("DisplayName");
        hitboxWidth = tag.contains("HitboxWidth") ? tag.getFloat("HitboxWidth") : 0.6f;
        hitboxHeight = tag.contains("HitboxHeight") ? tag.getFloat("HitboxHeight") : 1.8f;
        hitboxEye = tag.contains("HitboxEye") ? tag.getFloat("HitboxEye") : 1.62f;
        hitboxSneakingHeight = tag.contains("HitboxSneakingHeight") ? tag.getFloat("HitboxSneakingHeight") : 1.5f;
        customTexture = tag.getString("CustomTexture");
        animationSpeed = tag.contains("AnimationSpeed") ? tag.getFloat("AnimationSpeed") : 1.0f;
        animates = tag.contains("Animates") ? tag.getBoolean("Animates") : true;
        animationDuration = tag.contains("AnimationDuration") ? tag.getInt("AnimationDuration") : 20;
        interpolation = tag.contains("Interpolation") ? tag.getString("Interpolation") : "LINEAR";
        poseName = tag.getString("PoseName");
        actionPlayerMode = tag.getBoolean("ActionPlayerMode");
        globalSize = tag.contains("GlobalSize") ? tag.getFloat("GlobalSize") : 1.0f;
        guiSize = tag.contains("GuiSize") ? tag.getFloat("GuiSize") : 30.0f;
        skinPath = tag.getString("SkinPath");

        bodyParts.clear();
        if (tag.contains("BodyParts")) {
            CompoundTag bpTag = tag.getCompound("BodyParts");
            for (String key : bpTag.getAllKeys()) {
                CompoundTag t = bpTag.getCompound(key);
                BodyPartTransform bpt = new BodyPartTransform();
                bpt.rx = t.getFloat("rx"); bpt.ry = t.getFloat("ry"); bpt.rz = t.getFloat("rz");
                bpt.sx = t.contains("sx") ? t.getFloat("sx") : 1;
                bpt.sy = t.contains("sy") ? t.getFloat("sy") : 1;
                bpt.sz = t.contains("sz") ? t.getFloat("sz") : 1;
                bpt.tx = t.getFloat("tx"); bpt.ty = t.getFloat("ty"); bpt.tz = t.getFloat("tz");
                bodyParts.put(key, bpt);
            }
        }
        actions.clear();
        if (tag.contains("Actions")) {
            CompoundTag actTag = tag.getCompound("Actions");
            for (String key : actTag.getAllKeys()) {
                CompoundTag t = actTag.getCompound(key);
                ActionConfig ac = new ActionConfig();
                ac.id = t.getString("id");
                ac.speed = t.contains("speed") ? t.getFloat("speed") : 1;
                ac.fade = t.contains("fade") ? t.getInt("fade") : 5;
                actions.put(key, ac);
            }
        }
        canFly = tag.getBoolean("CanFly");
        canSwim = tag.getBoolean("CanSwim");
        canClimb = tag.getBoolean("CanClimb");
        fireImmune = tag.getBoolean("FireImmune");
        waterBreathing = tag.getBoolean("WaterBreathing");
        nightVision = tag.getBoolean("NightVision");
    }

    public void copyFrom(MorphData other) {
        this.modelId = other.modelId;
        this.textureId = other.textureId;
        this.currentAnimation = other.currentAnimation;
        this.scale = other.scale;
        this.morphed = other.morphed;
        this.visible = other.visible;
        this.animationTick = other.animationTick;
        this.displayName = other.displayName;
        this.hitboxWidth = other.hitboxWidth;
        this.hitboxHeight = other.hitboxHeight;
        this.hitboxEye = other.hitboxEye;
        this.hitboxSneakingHeight = other.hitboxSneakingHeight;
        this.customTexture = other.customTexture;
        this.animationSpeed = other.animationSpeed;
        this.animates = other.animates;
        this.animationDuration = other.animationDuration;
        this.interpolation = other.interpolation;
        this.bodyParts = other.bodyParts;
        this.actions = other.actions;
        this.canFly = other.canFly;
        this.canSwim = other.canSwim;
        this.canClimb = other.canClimb;
        this.fireImmune = other.fireImmune;
        this.waterBreathing = other.waterBreathing;
        this.nightVision = other.nightVision;
        this.poseName = other.poseName;
        this.actionPlayerMode = other.actionPlayerMode;
        this.globalSize = other.globalSize;
        this.guiSize = other.guiSize;
        this.skinPath = other.skinPath;
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final MorphData data = new MorphData();
        private final LazyOptional<MorphData> optional = LazyOptional.of(() -> data);
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == CAPABILITY ? optional.cast() : LazyOptional.empty();
        }
        @Override
        public CompoundTag serializeNBT() { return data.serialize(); }
        @Override
        public void deserializeNBT(CompoundTag tag) { data.deserialize(tag); }
    }
}