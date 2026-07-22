package com.iscript.iscript.morph.animation;

import com.iscript.iscript.morph.MorphData;
import com.iscript.iscript.morph.MorphManager;
import com.iscript.iscript.morph.model.GeoModel;

public class AnimationController {
    private final MorphData morphData;

    public AnimationController(MorphData data) {
        this.morphData = data;
    }

    public float[] getBoneRotation(String boneName, float time) {
        String animName = morphData.getCurrentAnimation();
        if (animName == null || animName.isEmpty()) return new float[]{0,0,0};

        GeoModel model = MorphManager.getModel(morphData.getModelId());
        AnimationData animData = MorphManager.getAnimation(morphData.getModelId());
        if (model == null || animData == null) return new float[]{0,0,0};

        AnimationData.Animation anim = animData.getAnimation(animName);
        if (anim == null) return new float[]{0,0,0};

        AnimationData.BoneAnimation ba = anim.getBoneAnimations().get(boneName);
        if (ba == null) return new float[]{0,0,0};

        AnimationData.KeyframeTrack rotTrack = ba.getTracks().get("rotation");
        if (rotTrack == null) return new float[]{0,0,0};

        return rotTrack.interpolate(time, anim.isLoop(), anim.getLength());
    }

    public float[] getBonePosition(String boneName, float time) {
        String animName = morphData.getCurrentAnimation();
        if (animName == null || animName.isEmpty()) return new float[]{0,0,0};

        GeoModel model = MorphManager.getModel(morphData.getModelId());
        AnimationData animData = MorphManager.getAnimation(morphData.getModelId());
        if (model == null || animData == null) return new float[]{0,0,0};

        AnimationData.Animation anim = animData.getAnimation(animName);
        if (anim == null) return new float[]{0,0,0};

        AnimationData.BoneAnimation ba = anim.getBoneAnimations().get(boneName);
        if (ba == null) return new float[]{0,0,0};

        AnimationData.KeyframeTrack posTrack = ba.getTracks().get("position");
        if (posTrack == null) return new float[]{0,0,0};

        return posTrack.interpolate(time, anim.isLoop(), anim.getLength());
    }
}