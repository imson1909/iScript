package com.iscript.iscript.morph.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

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
                            if (key.equals("vector") || key.equals("lerp_mode")) continue;
                            try {
                                float t = Float.parseFloat(key);
                                if (t > maxTime) maxTime = t;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
            return maxTime > 0.0f ? maxTime : 1.0f;
        }

        public String getName() { return name; }
        public boolean isLoop() { return loop; }
        public float getLength() { return animationLength; }
        public Map<String, BoneAnimation> getBoneAnimations() { return boneAnimations; }
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
        private final Map<Float, float[]> keyframes = new HashMap<>();

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

        public Map<Float, float[]> getKeyframes() { return keyframes; }

        public float[] interpolate(float time, boolean loop, float animLength) {
            if (keyframes.isEmpty()) return new float[]{0,0,0};
            float t = loop ? time % animLength : Math.min(time, animLength);
            float prevTime = -1;
            float nextTime = animLength;
            float[] prevVal = null;
            float[] nextVal = null;

            for (Map.Entry<Float, float[]> e : keyframes.entrySet()) {
                float kt = e.getKey();
                if (kt <= t && kt > prevTime) { prevTime = kt; prevVal = e.getValue(); }
                if (kt >= t && kt < nextTime) { nextTime = kt; nextVal = e.getValue(); }
            }

            if (prevVal == null) return nextVal != null ? nextVal : new float[]{0,0,0};
            if (nextVal == null || prevTime == nextTime) return prevVal;

            float progress = (t - prevTime) / (nextTime - prevTime);
            float[] result = new float[prevVal.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = prevVal[i] + (nextVal[i] - prevVal[i]) * progress;
            }
            return result;
        }
    }
}