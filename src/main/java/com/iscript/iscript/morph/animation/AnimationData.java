package com.iscript.iscript.morph.animation;

import com.iscript.iscript.IScriptMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AnimationData {
    private final Map<String, Animation> animations = new HashMap<>();

    public static AnimationData parse(JsonObject json) {
        AnimationData data = new AnimationData();
        if (!json.has("animations")) return data;
        JsonObject anims = json.getAsJsonObject("animations");
        for (Map.Entry<String, JsonElement> entry : anims.entrySet()) {
            data.animations.put(entry.getKey(), Animation.parse(entry.getKey(), entry.getValue().getAsJsonObject()));
        }
        return data;
    }

    public Animation getAnimation(String name) {
        return animations.get(name);
    }

    public Map<String, Animation> getAll() {
        return animations;
    }

    public static class Animation {
        private String name;
        private boolean loop = false;
        private float animationLength = 1.0f;
        private final Map<String, BoneAnimation> boneAnimations = new HashMap<>();
        private final List<AnimationEvent> soundEvents = new ArrayList<>();
        private final List<AnimationEvent> particleEvents = new ArrayList<>();
        private final List<AnimationEvent> customEvents = new ArrayList<>();

        public static Animation parse(String name, JsonObject json) {
            Animation a = new Animation();
            a.name = name;
            a.loop = json.has("loop") && json.get("loop").getAsBoolean();
            if (json.has("animation_length")) {
                a.animationLength = json.get("animation_length").getAsFloat();
            } else {
                a.animationLength = computeLength(json);
            }
            if (json.has("bones")) {
                JsonObject bones = json.getAsJsonObject("bones");
                for (Map.Entry<String, JsonElement> e : bones.entrySet()) {
                    a.boneAnimations.put(e.getKey(), BoneAnimation.parse(e.getValue().getAsJsonObject()));
                }
            }
            if (json.has("sound_effects")) {
                JsonObject sounds = json.getAsJsonObject("sound_effects");
                for (Map.Entry<String, JsonElement> e : sounds.entrySet()) {
                    a.soundEvents.add(AnimationEvent.parse(e.getKey(), e.getValue().getAsJsonObject(), AnimationEvent.Type.SOUND));
                }
            }
            if (json.has("particle_effects")) {
                JsonObject particles = json.getAsJsonObject("particle_effects");
                for (Map.Entry<String, JsonElement> e : particles.entrySet()) {
                    a.particleEvents.add(AnimationEvent.parse(e.getKey(), e.getValue().getAsJsonObject(), AnimationEvent.Type.PARTICLE));
                }
            }
            if (json.has("timeline")) {
                JsonObject timeline = json.getAsJsonObject("timeline");
                for (Map.Entry<String, JsonElement> e : timeline.entrySet()) {
                    a.customEvents.add(AnimationEvent.parse(e.getKey(), e.getValue().getAsJsonObject(), AnimationEvent.Type.CUSTOM));
                }
            }
            return a;
        }

        private static float computeLength(JsonObject json) {
            float maxTime = 0.0f;
            if (!json.has("bones")) return 1.0f;
            JsonObject bones = json.getAsJsonObject("bones");
            for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
                JsonObject boneObj = boneEntry.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> trackEntry : boneObj.entrySet()) {
                    String trackType = trackEntry.getKey();
                    if (!trackType.equals("rotation") && !trackType.equals("position") && !trackType.equals("scale"))
                        continue;
                    JsonElement trackEl = trackEntry.getValue();
                    if (trackEl.isJsonObject()) {
                        JsonObject trackObj = trackEl.getAsJsonObject();
                        for (Map.Entry<String, JsonElement> keyEntry : trackObj.entrySet()) {
                            String key = keyEntry.getKey();
                            if (key.equals("vector") || key.equals("lerp_mode") || key.equals("easing") || key.equals("easingArgs")) continue;
                            try {
                                float t = Float.parseFloat(key);
                                if (t > maxTime) maxTime = t;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
            for (String eventKey : new String[]{"sound_effects", "particle_effects", "timeline"}) {
                if (json.has(eventKey)) {
                    JsonObject events = json.getAsJsonObject(eventKey);
                    for (String key : events.keySet()) {
                        try {
                            float t = Float.parseFloat(key);
                            if (t > maxTime) maxTime = t;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            return maxTime > 0.0f ? maxTime : 1.0f;
        }

        public String getName() { return name; }
        public boolean isLoop() { return loop; }
        public float getLength() { return animationLength; }
        public Map<String, BoneAnimation> getBoneAnimations() { return boneAnimations; }
        public List<AnimationEvent> getSoundEvents() { return soundEvents; }
        public List<AnimationEvent> getParticleEvents() { return particleEvents; }
        public List<AnimationEvent> getCustomEvents() { return customEvents; }
    }

    public static class BoneAnimation {
        private final Map<String, KeyframeTrack> tracks = new HashMap<>();

        public static BoneAnimation parse(JsonObject json) {
            BoneAnimation ba = new BoneAnimation();
            if (json.has("rotation")) ba.tracks.put("rotation", KeyframeTrack.parse(json.get("rotation")));
            if (json.has("position")) ba.tracks.put("position", KeyframeTrack.parse(json.get("position")));
            if (json.has("scale")) ba.tracks.put("scale", KeyframeTrack.parse(json.get("scale")));
            return ba;
        }

        public Map<String, KeyframeTrack> getTracks() { return tracks; }
    }

    public static class KeyframeTrack {
        private final TreeMap<Float, float[]> keyframes = new TreeMap<>();

        public static KeyframeTrack parse(JsonElement el) {
            KeyframeTrack track = new KeyframeTrack();
            if (el == null || el.isJsonNull()) return track;

            if (el.isJsonArray()) {
                float[] arr = extractVector(el);
                if (arr != null) {
                    track.keyframes.put(0.0f, arr);
                }
                return track;
            }

            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();

                if (obj.has("vector")) {
                    float[] arr = extractVector(obj.get("vector"));
                    if (arr != null) {
                        track.keyframes.put(0.0f, arr);
                    }
                    return track;
                }

                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    String key = e.getKey();
                    if (key.equals("lerp_mode")) continue;
                    float time;
                    try {
                        time = Float.parseFloat(key);
                    } catch (NumberFormatException ex) {
                        continue;
                    }

                    float[] arr = extractVector(e.getValue());
                    if (arr != null) {
                        track.keyframes.put(time, arr);
                    }
                }
            }
            return track;
        }

        private static float[] extractVector(JsonElement el) {
            if (el == null || el.isJsonNull()) return null;

            if (el.isJsonArray()) {
                JsonArray array = el.getAsJsonArray();
                float[] arr = new float[array.size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = parseFloatSafe(array.get(i));
                }
                return arr;
            }

            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("vector")) {
                    return extractVector(obj.get("vector"));
                }
                if (obj.has("post")) {
                    return extractVector(obj.get("post"));
                }
            }

            return null;
        }

        private static float parseFloatSafe(JsonElement el) {
            if (el == null || el.isJsonNull()) return 0f;
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                return el.getAsFloat();
            }
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                String s = el.getAsString().trim();
                try {
                    return Float.parseFloat(s);
                } catch (NumberFormatException e) {
                    return 0f;
                }
            }
            return 0f;
        }

        public TreeMap<Float, float[]> getKeyframes() { return keyframes; }

        public float[] interpolate(float time, boolean loop, float animLength) {
            if (keyframes.isEmpty()) return new float[]{0,0,0};
            float t = loop ? time % animLength : Math.min(time, animLength);

            IScriptMod.LOGGER.info("[INTERP] time={}, animLength={}, t={}, keyframes={}", time, animLength, t, keyframes.keySet());

            Map.Entry<Float, float[]> floor = keyframes.floorEntry(t);
            Map.Entry<Float, float[]> ceil = keyframes.ceilingEntry(t);

            IScriptMod.LOGGER.info("[INTERP] floorKey={}, ceilKey={}", floor != null ? floor.getKey() : null, ceil != null ? ceil.getKey() : null);

            if (floor == null) return ceil != null ? ceil.getValue() : new float[]{0,0,0};
            if (ceil == null || floor.getKey().equals(ceil.getKey())) {
                IScriptMod.LOGGER.info("[INTERP] returning floor value: [{}, {}, {}]", floor.getValue()[0], floor.getValue()[1], floor.getValue()[2]);
                return floor.getValue();
            }

            float progress = (t - floor.getKey()) / (ceil.getKey() - floor.getKey());
            float[] prevVal = floor.getValue();
            float[] nextVal = ceil.getValue();
            float[] result = new float[prevVal.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = prevVal[i] + (nextVal[i] - prevVal[i]) * progress;
            }
            IScriptMod.LOGGER.info("[INTERP] progress={}, result=[{}, {}, {}]", progress, result[0], result[1], result[2]);
            return result;
        }
    }

    public static class AnimationEvent {
        public enum Type { SOUND, PARTICLE, CUSTOM }

        public float time;
        public Type type;
        public String effect;
        public String locator;
        public JsonObject data;

        public static AnimationEvent parse(String timeStr, JsonObject json, Type type) {
            AnimationEvent event = new AnimationEvent();
            try {
                event.time = Float.parseFloat(timeStr);
            } catch (NumberFormatException e) {
                event.time = 0;
            }
            event.type = type;
            if (json.has("effect")) event.effect = json.get("effect").getAsString();
            if (json.has("locator")) event.locator = json.get("locator").getAsString();
            event.data = json;
            return event;
        }
    }

    public enum AnimationEasing {
        LINEAR("linear") {
            @Override
            public float interpolate(float t, float[] args) { return t; }
        },
        SINE_IN("easeInSine") {
            @Override
            public float interpolate(float t, float[] args) { return (float) (1 - Math.cos(t * Math.PI / 2)); }
        },
        SINE_OUT("easeOutSine") {
            @Override
            public float interpolate(float t, float[] args) { return (float) Math.sin(t * Math.PI / 2); }
        },
        SINE_INOUT("easeInOutSine") {
            @Override
            public float interpolate(float t, float[] args) { return (float) (-(Math.cos(Math.PI * t) - 1) / 2); }
        },
        QUAD_IN("easeInQuad") {
            @Override
            public float interpolate(float t, float[] args) { return t * t; }
        },
        QUAD_OUT("easeOutQuad") {
            @Override
            public float interpolate(float t, float[] args) { return 1 - (1 - t) * (1 - t); }
        },
        QUAD_INOUT("easeInOutQuad") {
            @Override
            public float interpolate(float t, float[] args) { return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2; }
        },
        CUBIC_IN("easeInCubic") {
            @Override
            public float interpolate(float t, float[] args) { return t * t * t; }
        },
        CUBIC_OUT("easeOutCubic") {
            @Override
            public float interpolate(float t, float[] args) { return 1 - (float) Math.pow(1 - t, 3); }
        },
        CUBIC_INOUT("easeInOutCubic") {
            @Override
            public float interpolate(float t, float[] args) { return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }
        },
        QUART_IN("easeInQuart") {
            @Override
            public float interpolate(float t, float[] args) { return t * t * t * t; }
        },
        QUART_OUT("easeOutQuart") {
            @Override
            public float interpolate(float t, float[] args) { return 1 - (float) Math.pow(1 - t, 4); }
        },
        QUART_INOUT("easeInOutQuart") {
            @Override
            public float interpolate(float t, float[] args) { return t < 0.5f ? 8 * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 4) / 2; }
        },
        QUINT_IN("easeInQuint") {
            @Override
            public float interpolate(float t, float[] args) { return t * t * t * t * t; }
        },
        QUINT_OUT("easeOutQuint") {
            @Override
            public float interpolate(float t, float[] args) { return 1 - (float) Math.pow(1 - t, 5); }
        },
        QUINT_INOUT("easeInOutQuint") {
            @Override
            public float interpolate(float t, float[] args) { return t < 0.5f ? 16 * t * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 5) / 2; }
        },
        EXP_IN("easeInExpo") {
            @Override
            public float interpolate(float t, float[] args) { return t == 0 ? 0 : (float) Math.pow(2, 10 * (t - 1)); }
        },
        EXP_OUT("easeOutExpo") {
            @Override
            public float interpolate(float t, float[] args) { return t == 1 ? 1 : 1 - (float) Math.pow(2, -10 * t); }
        },
        EXP_INOUT("easeInOutExpo") {
            @Override
            public float interpolate(float t, float[] args) {
                if (t == 0) return 0;
                if (t == 1) return 1;
                return t < 0.5f ? (float) Math.pow(2, 20 * t - 10) / 2 : (2 - (float) Math.pow(2, -20 * t + 10)) / 2;
            }
        },
        CIRCLE_IN("easeInCirc") {
            @Override
            public float interpolate(float t, float[] args) { return 1 - (float) Math.sqrt(1 - Math.pow(t, 2)); }
        },
        CIRCLE_OUT("easeOutCirc") {
            @Override
            public float interpolate(float t, float[] args) { return (float) Math.sqrt(1 - Math.pow(t - 1, 2)); }
        },
        CIRCLE_INOUT("easeInOutCirc") {
            @Override
            public float interpolate(float t, float[] args) {
                return t < 0.5f ? (1 - (float) Math.sqrt(1 - Math.pow(2 * t, 2))) / 2 : ((float) Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2;
            }
        },
        BACK_IN("easeInBack") {
            @Override
            public float interpolate(float t, float[] args) {
                float c1 = args != null && args.length > 0 ? args[0] : 1.70158f;
                return (1 + c1) * t * t * t - c1 * t * t;
            }
        },
        BACK_OUT("easeOutBack") {
            @Override
            public float interpolate(float t, float[] args) {
                float c1 = args != null && args.length > 0 ? args[0] : 1.70158f;
                return 1 + (1 + c1) * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
            }
        },
        BACK_INOUT("easeInOutBack") {
            @Override
            public float interpolate(float t, float[] args) {
                float c1 = args != null && args.length > 0 ? args[0] : 1.70158f;
                float c2 = c1 * 1.525f;
                return t < 0.5f ? ((float) Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
                        : ((float) Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2;
            }
        },
        ELASTIC_IN("easeInElastic") {
            @Override
            public float interpolate(float t, float[] args) {
                if (t == 0) return 0;
                if (t == 1) return 1;
                float c4 = (float) (2 * Math.PI) / 3;
                return (float) (-Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * c4));
            }
        },
        ELASTIC_OUT("easeOutElastic") {
            @Override
            public float interpolate(float t, float[] args) {
                if (t == 0) return 0;
                if (t == 1) return 1;
                float c4 = (float) (2 * Math.PI) / 3;
                return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1);
            }
        },
        ELASTIC_INOUT("easeInOutElastic") {
            @Override
            public float interpolate(float t, float[] args) {
                if (t == 0) return 0;
                if (t == 1) return 1;
                float c5 = (float) (2 * Math.PI) / 4.5f;
                return t < 0.5f
                        ? (float) (-(Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125) * c5)) / 2)
                        : (float) ((Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125) * c5)) / 2 + 1);
            }
        },
        BOUNCE_IN("easeInBounce") {
            @Override
            public float interpolate(float t, float[] args) { return 1 - BOUNCE_OUT.interpolate(1 - t, args); }
        },
        BOUNCE_OUT("easeOutBounce") {
            @Override
            public float interpolate(float t, float[] args) {
                float n1 = 7.5625f;
                float d1 = 2.75f;
                if (t < 1 / d1) {
                    return n1 * t * t;
                } else if (t < 2 / d1) {
                    return n1 * (t -= 1.5f / d1) * t + 0.75f;
                } else if (t < 2.5 / d1) {
                    return n1 * (t -= 2.25f / d1) * t + 0.9375f;
                } else {
                    return n1 * (t -= 2.625f / d1) * t + 0.984375f;
                }
            }
        },
        BOUNCE_INOUT("easeInOutBounce") {
            @Override
            public float interpolate(float t, float[] args) {
                return t < 0.5f ? (1 - BOUNCE_OUT.interpolate(1 - 2 * t, args)) / 2 : (1 + BOUNCE_OUT.interpolate(2 * t - 1, args)) / 2;
            }
        };

        public final String name;

        AnimationEasing(String name) { this.name = name; }

        public abstract float interpolate(float t, float[] args);

        public static AnimationEasing byName(String name) {
            if (name == null) return LINEAR;
            for (AnimationEasing e : values()) {
                if (e.name.equalsIgnoreCase(name)) return e;
            }
            return LINEAR;
        }
    }
}