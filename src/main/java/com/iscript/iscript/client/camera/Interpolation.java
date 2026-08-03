package com.iscript.iscript.client.camera;

public enum Interpolation {
    LINEAR,
    EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD,
    EASE_IN_CUBIC, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC,
    EASE_IN_QUART, EASE_OUT_QUART, EASE_IN_OUT_QUART,
    EASE_IN_SINE, EASE_OUT_SINE, EASE_IN_OUT_SINE,
    EASE_IN_EXPO, EASE_OUT_EXPO, EASE_IN_OUT_EXPO,
    EASE_IN_CIRC, EASE_OUT_CIRC, EASE_IN_OUT_CIRC,
    EASE_IN_BACK, EASE_OUT_BACK, EASE_IN_OUT_BACK,
    EASE_IN_ELASTIC, EASE_OUT_ELASTIC, EASE_IN_OUT_ELASTIC,
    EASE_IN_BOUNCE, EASE_OUT_BOUNCE, EASE_IN_OUT_BOUNCE;

    public static final Interpolation[] VALUES = values();

    public float apply(float t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        switch (this) {
            case EASE_IN_QUAD: return t * t;
            case EASE_OUT_QUAD: return 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT_QUAD: return t < 0.5f ? 2 * t * t : 1 - (float)Math.pow(-2 * t + 2, 2) / 2;
            case EASE_IN_CUBIC: return t * t * t;
            case EASE_OUT_CUBIC: return 1 - (float)Math.pow(1 - t, 3);
            case EASE_IN_OUT_CUBIC: return t < 0.5f ? 4 * t * t * t : 1 - (float)Math.pow(-2 * t + 2, 3) / 2;
            case EASE_IN_QUART: return t * t * t * t;
            case EASE_OUT_QUART: return 1 - (float)Math.pow(1 - t, 4);
            case EASE_IN_OUT_QUART: return t < 0.5f ? 8 * t * t * t * t : 1 - (float)Math.pow(-2 * t + 2, 4) / 2;
            case EASE_IN_SINE: return 1 - (float)Math.cos(t * Math.PI / 2);
            case EASE_OUT_SINE: return (float)Math.sin(t * Math.PI / 2);
            case EASE_IN_OUT_SINE: return (float)(-(Math.cos(Math.PI * t) - 1) / 2);
            case EASE_IN_EXPO: return (float)Math.pow(2, 10 * (t - 1));
            case EASE_OUT_EXPO: return 1 - (float)Math.pow(2, -10 * t);
            case EASE_IN_OUT_EXPO: return t < 0.5f ? (float)Math.pow(2, 20 * t - 10) / 2 : (2 - (float)Math.pow(2, -20 * t + 10)) / 2;
            case EASE_IN_CIRC: return 1 - (float)Math.sqrt(1 - t * t);
            case EASE_OUT_CIRC: return (float)Math.sqrt(1 - (t - 1) * (t - 1));
            case EASE_IN_OUT_CIRC: return t < 0.5f ? (1 - (float)Math.sqrt(1 - 4 * t * t)) / 2 : ((float)Math.sqrt(1 - 4 * (t - 1) * (t - 1)) + 1) / 2;
            case EASE_IN_BACK: {
                float c1 = 1.70158f, c3 = c1 + 1;
                return c3 * t * t * t - c1 * t * t;
            }
            case EASE_OUT_BACK: {
                float c1 = 1.70158f, c3 = c1 + 1;
                return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2);
            }
            case EASE_IN_OUT_BACK: {
                float c1 = 1.70158f, c2 = c1 * 1.525f;
                return t < 0.5f
                        ? ((float)Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
                        : ((float)Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2;
            }
            case EASE_IN_ELASTIC: {
                float c4 = (2 * (float)Math.PI) / 3;
                return t == 0 ? 0 : -(float)Math.pow(2, 10 * t - 10) * (float)Math.sin((t * 10 - 10.75f) * c4);
            }
            case EASE_OUT_ELASTIC: {
                float c4 = (2 * (float)Math.PI) / 3;
                return t == 1 ? 1 : (float)Math.pow(2, -10 * t) * (float)Math.sin((t * 10 - 0.75f) * c4) + 1;
            }
            case EASE_IN_OUT_ELASTIC: {
                float c5 = (2 * (float)Math.PI) / 4.5f;
                return t == 0 ? 0 : t == 1 ? 1 : t < 0.5f
                        ? -((float)Math.pow(2, 20 * t - 10) * (float)Math.sin((20 * t - 11.125f) * c5)) / 2
                        : ((float)Math.pow(2, -20 * t + 10) * (float)Math.sin((20 * t - 11.125f) * c5)) / 2 + 1;
            }
            case EASE_IN_BOUNCE: return 1 - bounceOut(1 - t);
            case EASE_OUT_BOUNCE: return bounceOut(t);
            case EASE_IN_OUT_BOUNCE: return t < 0.5f ? (1 - bounceOut(1 - 2 * t)) / 2 : (1 + bounceOut(2 * t - 1)) / 2;
            default: return t;
        }
    }

    private static float bounceOut(float t) {
        float n1 = 7.5625f, d1 = 2.75f;
        if (t < 1 / d1) return n1 * t * t;
        else if (t < 2 / d1) return n1 * (t -= 1.5f / d1) * t + 0.75f;
        else if (t < 2.5f / d1) return n1 * (t -= 2.25f / d1) * t + 0.9375f;
        else return n1 * (t -= 2.625f / d1) * t + 0.984375f;
    }

    public static float lerp(float a, float b, float t, Interpolation interp) {
        return a + (b - a) * interp.apply(t);
    }

    public static double lerp(double a, double b, float t, Interpolation interp) {
        return a + (b - a) * interp.apply(t);
    }

    public static float lerpYaw(float a, float b, float t, Interpolation interp) {
        float diff = b - a;
        while (diff < -180f) diff += 360f;
        while (diff >= 180f) diff -= 360f;
        return a + diff * interp.apply(t);
    }

    public static float lerpPitch(float a, float b, float t, Interpolation interp) {
        return lerpYaw(a, b, t, interp);
    }
}