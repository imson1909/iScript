package com.iscript.imson.morph.animation;

import com.iscript.imson.morph.MorphData;
import com.iscript.imson.morph.MorphManager;
import com.iscript.imson.morph.model.GeoModel;

import java.util.ArrayList;
import java.util.List;

public class AnimationController {
    private final MorphData morphData;
    private float prevTime;
    private float currentTime;
    private float speed = 1.0f;
    private boolean looping = true;
    private float blend = 1.0f;
    private float transitionLength = 0.0f;
    private float transitionProgress = 0.0f;
    private String lastAnimation;
    private final List<AnimationEventListener> eventListeners = new ArrayList<>();

    public AnimationController(MorphData data) {
        this.morphData = data;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public boolean isLooping() {
        return this.looping;
    }

    public void setBlend(float blend) {
        this.blend = blend;
    }

    public float getBlend() {
        return this.blend;
    }

    public void setTransitionLength(float ticks) {
        this.transitionLength = ticks;
    }

    public void addEventListener(AnimationEventListener listener) {
        this.eventListeners.add(listener);
    }

    public void removeEventListener(AnimationEventListener listener) {
        this.eventListeners.remove(listener);
    }

    public void update(float deltaTime) {
        this.prevTime = this.currentTime;
        this.currentTime += deltaTime * this.speed;

        if (this.transitionProgress < 1.0f && this.transitionLength > 0.0f) {
            this.transitionProgress = Math.min(1.0f, this.transitionProgress + deltaTime / this.transitionLength);
        }

        String animName = morphData.getCurrentAnimation();
        if (animName != null && !animName.equals(this.lastAnimation)) {
            if (this.lastAnimation != null && this.transitionLength > 0.0f) {
                this.transitionProgress = 0.0f;
            }
            this.lastAnimation = animName;
        }

        processEvents(this.currentTime, this.prevTime);
    }

    private void processEvents(float current, float previous) {
        String animName = morphData.getCurrentAnimation();
        if (animName == null || animName.isEmpty()) return;

        AnimationData animData = MorphManager.getAnimation(morphData.getModelId());
        if (animData == null) return;

        AnimationData.Animation anim = animData.getAnimation(animName);
        if (anim == null) return;

        for (AnimationData.AnimationEvent event : anim.getSoundEvents()) {
            if (shouldFireEvent(event.time, current, previous, anim.isLoop(), anim.getLength())) {
                for (AnimationEventListener listener : eventListeners) {
                    listener.onSoundEvent(event);
                }
            }
        }

        for (AnimationData.AnimationEvent event : anim.getParticleEvents()) {
            if (shouldFireEvent(event.time, current, previous, anim.isLoop(), anim.getLength())) {
                for (AnimationEventListener listener : eventListeners) {
                    listener.onParticleEvent(event);
                }
            }
        }

        for (AnimationData.AnimationEvent event : anim.getCustomEvents()) {
            if (shouldFireEvent(event.time, current, previous, anim.isLoop(), anim.getLength())) {
                for (AnimationEventListener listener : eventListeners) {
                    listener.onCustomEvent(event);
                }
            }
        }
    }

    private boolean shouldFireEvent(float eventTime, float current, float previous, boolean loop, float animLength) {
        if (loop && animLength > 0) {
            float modCurrent = current % animLength;
            float modPrevious = previous % animLength;
            if (modPrevious > modCurrent) {
                return eventTime >= modPrevious || eventTime <= modCurrent;
            }
            return eventTime > modPrevious && eventTime <= modCurrent;
        }
        return eventTime > previous && eventTime <= current;
    }

    public float[] getBoneRotation(String boneName, float time) {
        return getBoneTrack(boneName, "rotation", time, new float[]{0,0,0});
    }

    public float[] getBonePosition(String boneName, float time) {
        return getBoneTrack(boneName, "position", time, new float[]{0,0,0});
    }

    public float[] getBoneScale(String boneName, float time) {
        return getBoneTrack(boneName, "scale", time, new float[]{1,1,1});
    }

    private float[] getBoneTrack(String boneName, String trackType, float time, float[] defaultValue) {
        String animName = morphData.getCurrentAnimation();
        if (animName == null || animName.isEmpty()) return defaultValue;

        GeoModel model = MorphManager.getModel(morphData.getModelId());
        AnimationData animData = MorphManager.getAnimation(morphData.getModelId());
        if (model == null || animData == null) return defaultValue;

        AnimationData.Animation anim = animData.getAnimation(animName);
        if (anim == null) return defaultValue;

        AnimationData.BoneAnimation ba = anim.getBoneAnimations().get(boneName);
        if (ba == null) return defaultValue;

        AnimationData.KeyframeTrack track = ba.getTracks().get(trackType);
        if (track == null) return defaultValue;

        float[] current = track.interpolate(time, looping, anim.getLength());

        if (this.lastAnimation != null && !this.lastAnimation.equals(animName) && this.transitionProgress < 1.0f && this.transitionLength > 0.0f) {
            AnimationData.Animation lastAnim = animData.getAnimation(this.lastAnimation);
            if (lastAnim != null) {
                AnimationData.BoneAnimation lastBa = lastAnim.getBoneAnimations().get(boneName);
                if (lastBa != null) {
                    AnimationData.KeyframeTrack lastTrack = lastBa.getTracks().get(trackType);
                    if (lastTrack != null) {
                        float[] last = lastTrack.interpolate(time, looping, lastAnim.getLength());
                        float t = this.transitionProgress;
                        for (int i = 0; i < current.length; i++) {
                            current[i] = last[i] + (current[i] - last[i]) * t;
                        }
                    }
                }
            }
        }

        return current;
    }

    public void reset() {
        this.currentTime = 0;
        this.prevTime = 0;
        this.transitionProgress = 1.0f;
    }

    public interface AnimationEventListener {
        void onSoundEvent(AnimationData.AnimationEvent event);
        void onParticleEvent(AnimationData.AnimationEvent event);
        void onCustomEvent(AnimationData.AnimationEvent event);
    }
}