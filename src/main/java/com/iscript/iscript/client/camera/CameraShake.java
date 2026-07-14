package com.iscript.iscript.client.camera;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class CameraShake {
    private float trauma = 0f;
    private float traumaDecay = 1.5f;
    private float maxAngle = 5f;
    private float maxOffset = 0.3f;
    private final Random random = new Random();
    private float seed;

    public CameraShake() {
        this.seed = random.nextFloat() * 100f;
    }

    public void addTrauma(float amount) {
        trauma = Math.min(1f, trauma + amount);
    }

    public void setTrauma(float trauma) {
        this.trauma = Mth.clamp(trauma, 0f, 1f);
    }

    public float getTrauma() {
        return trauma;
    }

    public void setDecay(float decay) {
        this.traumaDecay = decay;
    }

    public float getDecay() {
        return traumaDecay;
    }

    public void setMaxAngle(float angle) {
        this.maxAngle = angle;
    }

    public float getMaxAngle() {
        return maxAngle;
    }

    public void setMaxOffset(float offset) {
        this.maxOffset = offset;
    }

    public float getMaxOffset() {
        return maxOffset;
    }

    public void update(float deltaTime) {
        trauma = Math.max(0f, trauma - traumaDecay * deltaTime);
    }

    public boolean isActive() {
        return trauma > 0.001f;
    }

    public float getShakeAmount() {
        return trauma * trauma;
    }

    public Vec3 getOffset(float time) {
        float amount = getShakeAmount();
        if (amount < 0.001f) return Vec3.ZERO;

        float x = noise(seed, time) * maxOffset * amount;
        float y = noise(seed + 10, time) * maxOffset * amount;
        float z = noise(seed + 20, time) * maxOffset * amount;

        return new Vec3(x, y, z);
    }

    public float getYawShake(float time) {
        float amount = getShakeAmount();
        if (amount < 0.001f) return 0f;
        return noise(seed + 30, time) * maxAngle * amount;
    }

    public float getPitchShake(float time) {
        float amount = getShakeAmount();
        if (amount < 0.001f) return 0f;
        return noise(seed + 40, time) * maxAngle * amount;
    }

    public float getRollShake(float time) {
        float amount = getShakeAmount();
        if (amount < 0.001f) return 0f;
        return noise(seed + 50, time) * maxAngle * amount;
    }

    private float noise(float seed, float time) {
        return (float) Math.sin(seed + time * 15f) * (float) Math.cos(seed * 0.5f + time * 23f);
    }
}